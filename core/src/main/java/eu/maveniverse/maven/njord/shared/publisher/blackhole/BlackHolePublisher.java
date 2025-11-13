/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher.blackhole;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherSupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Black hole publisher (or like {@code /dev/null} device), publisher that does not publish anything.
 * Usable for testing.
 */
public class BlackHolePublisher extends ArtifactStorePublisherSupport {
    private final boolean fail;

    public BlackHolePublisher(
            Session session,
            RepositorySystem repositorySystem,
            RemoteRepository releasesRepository,
            RemoteRepository snapshotsRepository,
            ArtifactStoreRequirements artifactStoreRequirements,
            boolean fail) {
        super(
                session,
                repositorySystem,
                BlackHolePublisherFactory.NAME,
                "Publishes to /dev/null (for testing purposes)",
                releasesRepository,
                snapshotsRepository,
                releasesRepository,
                snapshotsRepository,
                artifactStoreRequirements);
        this.fail = fail;
    }

    @Override
    protected void doPublish(ArtifactStore artifactStore) throws IOException {
        RemoteRepository repository = selectRemoteRepositoryFor(artifactStore);
        if (session.config().dryRun()) {
            logger.info(
                    "Dry run; not publishing '{}' to '{}' service at {}",
                    artifactStore.name(),
                    name,
                    repository.getUrl());
            return;
        }
        try (ArtifactStore store = artifactStore) {
            RemoteRepository publishingRepository =
                    session.artifactPublisherRedirector().getPublishingRepository(repository, true);
            logger.debug("Publishing '{}' to '{}' service at {}", artifactStore.name(), name, publishingRepository);
            if (fail) {
                throw new IOException(name + " is set to fail");
            }
        }
    }
}
