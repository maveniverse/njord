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
import eu.maveniverse.maven.njord.shared.impl.factories.ArtifactStoreMergerFactory;
import eu.maveniverse.maven.njord.shared.impl.factories.ArtifactStoreWriterFactory;
import eu.maveniverse.maven.njord.shared.impl.factories.InternalArtifactStoreManagerFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreComparator;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreComparatorFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMerger;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultSession extends CloseableConfigSupport<SessionConfig> implements Session {
    private final InternalArtifactStoreManager internalArtifactStoreManager;
    private final ArtifactStoreWriterFactory artifactStoreWriterFactory;
    private final ArtifactStoreMergerFactory artifactStoreMergerFactory;
    private final Map<String, ArtifactStorePublisherFactory> artifactStorePublisherFactories;
    private final Map<String, ArtifactStoreComparatorFactory> artifactStoreComparatorFactories;

    public DefaultSession(
            SessionConfig sessionConfig,
            InternalArtifactStoreManagerFactory internalArtifactStoreManagerFactory,
            ArtifactStoreWriterFactory artifactStoreWriterFactory,
            ArtifactStoreMergerFactory artifactStoreMergerFactory,
            Map<String, ArtifactStorePublisherFactory> artifactStorePublisherFactories,
            Map<String, ArtifactStoreComparatorFactory> artifactStoreComparatorFactories) {
        super(sessionConfig);
        this.internalArtifactStoreManager = internalArtifactStoreManagerFactory.create(sessionConfig);
        this.artifactStoreWriterFactory = requireNonNull(artifactStoreWriterFactory);
        this.artifactStoreMergerFactory = requireNonNull(artifactStoreMergerFactory);
        this.artifactStorePublisherFactories = requireNonNull(artifactStorePublisherFactories);
        this.artifactStoreComparatorFactories = requireNonNull(artifactStoreComparatorFactories);
    }

    @Override
    public SessionConfig config() {
        return config;
    }

    @Override
    public ArtifactStoreManager artifactStoreManager() {
        checkClosed();
        return internalArtifactStoreManager;
    }

    @Override
    public ArtifactStoreWriter createArtifactStoreWriter() {
        checkClosed();
        return artifactStoreWriterFactory.create(config());
    }

    @Override
    public ArtifactStoreMerger createArtifactStoreMerger() {
        checkClosed();
        return artifactStoreMergerFactory.create(config());
    }

    @Override
    public Collection<ArtifactStorePublisher> availablePublishers() {
        checkClosed();
        return artifactStorePublisherFactories.values().stream()
                .map(f -> f.create(config()))
                .toList();
    }

    @Override
    public Collection<ArtifactStoreComparator> availableComparators() {
        checkClosed();
        return artifactStoreComparatorFactories.values().stream()
                .map(f -> f.create(config()))
                .toList();
    }

    private static final String SESSION_BOUND_STORES_KEY = Session.class.getName() + "." + ArtifactStore.class;

    @Override
    public ArtifactStore getOrCreateSessionArtifactStore(String uri) {
        requireNonNull(uri);

        ConcurrentHashMap<String, String> sessionBoundStore = (ConcurrentHashMap<String, String>)
                config.session().getData().computeIfAbsent(SESSION_BOUND_STORES_KEY, () -> new ConcurrentHashMap<>());
        String storeName = sessionBoundStore.computeIfAbsent(uri, k -> {
            try {
                String artifactStoreName;
                if (!uri.contains(":")) {
                    if (uri.isEmpty()) {
                        // empty -> default
                        artifactStoreName = createUsingTemplate(
                                internalArtifactStoreManager.defaultTemplate().name());
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
            return internalArtifactStoreManager
                    .selectArtifactStore(storeName)
                    .orElseThrow(() -> new IllegalArgumentException("No such store: " + storeName));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String createUsingTemplate(String templateName) throws IOException {
        List<ArtifactStoreTemplate> templates = internalArtifactStoreManager.listTemplates().stream()
                .filter(t -> t.name().equals(templateName))
                .toList();
        if (templates.size() != 1) {
            throw new IllegalArgumentException("Unknown template: " + templateName);
        } else {
            ArtifactStoreTemplate template = templates.get(0);
            if (config.prefix().isPresent()) {
                template = template.withPrefix(config.prefix().orElseThrow());
            }
            try (ArtifactStore artifactStore = internalArtifactStoreManager.createArtifactStore(template)) {
                return artifactStore.name();
            }
        }
    }

    @Override
    public boolean dropSessionArtifactStores() {
        ConcurrentHashMap<String, String> sessionBoundStore = (ConcurrentHashMap<String, String>)
                config.session().getData().computeIfAbsent(SESSION_BOUND_STORES_KEY, () -> new ConcurrentHashMap<>());
        AtomicBoolean result = new AtomicBoolean(false);
        sessionBoundStore.values().forEach(n -> {
            try {
                if (internalArtifactStoreManager.dropArtifactStore(n)) {
                    result.set(true);
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
}
