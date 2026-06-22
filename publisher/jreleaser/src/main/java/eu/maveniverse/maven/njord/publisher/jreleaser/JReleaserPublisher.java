/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.jreleaser;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherSupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.jreleaser.workflow.Workflows;

public class JReleaserPublisher extends ArtifactStorePublisherSupport {
    private final JReleaserPublisherConfig config;

    public JReleaserPublisher(
            Session session,
            RepositorySystem repositorySystem,
            JReleaserPublisherConfig config,
            ArtifactStoreRequirements artifactStoreRequirements) {
        super(
                session,
                repositorySystem,
                JReleaserPublisherFactory.NAME,
                "Invokes using JReleaser over given ArtifactStore",
                config.targetReleaseRepository(),
                config.targetSnapshotRepository(),
                config.targetReleaseRepository(),
                config.targetSnapshotRepository(),
                artifactStoreRequirements);
        this.config = requireNonNull(config);
    }

    @Override
    protected void doPublish(ArtifactStore artifactStore) {
        try {
            RemoteRepository repository = selectServiceRemoteRepositoryFor(artifactStore);
            if (session.config().dryRun()) {
                logger.info(
                        "Dry run; not publishing '{}' to '{}' service at {}",
                        artifactStore.name(),
                        name,
                        repository.getUrl());
                return;
            }
            Workflows.fullRelease(config.createContext()).execute();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
