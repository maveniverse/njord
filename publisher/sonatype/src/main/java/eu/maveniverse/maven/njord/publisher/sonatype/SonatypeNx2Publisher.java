/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.impl.publisher.ArtifactStorePublisherSupport;
import eu.maveniverse.maven.njord.shared.impl.repository.ArtifactStoreDeployer;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public class SonatypeNx2Publisher extends ArtifactStorePublisherSupport {
    public SonatypeNx2Publisher(
            RepositorySystem repositorySystem,
            RepositorySystemSession session,
            String serviceName,
            String serviceDescription,
            RemoteRepository targetReleaseRepository,
            RemoteRepository targetSnapshotRepository,
            RemoteRepository serviceReleaseRepository,
            RemoteRepository serviceSnapshotRepository,
            ArtifactStoreRequirements artifactStoreRequirements) {
        super(
                repositorySystem,
                session,
                serviceName,
                serviceDescription,
                targetReleaseRepository,
                targetSnapshotRepository,
                serviceReleaseRepository,
                serviceSnapshotRepository,
                artifactStoreRequirements);
    }

    @Override
    public void publish(ArtifactStore artifactStore) throws IOException {
        requireNonNull(artifactStore);
        checkClosed();

        logger.info("Publishing {} to {}", artifactStore, name());

        validateArtifactStore(artifactStore);

        RemoteRepository repository = artifactStore.repositoryMode() == RepositoryMode.RELEASE
                ? serviceReleaseRepository
                : serviceSnapshotRepository;
        if (repository == null) {
            throw new IllegalArgumentException("Repository mode " + artifactStore.repositoryMode()
                    + " not supported; provide RemoteRepository for it");
        }
        new ArtifactStoreDeployer(repositorySystem, session, repository).deploy(artifactStore);
    }
}
