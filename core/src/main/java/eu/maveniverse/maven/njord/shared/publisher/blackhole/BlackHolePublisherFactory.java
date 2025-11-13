/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher.blackhole;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirementsFactory;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

/**
 * Black hole publisher (or like {@code /dev/null} device) factory.
 */
@Singleton
@Named(BlackHolePublisherFactory.NAME)
public class BlackHolePublisherFactory implements ArtifactStorePublisherFactory {
    public static final String NAME = "black-hole";

    private final RepositorySystem repositorySystem;
    private final Map<String, ArtifactStoreRequirementsFactory> artifactStoreRequirementsFactories;

    @Inject
    public BlackHolePublisherFactory(
            RepositorySystem repositorySystem,
            Map<String, ArtifactStoreRequirementsFactory> artifactStoreRequirementsFactories) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.artifactStoreRequirementsFactories = requireNonNull(artifactStoreRequirementsFactories);
    }

    @Override
    public ArtifactStorePublisher create(Session session) {
        requireNonNull(session);

        BlackHolePublisherConfig config = new BlackHolePublisherConfig(session.config());

        RemoteRepository releasesRepository = config.releaseRepositoryId() != null
                        && config.releaseRepositoryUrl() != null
                ? new RemoteRepository.Builder(config.releaseRepositoryId(), "default", config.releaseRepositoryUrl())
                        .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                        .build()
                : null;
        RemoteRepository snapshotsRepository = config.snapshotRepositoryId() != null
                        && config.snapshotRepositoryUrl() != null
                ? new RemoteRepository.Builder(config.snapshotRepositoryId(), "default", config.snapshotRepositoryUrl())
                        .setReleasePolicy(new RepositoryPolicy(false, null, null))
                        .build()
                : null;

        ArtifactStoreRequirements artifactStoreRequirements = ArtifactStoreRequirements.NONE;
        if (!ArtifactStoreRequirements.NONE.name().equals(config.artifactStoreRequirements())) {
            artifactStoreRequirements = artifactStoreRequirementsFactories
                    .get(config.artifactStoreRequirements())
                    .create(session);
        }

        return new BlackHolePublisher(
                session,
                repositorySystem,
                releasesRepository,
                snapshotsRepository,
                artifactStoreRequirements,
                config.fail());
    }
}
