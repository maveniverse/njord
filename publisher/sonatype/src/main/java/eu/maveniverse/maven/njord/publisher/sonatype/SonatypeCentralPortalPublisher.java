/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.impl.CloseableSupport;
import eu.maveniverse.maven.njord.shared.impl.repository.ArtifactStoreDeployer;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreValidator;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public class SonatypeCentralPortalPublisher extends CloseableSupport implements ArtifactStorePublisher {
    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession session;
    private final RemoteRepository releasesRepository;
    private final RemoteRepository snapshotsRepository;

    public SonatypeCentralPortalPublisher(
            RepositorySystem repositorySystem,
            RepositorySystemSession session,
            RemoteRepository releasesRepository,
            RemoteRepository snapshotsRepository) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.session = requireNonNull(session);
        this.releasesRepository = releasesRepository;
        this.snapshotsRepository = snapshotsRepository;
    }

    @Override
    public String name() {
        return SonatypeCentralPortalPublisherFactory.NAME;
    }

    @Override
    public String description() {
        return "Publishes to Sonatype Central Portal";
    }

    @Override
    public Optional<RemoteRepository> targetReleaseRepository() {
        return Optional.of(Config.CENTRAL);
    }

    @Override
    public Optional<RemoteRepository> targetSnapshotRepository() {
        return Optional.ofNullable(snapshotsRepository);
    }

    @Override
    public Optional<RemoteRepository> serviceReleaseRepository() {
        return Optional.ofNullable(releasesRepository);
    }

    @Override
    public Optional<RemoteRepository> serviceSnapshotRepository() {
        return Optional.ofNullable(snapshotsRepository);
    }

    @Override
    public Optional<ArtifactStoreValidator> validator() {
        return Optional.empty();
    }

    @Override
    public void publish(ArtifactStore artifactStore) throws IOException {
        requireNonNull(artifactStore);
        checkClosed();

        RemoteRepository repository = artifactStore.repositoryMode() == RepositoryMode.RELEASE
                ? this.releasesRepository
                : this.snapshotsRepository;
        if (repository == null) {
            throw new IllegalArgumentException("Repository mode " + artifactStore.repositoryMode()
                    + " not supported; provide RemoteRepository for it");
        }

        if (repository.getPolicy(false).isEnabled()) { // release
            // create ZIP
            // upload ZIP (rel/snap)

        } else { // snapshot
            // just deploy to snapshots as m-deploy-p would
            try (ArtifactStore store = artifactStore) {
                new ArtifactStoreDeployer(repositorySystem, session, repository).deploy(store);
            }
        }
    }
}
