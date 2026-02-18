/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.deploy;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.NjordUtils;
import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.impl.store.ArtifactStoreDeployer;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherSupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

public class DeployPublisher extends ArtifactStorePublisherSupport {
    private final DeployPublisherConfig config;

    public DeployPublisher(
            Session session,
            RepositorySystem repositorySystem,
            DeployPublisherConfig config,
            ArtifactStoreRequirements artifactStoreRequirements) {
        super(
                session,
                repositorySystem,
                DeployPublisherFactory.NAME,
                "Publishes to any repository just like maven-deploy-plugin would",
                config.targetReleaseRepository(),
                config.targetSnapshotRepository(),
                config.targetReleaseRepository(),
                config.targetSnapshotRepository(),
                artifactStoreRequirements);
        this.config = requireNonNull(config);
    }

    @Override
    protected void doPublish(ArtifactStore artifactStore) throws IOException {
        RemoteRepository repository = selectServiceRemoteRepositoryFor(artifactStore);
        if (session.config().dryRun()) {
            logger.info(
                    "Dry run; not publishing '{}' to '{}' service at {}",
                    artifactStore.name(),
                    name,
                    repository.getUrl());
            return;
        }
        // handle auth redirection, if needed and
        // just deploy as m-deploy-p would
        RemoteRepository publishingRepository =
                session.artifactPublisherRedirector().getPublishingRepository(repository, true);
        logger.debug("Publishing '{}' to '{}' service at {}", artifactStore.name(), name, publishingRepository);
        new ArtifactStoreDeployer(
                        repositorySystem,
                        new DefaultRepositorySystemSession(session.config().session())
                                .setConfigProperty(NjordUtils.RESOLVER_SESSION_CONNECTOR_SKIP, true),
                        config.isSilent(),
                        publishingRepository,
                        true)
                .deploy(artifactStore);
    }
}
