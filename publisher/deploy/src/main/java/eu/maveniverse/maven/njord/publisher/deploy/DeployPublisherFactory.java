/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.deploy;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

@Singleton
@Named(DeployPublisherFactory.NAME)
public class DeployPublisherFactory implements ArtifactStorePublisherFactory {
    public static final String NAME = "deploy";

    private final RepositorySystem repositorySystem;

    @Inject
    public DeployPublisherFactory(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public ArtifactStorePublisher create(SessionConfig sessionConfig) {
        DeployPublisherConfig cpConfig = new DeployPublisherConfig(sessionConfig);
        RemoteRepository releasesRepository = null;
        RemoteRepository snapshotsRepository = null;
        if (cpConfig.releaseRepositoryId() != null && cpConfig.releaseRepositoryUrl() != null) {
            releasesRepository = new RemoteRepository.Builder(
                            cpConfig.releaseRepositoryId(), "default", cpConfig.releaseRepositoryUrl())
                    .build();
        }
        if (cpConfig.snapshotRepositoryId() != null && cpConfig.snapshotRepositoryUrl() != null) {
            snapshotsRepository = new RemoteRepository.Builder(
                            cpConfig.snapshotRepositoryId(), "default", cpConfig.snapshotRepositoryUrl())
                    .build();
        }

        return new DeployPublisher(
                sessionConfig,
                repositorySystem,
                releasesRepository,
                snapshotsRepository,
                ArtifactStoreRequirements.NONE);
    }
}
