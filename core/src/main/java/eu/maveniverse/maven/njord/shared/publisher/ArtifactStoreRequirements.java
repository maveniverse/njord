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
    Optional<List<ChecksumAlgorithmFactory>> mandatoryChecksumAlgorithms();

    /**
     * Returns the list of supported and optional checksum algorithms.
     */
    Optional<List<ChecksumAlgorithmFactory>> optionalChecksumAlgorithms();

    /**
     * Returns the list of mandatory signature types.
     */
    Optional<List<SignatureType>> mandatorySignatureTypes();

    /**
     * Returns the list of supported and optional signature types.
     */
    Optional<List<SignatureType>> optionalSignatureTypes();

    /**
     * The validator that must be applied to release store before publishing.
     */
    Optional<ArtifactStoreValidator> releaseValidator();

    /**
     * The validator that must be applied to snapshot store before publishing.
     */
    Optional<ArtifactStoreValidator> snapshotValidator();
}
