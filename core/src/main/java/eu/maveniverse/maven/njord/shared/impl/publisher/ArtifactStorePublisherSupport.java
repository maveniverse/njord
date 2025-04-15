/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.impl.CloseableSupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreValidator;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public abstract class ArtifactStorePublisherSupport extends CloseableSupport implements ArtifactStorePublisher {
    protected final RepositorySystem repositorySystem;
    protected final RepositorySystemSession session;
    protected final String name;
    protected final String description;
    protected final RemoteRepository targetReleaseRepository;
    protected final RemoteRepository targetSnapshotRepository;
    protected final RemoteRepository serviceReleaseRepository;
    protected final RemoteRepository serviceSnapshotRepository;

    protected ArtifactStorePublisherSupport(
            RepositorySystem repositorySystem,
            RepositorySystemSession session,
            String name,
            String description,
            RemoteRepository targetReleaseRepository,
            RemoteRepository targetSnapshotRepository,
            RemoteRepository serviceReleaseRepository,
            RemoteRepository serviceSnapshotRepository) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.session = requireNonNull(session);
        this.name = requireNonNull(name);
        this.description = requireNonNull(description);
        this.targetReleaseRepository = targetReleaseRepository;
        this.targetSnapshotRepository = targetSnapshotRepository;
        this.serviceReleaseRepository = serviceReleaseRepository;
        this.serviceSnapshotRepository = serviceSnapshotRepository;
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
    public Optional<ArtifactStoreValidator> releaseValidator() {
        return Optional.empty();
    }

    @Override
    public Optional<ArtifactStoreValidator> snapshotValidator() {
        return Optional.empty();
    }

    @Override
    public abstract void publish(ArtifactStore artifactStore) throws IOException;

    protected void validateArtifactStore(ArtifactStore artifactStore) throws IOException {
        Optional<ArtifactStoreValidator> vo =
                artifactStore.repositoryMode() == RepositoryMode.RELEASE ? releaseValidator() : snapshotValidator();
        if (vo.isPresent()) {
            ArtifactStoreValidator.ValidationResult vr = vo.orElseThrow().validate(artifactStore);
            logger.error("ArtifactStore {} validation result:", artifactStore);
            AtomicInteger counter = new AtomicInteger(0);
            for (String msg : vr.error()) {
                logger.error("  {}. {}", counter.incrementAndGet(), msg);
            }
            for (String msg : vr.warning()) {
                logger.warn("  {}. {}", counter.incrementAndGet(), msg);
            }
            for (String msg : vr.info()) {
                logger.info("  {}. {}", counter.incrementAndGet(), msg);
            }
            if (!vr.isValid()) {
                logger.error("ArtifactStore {} failed validation", artifactStore);
                throw new IOException("Validation failed");
            }
        }
    }
}
