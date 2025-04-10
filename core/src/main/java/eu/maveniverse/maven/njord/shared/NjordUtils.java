/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.impl.repository.DefaultArtifactStoreManager;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import java.util.Optional;
import java.util.function.Supplier;
import org.eclipse.aether.RepositorySystemSession;

public final class NjordUtils {
    private NjordUtils() {}

    public static boolean lazyInitConfig(
            RepositorySystemSession repositorySystemSession, Supplier<Config> configSupplier) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        requireNonNull(configSupplier, "configSupplier");
        Config config = mayGetConfig(repositorySystemSession).orElse(null);
        boolean result = false;
        if (config == null) {
            config = configSupplier.get();
            setConfig(repositorySystemSession, config);
            if (config.enabled()) {
                setArtifactStoreManager(repositorySystemSession, new DefaultArtifactStoreManager(config));
                result = true;
            }
        }
        return result;
    }

    public static void setConfig(RepositorySystemSession repositorySystemSession, Config config) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        requireNonNull(config, "config");
        Config conf = (Config) repositorySystemSession.getData().get(Config.class);
        if (conf != null) {
            throw new IllegalStateException("Njord config already present");
        }
        repositorySystemSession.getData().set(Config.class, config);
    }

    public static void setArtifactStoreManager(
            RepositorySystemSession repositorySystemSession, ArtifactStoreManager artifactStoreManager) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        requireNonNull(artifactStoreManager, "artifactStoreManager");
        ArtifactStoreManager asm =
                (ArtifactStoreManager) repositorySystemSession.getData().get(ArtifactStoreManager.class);
        if (asm != null) {
            throw new IllegalStateException("Njord ArtifactStoreManager already present");
        }
        repositorySystemSession.getData().set(ArtifactStoreManager.class, artifactStoreManager);
    }

    public static Optional<Config> mayGetConfig(RepositorySystemSession repositorySystemSession) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        Config config = (Config) repositorySystemSession.getData().get(Config.class);
        if (config == null) {
            return Optional.empty();
        }
        return Optional.of(config);
    }

    public static Optional<ArtifactStoreManager> mayGetArtifactStoreManager(
            RepositorySystemSession repositorySystemSession) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        ArtifactStoreManager asm =
                (ArtifactStoreManager) repositorySystemSession.getData().get(ArtifactStoreManager.class);
        if (asm == null) {
            return Optional.empty();
        }
        return Optional.of(asm);
    }
}
