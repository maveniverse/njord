/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.NjordSession;
import eu.maveniverse.maven.njord.shared.impl.factories.ArtifactStoreExporterFactory;
import eu.maveniverse.maven.njord.shared.impl.factories.ArtifactStoreMergerFactory;
import eu.maveniverse.maven.njord.shared.impl.factories.InternalArtifactStoreManagerFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreExporter;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMerger;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import org.eclipse.aether.RepositorySystemSession;

public class DefaultNjordSession extends CloseableConfigSupport<Config> implements NjordSession {
    private final RepositorySystemSession session;
    private final InternalArtifactStoreManager internalArtifactStoreManager;
    private final ArtifactStoreExporterFactory artifactStoreExporterFactory;
    private final ArtifactStoreMergerFactory artifactStoreMergerFactory;
    private final Map<String, ArtifactStorePublisherFactory> artifactStorePublisherFactories;

    public DefaultNjordSession(
            RepositorySystemSession session,
            Config config,
            InternalArtifactStoreManagerFactory internalArtifactStoreManagerFactory,
            ArtifactStoreExporterFactory artifactStoreExporterFactory,
            ArtifactStoreMergerFactory artifactStoreMergerFactory,
            Map<String, ArtifactStorePublisherFactory> artifactStorePublisherFactories) {
        super(config);
        this.session = requireNonNull(session);
        this.internalArtifactStoreManager = internalArtifactStoreManagerFactory.create(config);
        this.artifactStoreExporterFactory = requireNonNull(artifactStoreExporterFactory);
        this.artifactStoreMergerFactory = requireNonNull(artifactStoreMergerFactory);
        this.artifactStorePublisherFactories = requireNonNull(artifactStorePublisherFactories);
    }

    @Override
    public Config config() {
        return config;
    }

    @Override
    public ArtifactStoreManager artifactStoreManager() {
        checkClosed();
        return internalArtifactStoreManager;
    }

    @Override
    public ArtifactStoreExporter createArtifactStoreExporter() {
        checkClosed();
        return artifactStoreExporterFactory.create(session, config);
    }

    @Override
    public ArtifactStoreMerger createArtifactStoreMerger() {
        checkClosed();
        return artifactStoreMergerFactory.create(session, config);
    }

    @Override
    public Collection<ArtifactStorePublisher> availablePublishers() {
        checkClosed();
        return artifactStorePublisherFactories.values().stream()
                .map(f -> f.create(session, config))
                .toList();
    }

    @Override
    protected void doClose() throws IOException {
        internalArtifactStoreManager.close();
    }
}
