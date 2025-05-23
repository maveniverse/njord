/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactPublisherRedirector;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactPublisherRedirectorFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreComparator;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreComparatorFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMerger;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMergerFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreWriter;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreWriterFactory;
import eu.maveniverse.maven.shared.core.component.CloseableConfigSupport;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DefaultSession extends CloseableConfigSupport<SessionConfig> implements Session {
    private final String sessionBoundStoreKey;
    private final DefaultSessionFactory defaultSessionFactory;
    private final InternalArtifactStoreManager internalArtifactStoreManager;
    private final ArtifactStoreWriter artifactStoreWriter;
    private final ArtifactStoreMerger artifactStoreMerger;
    private final ArtifactPublisherRedirector artifactPublisherRedirector;
    private final Map<String, ArtifactStorePublisherFactory> artifactStorePublisherFactories;
    private final Map<String, ArtifactStoreComparatorFactory> artifactStoreComparatorFactories;

    private final CopyOnWriteArrayList<Supplier<Integer>> derivedPublishSessionArtifactStores;
    private final CopyOnWriteArrayList<Supplier<Integer>> derivedDropSessionArtifactStores;

    public DefaultSession(
            SessionConfig sessionConfig,
            DefaultSessionFactory defaultSessionFactory,
            InternalArtifactStoreManagerFactory internalArtifactStoreManagerFactory,
            ArtifactStoreWriterFactory artifactStoreWriterFactory,
            ArtifactStoreMergerFactory artifactStoreMergerFactory,
            ArtifactPublisherRedirectorFactory artifactPublisherRedirectorFactory,
            Map<String, ArtifactStorePublisherFactory> artifactStorePublisherFactories,
            Map<String, ArtifactStoreComparatorFactory> artifactStoreComparatorFactories) {
        super(sessionConfig);
        this.sessionBoundStoreKey = Session.class.getName() + "." + ArtifactStore.class + "." + UUID.randomUUID();
        this.defaultSessionFactory = requireNonNull(defaultSessionFactory);
        this.internalArtifactStoreManager = internalArtifactStoreManagerFactory.create(sessionConfig);
        this.artifactStoreWriter = requireNonNull(artifactStoreWriterFactory).create(sessionConfig);
        this.artifactStoreMerger = requireNonNull(artifactStoreMergerFactory).create(sessionConfig);
        this.artifactPublisherRedirector =
                requireNonNull(artifactPublisherRedirectorFactory).create(this);
        this.artifactStorePublisherFactories = requireNonNull(artifactStorePublisherFactories);
        this.artifactStoreComparatorFactories = requireNonNull(artifactStoreComparatorFactories);

        this.derivedPublishSessionArtifactStores = new CopyOnWriteArrayList<>();
        this.derivedDropSessionArtifactStores = new CopyOnWriteArrayList<>();
    }

    @Override
    public SessionConfig config() {
        return config;
    }

    @Override
    public Session derive(SessionConfig config) {
        requireNonNull(config);
        DefaultSession result = defaultSessionFactory.create(config);
        derivedPublishSessionArtifactStores.add(result::publishSessionArtifactStores);
        derivedDropSessionArtifactStores.add(result::dropSessionArtifactStores);
        return result;
    }

    @Override
    public ArtifactStoreManager artifactStoreManager() {
        checkClosed();
        return internalArtifactStoreManager;
    }

    @Override
    public ArtifactStoreWriter artifactStoreWriter() {
        checkClosed();
        return artifactStoreWriter;
    }

    @Override
    public ArtifactStoreMerger artifactStoreMerger() {
        checkClosed();
        return artifactStoreMerger;
    }

    @Override
    public ArtifactPublisherRedirector artifactPublisherRedirector() {
        checkClosed();
        return artifactPublisherRedirector;
    }

    @Override
    public Collection<ArtifactStorePublisher> availablePublishers() {
        checkClosed();
        return artifactStorePublisherFactories.values().stream()
                .map(f -> f.create(this))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ArtifactStoreComparator> availableComparators() {
        checkClosed();
        return artifactStoreComparatorFactories.values().stream()
                .map(f -> f.create(this))
                .collect(Collectors.toList());
    }

    @Override
    public ArtifactStoreTemplate selectSessionArtifactStoreTemplate(String uri) {
        try {
            if (!uri.contains(":")) {
                if (uri.isEmpty()) {
                    // empty -> default IF project is available
                    if (config.currentProject().isPresent()) {
                        return internalArtifactStoreManager.defaultTemplate(
                                config.currentProject().orElseThrow(J8Utils.OET).repositoryMode());
                    } else {
                        throw new IllegalStateException(
                                "No project present, cannot deduce repository mode: specify template explicitly as `njord:template:<TEMPLATE>`");
                    }
                } else {
                    // non-empty -> template name
                    return selectTemplate(uri);
                }
            } else if (uri.startsWith("template:")) {
                // template:xxx
                return selectTemplate(uri.substring(9));
            } else if (uri.startsWith("store:")) {
                // store:xxx
                try (ArtifactStore artifactStore = internalArtifactStoreManager
                        .selectArtifactStore(uri.substring(6))
                        .orElseThrow(J8Utils.OET)) {
                    return artifactStore.template();
                }
            } else {
                throw new IllegalArgumentException("Invalid repository URI: " + uri);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ArtifactStore getOrCreateSessionArtifactStore(String uri) {
        requireNonNull(uri);

        ConcurrentMap<String, String> sessionBoundStore = getSessionBoundStore();
        String storeName = sessionBoundStore.computeIfAbsent(uri, k -> {
            try {
                String artifactStoreName;
                if (!uri.contains(":")) {
                    if (uri.isEmpty()) {
                        // empty -> default IF project is available
                        if (config.currentProject().isPresent()) {
                            artifactStoreName = createUsingTemplate(internalArtifactStoreManager
                                    .defaultTemplate(config.currentProject()
                                            .orElseThrow(J8Utils.OET)
                                            .repositoryMode())
                                    .name());
                        } else {
                            throw new IllegalStateException(
                                    "No project present, cannot deduce repository mode: specify template explicitly as `njord:template:<TEMPLATE>`");
                        }
                    } else {
                        // non-empty -> template name
                        artifactStoreName = createUsingTemplate(uri);
                    }
                } else if (uri.startsWith("template:")) {
                    // template:xxx
                    artifactStoreName = createUsingTemplate(uri.substring(9));
                } else if (uri.startsWith("store:")) {
                    // store:xxx
                    artifactStoreName = uri.substring(6);
                } else {
                    throw new IllegalArgumentException("Invalid repository URI: " + uri);
                }
                return artifactStoreName;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        try {
            return internalArtifactStoreManager.selectArtifactStore(storeName).orElseThrow(J8Utils.OET);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ArtifactStoreTemplate selectTemplate(String templateName) {
        List<ArtifactStoreTemplate> templates = internalArtifactStoreManager.listTemplates().stream()
                .filter(t -> t.name().equals(templateName))
                .collect(Collectors.toList());
        if (templates.size() != 1) {
            throw new IllegalArgumentException("Unknown template: " + templateName);
        } else {
            ArtifactStoreTemplate template = templates.get(0);
            if (config.prefix().isPresent()) {
                template = template.withPrefix(config.prefix().orElseThrow(J8Utils.OET));
            }
            return template;
        }
    }

    private String createUsingTemplate(String templateName) throws IOException {
        try (ArtifactStore artifactStore = internalArtifactStoreManager.createArtifactStore(
                selectTemplate(templateName),
                config.currentProject().isPresent()
                        ? config.currentProject().orElseThrow(J8Utils.OET).artifact()
                        : null)) {
            return artifactStore.name();
        }
    }

    @Override
    public int publishSessionArtifactStores() {
        int published = 0;
        for (Supplier<Integer> callable : derivedPublishSessionArtifactStores) {
            published += callable.get();
        }
        ConcurrentMap<String, String> sessionBoundStore = getSessionBoundStore();
        if (sessionBoundStore.isEmpty()) {
            return published;
        }
        AtomicInteger result = new AtomicInteger(published);
        Optional<String> pno = artifactPublisherRedirector.getArtifactStorePublisherName();
        if (pno.isPresent()) {
            String publisherName = pno.orElseThrow(J8Utils.OET);
            Optional<ArtifactStorePublisher> po = selectArtifactStorePublisher(publisherName);
            if (po.isPresent()) {
                ArtifactStorePublisher p = po.orElseThrow(J8Utils.OET);
                sessionBoundStore.values().forEach(n -> {
                    try {
                        logger.info("Publishing {} with {}", n, publisherName);
                        try (ArtifactStore as = internalArtifactStoreManager
                                .selectArtifactStore(n)
                                .orElseThrow(J8Utils.OET)) {
                            p.publish(as);
                            result.addAndGet(1);
                        }
                        internalArtifactStoreManager.dropArtifactStore(n);
                    } catch (IOException e) {
                        logger.warn("Could not select ArtifactStore with name {}", n, e);
                    }
                });
            } else {
                throw new IllegalArgumentException("Publisher not found: " + publisherName);
            }
        } else {
            throw new IllegalStateException("Publisher name was not specified nor could be discovered");
        }
        return result.get();
    }

    @Override
    public int dropSessionArtifactStores() {
        int dropped = 0;
        for (Supplier<Integer> callable : derivedDropSessionArtifactStores) {
            dropped += callable.get();
        }
        ConcurrentMap<String, String> sessionBoundStore = getSessionBoundStore();
        AtomicInteger result = new AtomicInteger(dropped);
        sessionBoundStore.values().forEach(n -> {
            try {
                if (internalArtifactStoreManager.dropArtifactStore(n)) {
                    result.addAndGet(1);
                }
            } catch (IOException e) {
                logger.warn("Could not select ArtifactStore with name {}", n, e);
            }
        });
        return result.get();
    }

    @Override
    protected void doClose() throws IOException {
        internalArtifactStoreManager.close();
    }

    /**
     * Returns map of "Njord URI" to "storeName" that were created in current session.
     */
    @SuppressWarnings("unchecked")
    private ConcurrentMap<String, String> getSessionBoundStore() {
        return (ConcurrentHashMap<String, String>)
                config.session().getData().computeIfAbsent(sessionBoundStoreKey, ConcurrentHashMap::new);
    }
}
