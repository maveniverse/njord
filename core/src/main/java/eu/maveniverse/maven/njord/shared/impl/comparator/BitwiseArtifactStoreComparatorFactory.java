/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.comparator;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreComparator;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreComparatorFactory;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;

/**
 * Bitwise comparator factory.
 */
@Singleton
@Named(BitwiseArtifactStoreComparatorFactory.NAME)
public class BitwiseArtifactStoreComparatorFactory implements ArtifactStoreComparatorFactory {
    public static final String NAME = "bitwise";

    private final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;

    @Inject
    public BitwiseArtifactStoreComparatorFactory(ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        this.checksumAlgorithmFactorySelector = requireNonNull(checksumAlgorithmFactorySelector);
    }

    @Override
    public ArtifactStoreComparator create(SessionConfig sessionConfig) {
        return new BitwiseArtifactStoreComparator(
                sessionConfig, NAME, "Compares store contents by bitwise comparison", checksumAlgorithmFactorySelector);
    }
}
