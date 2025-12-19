/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype.nx2;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactorySupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirementsFactory;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

@Singleton
@Named(SonatypeNx2PublisherFactory.NAME)
public class SonatypeNx2PublisherFactory extends ArtifactStorePublisherFactorySupport
        implements ArtifactStorePublisherFactory {
    public static final String NAME = "sonatype-nx2";

    private final RepositorySystem repositorySystem;
    private final Map<String, ArtifactStoreRequirementsFactory> artifactStoreRequirementsFactories;

    @Inject
    public SonatypeNx2PublisherFactory(
            RepositorySystem repositorySystem,
            Map<String, ArtifactStoreRequirementsFactory> artifactStoreRequirementsFactories) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.artifactStoreRequirementsFactories = requireNonNull(artifactStoreRequirementsFactories);
    }

    @Override
    protected ArtifactStorePublisher doCreate(
            Session session, RemoteRepository releasesRepository, RemoteRepository snapshotsRepository) {
        SonatypeNx2PublisherConfig config = new SonatypeNx2PublisherConfig(session.config());
        ArtifactStoreRequirements artifactStoreRequirements = ArtifactStoreRequirements.NONE;
        if (!ArtifactStoreRequirements.NONE.name().equals(config.artifactStoreRequirements())) {
            artifactStoreRequirements = artifactStoreRequirementsFactories
                    .get(config.artifactStoreRequirements())
                    .create(session);
        }

        return new SonatypeNx2Publisher(
                session,
                repositorySystem,
                NAME,
                "Publishes to Sonatype Nx2",
                releasesRepository,
                snapshotsRepository,
                releasesRepository,
                snapshotsRepository,
                artifactStoreRequirements);
    }
}
