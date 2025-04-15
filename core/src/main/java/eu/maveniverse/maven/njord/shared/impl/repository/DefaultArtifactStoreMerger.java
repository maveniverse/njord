/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.repository;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.impl.CloseableConfigSupport;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMerger;
import java.io.IOException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public class DefaultArtifactStoreMerger extends CloseableConfigSupport<Config> implements ArtifactStoreMerger {
    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession session;

    public DefaultArtifactStoreMerger(
            Config config, RepositorySystem repositorySystem, RepositorySystemSession session) {
        super(config);
        this.repositorySystem = requireNonNull(repositorySystem);
        this.session = requireNonNull(session);
    }

    @Override
    public void redeploy(ArtifactStore source, ArtifactStore target) throws IOException {
        requireNonNull(source);
        requireNonNull(target);
        checkClosed();

        logger.info("Redeploying {} -> {}", source, target);
        String targetName = target.name();
        target.close();
        try (ArtifactStore from = source; ) {
            new ArtifactStoreDeployer(
                            repositorySystem,
                            session,
                            new RemoteRepository.Builder(targetName, "default", "njord:store:" + targetName).build())
                    .deploy(from);
        }
    }

    @Override
    public void merge(ArtifactStore source, ArtifactStore target) throws IOException {
        requireNonNull(source);
        requireNonNull(target);
        checkClosed();

        logger.info("Merging {} -> {}", source, target);
        throw new IOException("not implemented");
    }
}
