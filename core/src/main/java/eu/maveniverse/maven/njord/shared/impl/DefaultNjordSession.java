/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.NjordSession;
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

public class DefaultNjordSession extends CloseableConfigSupport<SessionConfig> implements NjordSession {
    private final InternalArtifactStoreManager internalArtifactStoreManager;
    private final ArtifactStoreWriterFactory artifactStoreWriterFactory;
    private final ArtifactStoreMergerFactory artifactStoreMergerFactory;
    private final Map<String, ArtifactStorePublisherFactory> artifactStorePublisherFactories;
    private final Map<String, ArtifactStoreComparatorFactory> artifactStoreComparatorFactories;

    public DefaultNjordSession(
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
    public SessionConfig sessionConfig() {
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
        return artifactStoreWriterFactory.create(sessionConfig());
    }

    @Override
    public ArtifactStoreMerger createArtifactStoreMerger() {
        checkClosed();
        return artifactStoreMergerFactory.create(sessionConfig());
    }

    @Override
    public Collection<ArtifactStorePublisher> availablePublishers() {
        checkClosed();
        return artifactStorePublisherFactories.values().stream()
                .map(f -> f.create(sessionConfig()))
                .toList();
    }

    @Override
    public Collection<ArtifactStoreComparator> availableComparators() {
        checkClosed();
        return artifactStoreComparatorFactories.values().stream()
                .map(f -> f.create(sessionConfig()))
                .toList();
    }

    private static final String SESSION_BOUND_STORES_KEY = NjordSession.class.getName() + "." + ArtifactStore.class;

    @Override
    public ArtifactStore getOrCreateSessionArtifactStore(String uri) {
        ConcurrentHashMap<String, String> sessionBoundStore = (ConcurrentHashMap<String, String>)
                config.session().getData().computeIfAbsent(SESSION_BOUND_STORES_KEY, () -> new ConcurrentHashMap<>());
        String storeName = sessionBoundStore.computeIfAbsent(uri, k -> {
            try {
                String artifactStoreName;
                if (!uri.contains(":")) {
                    if (uri.isEmpty()) {
                        // empty -> default
                        try (ArtifactStore artifactStore = internalArtifactStoreManager.createArtifactStore(
                                internalArtifactStoreManager.defaultTemplate())) {
                            artifactStoreName = artifactStore.name();
                        }
                    } else {
                        // non-empty -> template name
                        List<ArtifactStoreTemplate> templates = internalArtifactStoreManager.listTemplates().stream()
                                .filter(t -> t.name().equals(uri))
                                .toList();
                        if (templates.size() != 1) {
                            throw new IllegalArgumentException("Unknown template: " + uri);
                        } else {
                            try (ArtifactStore artifactStore =
                                    internalArtifactStoreManager.createArtifactStore(templates.get(0))) {
                                artifactStoreName = artifactStore.name();
                            }
                        }
                    }
                } else if (uri.startsWith("store:")) {
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
