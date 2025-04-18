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
import eu.maveniverse.maven.njord.shared.NjordSessionFactory;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.factories.ArtifactStoreExporterFactory;
import eu.maveniverse.maven.njord.shared.impl.factories.ArtifactStoreMergerFactory;
import eu.maveniverse.maven.njord.shared.impl.factories.InternalArtifactStoreManagerFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class DefaultNjordSessionFactory<C> implements NjordSessionFactory {
    private final InternalArtifactStoreManagerFactory internalArtifactStoreManagerFactory;
    private final ArtifactStoreExporterFactory artifactStoreExporterFactory;
    private final ArtifactStoreMergerFactory artifactStoreMergerFactory;
    private final Map<String, ArtifactStorePublisherFactory> artifactStorePublisherFactories;

    @Inject
    public DefaultNjordSessionFactory(
            InternalArtifactStoreManagerFactory internalArtifactStoreManagerFactory,
            ArtifactStoreExporterFactory artifactStoreExporterFactory,
            ArtifactStoreMergerFactory artifactStoreMergerFactory,
            Map<String, ArtifactStorePublisherFactory> artifactStorePublisherFactories) {
        this.internalArtifactStoreManagerFactory = requireNonNull(internalArtifactStoreManagerFactory);
        this.artifactStoreExporterFactory = requireNonNull(artifactStoreExporterFactory);
        this.artifactStoreMergerFactory = requireNonNull(artifactStoreMergerFactory);
        this.artifactStorePublisherFactories = requireNonNull(artifactStorePublisherFactories);
    }

    @Override
    public NjordSession create(SessionConfig sessionConfig) {
        return new DefaultNjordSession(
                sessionConfig,
                internalArtifactStoreManagerFactory,
                artifactStoreExporterFactory,
                artifactStoreMergerFactory,
                artifactStorePublisherFactories);
    }
}
