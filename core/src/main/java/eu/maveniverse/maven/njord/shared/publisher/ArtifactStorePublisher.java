/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher;

import eu.maveniverse.maven.njord.shared.publisher.spi.signature.SignatureType;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

public interface ArtifactStorePublisher {
    /**
     * Publisher name.
     */
    String name();

    /**
     * Returns short description of publisher.
     */
    String description();

    /**
     * The remote repository where release artifacts will become available after publishing succeeded.
     */
    Optional<RemoteRepository> targetReleaseRepository();

    /**
     * The remote repository where snapshot artifacts will become available after publishing succeeded.
     */
    Optional<RemoteRepository> targetSnapshotRepository();

    /**
     * The remote repository where release artifacts will be published.
     */
    Optional<RemoteRepository> serviceReleaseRepository();

    /**
     * The remote repository where snapshot artifacts will be published.
     */
    Optional<RemoteRepository> serviceSnapshotRepository();

    /**
     * Returns the list of mandatory checksum algorithms.
     */
    Optional<List<ChecksumAlgorithmFactory>> mandatoryChecksumAlgorithms();

    /**
     * Returns the list of supported and optional checksum algorithms.
     */
    Optional<List<ChecksumAlgorithmFactory>> optionalChecksumAlgorithms();

    /**
     * Returns the list of mandatory signature types.
     */
    Optional<List<SignatureType>> mandatorySignatureAlgorithms();

    /**
     * Returns the list of supported and optional signature types.
     */
    Optional<List<SignatureType>> optionalSignatureAlgorithms();

    /**
     * The validator that must be applied to release store before publishing.
     */
    Optional<ArtifactStoreValidator> releaseValidator();

    /**
     * The validator that must be applied to snapshot store before publishing.
     */
    Optional<ArtifactStoreValidator> snapshotValidator();

    /**
     * Performs a non-disruptive validation of artifact store, if validator present, or empty.
     */
    Optional<ArtifactStoreValidator.ValidationResult> validate(ArtifactStore artifactStore) throws IOException;

    /**
     * Performs the publishing.
     */
    void publish(ArtifactStore artifactStore) throws IOException;
}
