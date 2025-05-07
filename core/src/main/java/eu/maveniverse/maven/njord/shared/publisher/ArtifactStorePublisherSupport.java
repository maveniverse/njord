/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.ComponentSupport;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

public abstract class ArtifactStorePublisherSupport extends ComponentSupport implements ArtifactStorePublisher {
    protected final SessionConfig sessionConfig;
    protected final RepositorySystem repositorySystem;
    protected final String name;
    protected final String description;
    protected final RemoteRepository targetReleaseRepository;
    protected final RemoteRepository targetSnapshotRepository;
    protected final RemoteRepository serviceReleaseRepository;
    protected final RemoteRepository serviceSnapshotRepository;
    protected final ArtifactStoreRequirements artifactStoreRequirements;

    protected ArtifactStorePublisherSupport(
            SessionConfig sessionConfig,
            RepositorySystem repositorySystem,
            String name,
            String description,
            RemoteRepository targetReleaseRepository,
            RemoteRepository targetSnapshotRepository,
            RemoteRepository serviceReleaseRepository,
            RemoteRepository serviceSnapshotRepository,
            ArtifactStoreRequirements artifactStoreRequirements) {
        this.sessionConfig = requireNonNull(sessionConfig);
        this.repositorySystem = requireNonNull(repositorySystem);
        this.name = requireNonNull(name);
        this.description = requireNonNull(description);
        this.targetReleaseRepository = targetReleaseRepository;
        this.targetSnapshotRepository = targetSnapshotRepository;
        this.serviceReleaseRepository = serviceReleaseRepository;
        this.serviceSnapshotRepository = serviceSnapshotRepository;
        this.artifactStoreRequirements = requireNonNull(artifactStoreRequirements);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public Optional<RemoteRepository> targetReleaseRepository() {
        return Optional.ofNullable(targetReleaseRepository);
    }

    @Override
    public Optional<RemoteRepository> targetSnapshotRepository() {
        return Optional.ofNullable(targetSnapshotRepository);
    }

    @Override
    public Optional<RemoteRepository> serviceReleaseRepository() {
        return Optional.ofNullable(serviceReleaseRepository);
    }

    @Override
    public Optional<RemoteRepository> serviceSnapshotRepository() {
        return Optional.ofNullable(serviceSnapshotRepository);
    }

    @Override
    public ArtifactStoreRequirements artifactStoreRequirements() {
        return artifactStoreRequirements;
    }

    @Override
    public Optional<ArtifactStoreValidator.ValidationResult> validate(ArtifactStore artifactStore) throws IOException {
        requireNonNull(artifactStore);

        return doValidate(artifactStore);
    }

    protected Optional<ArtifactStoreValidator.ValidationResult> doValidate(ArtifactStore artifactStore)
            throws IOException {
        Optional<ArtifactStoreValidator> vo = validatorFor(artifactStore);
        if (vo.isPresent()) {
            ArtifactStoreValidator.ValidationResult vr = vo.orElseThrow().validate(artifactStore);
            return Optional.of(vr);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void publish(ArtifactStore artifactStore) throws IOException {
        requireNonNull(artifactStore);

        doPublish(artifactStore);
    }

    protected abstract void doPublish(ArtifactStore artifactStore) throws IOException;

    protected RemoteRepository selectRemoteRepositoryFor(ArtifactStore artifactStore) {
        RemoteRepository repository = artifactStore.repositoryMode() == RepositoryMode.RELEASE
                ? serviceReleaseRepository
                : serviceSnapshotRepository;
        if (repository == null) {
            throw new IllegalArgumentException("Repository mode " + artifactStore.repositoryMode()
                    + " not supported; provide RemoteRepository for it");
        }
        return repository;
    }

    protected Optional<ArtifactStoreValidator> validatorFor(ArtifactStore artifactStore) throws IOException {
        requireNonNull(artifactStore);
        return artifactStore.repositoryMode() == RepositoryMode.RELEASE
                ? artifactStoreRequirements.releaseValidator()
                : artifactStoreRequirements.snapshotValidator();
    }
}
