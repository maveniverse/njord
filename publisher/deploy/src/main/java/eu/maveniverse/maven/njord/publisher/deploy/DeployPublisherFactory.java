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
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

@Singleton
@Named(DeployPublisherFactory.NAME)
public class DeployPublisherFactory implements ArtifactStorePublisherFactory {
    public static final String NAME = "deploy";

    private static final String PROP_ALT_DEPLOYMENT_REPOSITORY = "altDeploymentRepository";

    private final RepositorySystem repositorySystem;

    @Inject
    public DeployPublisherFactory(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public ArtifactStorePublisher create(SessionConfig sessionConfig) {
        RemoteRepository releasesRepository = null;
        RemoteRepository snapshotsRepository = null;
        if (sessionConfig.effectiveProperties().containsKey(PROP_ALT_DEPLOYMENT_REPOSITORY)) {
            String altDeploymentRepository = sessionConfig.effectiveProperties().get(PROP_ALT_DEPLOYMENT_REPOSITORY);
            String[] split = altDeploymentRepository.split("::");
            if (split.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid alt deployment repository syntax (supported is id::url): " + altDeploymentRepository);
            }
            String id = split[0];
            String url = split[1];
            releasesRepository = new RemoteRepository.Builder(id, "default", url)
                    .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                    .build();
            snapshotsRepository = new RemoteRepository.Builder(id, "default", url)
                    .setReleasePolicy(new RepositoryPolicy(false, null, null))
                    .build();
        } else if (sessionConfig.currentProject().isPresent()) {
            SessionConfig.CurrentProject project =
                    sessionConfig.currentProject().orElseThrow();
            releasesRepository = project.distributionManagementRepositories().get(RepositoryMode.RELEASE);
            snapshotsRepository = project.distributionManagementRepositories().get(RepositoryMode.SNAPSHOT);
        }

        return new DeployPublisher(
                sessionConfig,
                repositorySystem,
                releasesRepository,
                snapshotsRepository,
                ArtifactStoreRequirements.NONE);
    }
}
