/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype.nx2;

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

public class SonatypeNx2Publisher extends ArtifactStorePublisherSupport {
    private final boolean silent;

    public SonatypeNx2Publisher(
            Session session,
            RepositorySystem repositorySystem,
            String serviceName,
            String serviceDescription,
            RemoteRepository targetReleaseRepository,
            RemoteRepository targetSnapshotRepository,
            RemoteRepository serviceReleaseRepository,
            RemoteRepository serviceSnapshotRepository,
            ArtifactStoreRequirements artifactStoreRequirements,
            boolean silent) {
        super(
                session,
                repositorySystem,
                serviceName,
                serviceDescription,
                targetReleaseRepository,
                targetSnapshotRepository,
                serviceReleaseRepository,
                serviceSnapshotRepository,
                artifactStoreRequirements);
        this.silent = silent;
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
        // deploy as m-deploy-p would
        RemoteRepository publishingRepository =
                session.artifactPublisherRedirector().getPublishingRepository(repository, true);
        logger.debug("Publishing '{}' to '{}' service at {}", artifactStore.name(), name, publishingRepository);
        new ArtifactStoreDeployer(
                        repositorySystem,
                        new DefaultRepositorySystemSession(session.config().session())
                                .setConfigProperty(NjordUtils.RESOLVER_SESSION_CONNECTOR_SKIP, true),
                        silent,
                        publishingRepository,
                        true)
                .deploy(artifactStore);
    }
}
