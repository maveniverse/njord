/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.store;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMerger;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMergerFactory;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;

@Singleton
@Named
public class DefaultArtifactStoreMergerFactory implements ArtifactStoreMergerFactory {
    private final RepositorySystem repositorySystem;
    private final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;

    @Inject
    public DefaultArtifactStoreMergerFactory(
            RepositorySystem repositorySystem, ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.checksumAlgorithmFactorySelector = requireNonNull(checksumAlgorithmFactorySelector);
    }

    @Override
    public ArtifactStoreMerger create(SessionConfig sessionConfig) {
        return new DefaultArtifactStoreMerger(sessionConfig, repositorySystem, checksumAlgorithmFactorySelector);
    }
}
