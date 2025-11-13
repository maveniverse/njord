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

@Singleton
@Named(SonatypeNx2PublisherFactory.NAME)
public class SonatypeNx2PublisherFactory implements ArtifactStorePublisherFactory {
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
    public SonatypeNx2Publisher create(Session session) {
        SonatypeNx2PublisherConfig config = new SonatypeNx2PublisherConfig(session.config());
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
        if (!ArtifactStoreRequirements.NONE.name().equals(config.getArtifactStoreRequirements())) {
            artifactStoreRequirements = artifactStoreRequirementsFactories
                    .get(config.getArtifactStoreRequirements())
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
