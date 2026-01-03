/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher.blackhole;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactorySupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirementsFactory;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;

/**
 * Black hole publisher (or like {@code /dev/null} device) factory.
 */
@Singleton
@Named(BlackHolePublisherFactory.NAME)
public class BlackHolePublisherFactory extends ArtifactStorePublisherFactorySupport
        implements ArtifactStorePublisherFactory {
    public static final String NAME = "black-hole";

    @Inject
    public BlackHolePublisherFactory(
            RepositorySystem repositorySystem,
            Map<String, ArtifactStoreRequirementsFactory> artifactStoreRequirementsFactories) {
        super(repositorySystem, artifactStoreRequirementsFactories);
    }

    @Override
    protected ArtifactStorePublisher doCreate(Session session) {
        BlackHolePublisherConfig config = new BlackHolePublisherConfig(session.config());

        return new BlackHolePublisher(
                session,
                repositorySystem,
                config.targetReleaseRepository(),
                config.targetSnapshotRepository(),
                config.serviceReleaseRepository(),
                config.serviceSnapshotRepository(),
                createArtifactStoreRequirements(session, config),
                config.fail());
    }
}
