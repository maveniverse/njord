/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher;

import eu.maveniverse.maven.njord.shared.publisher.spi.signature.SignatureType;
import java.util.List;
import java.util.Optional;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

public interface ArtifactStoreRequirements {
    /**
     * Requirements name.
     */
    String name();

    /**
     * Returns short description of requirements.
     */
    String description();

    /**
     * Returns the list of mandatory checksum algorithms.
     */
    default Optional<List<ChecksumAlgorithmFactory>> mandatoryChecksumAlgorithms() {
        return Optional.empty();
    }

    /**
     * Returns the list of supported and optional checksum algorithms.
     */
    default Optional<List<ChecksumAlgorithmFactory>> optionalChecksumAlgorithms() {
        return Optional.empty();
    }

    /**
     * Returns the list of mandatory signature types.
     */
    default Optional<List<SignatureType>> mandatorySignatureTypes() {
        return Optional.empty();
    }

    /**
     * Returns the list of supported and optional signature types.
     */
    default Optional<List<SignatureType>> optionalSignatureTypes() {
        return Optional.empty();
    }

    /**
     * The validator that must be applied to release store before publishing.
     */
    default Optional<ArtifactStoreValidator> releaseValidator() {
        return Optional.empty();
    }

    /**
     * The validator that must be applied to snapshot store before publishing.
     */
    default Optional<ArtifactStoreValidator> snapshotValidator() {
        return Optional.empty();
    }
}
