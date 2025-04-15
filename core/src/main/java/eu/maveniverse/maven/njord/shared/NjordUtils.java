/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Function;
import org.eclipse.aether.RepositorySystemSession;

public final class NjordUtils {
    private NjordUtils() {}

    public static boolean lazyInit(
            RepositorySystemSession repositorySystemSession,
            Config config,
            Function<Config, NjordSession> njordSessionFactory) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        requireNonNull(config, "config");
        Config oldConfig = mayGetConfig(repositorySystemSession).orElse(null);
        boolean result = false;
        if (oldConfig == null) {
            setConfig(repositorySystemSession, config);
            if (config.enabled()) {
                setNjordSession(repositorySystemSession, njordSessionFactory.apply(config));
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

    public static void setNjordSession(RepositorySystemSession repositorySystemSession, NjordSession njordSession) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        requireNonNull(njordSession, "njordSession");
        NjordSession ns = (NjordSession) repositorySystemSession.getData().get(NjordSession.class);
        if (ns != null) {
            throw new IllegalStateException("Njord session already present");
        }
        repositorySystemSession.getData().set(NjordSession.class, njordSession);
    }

    public static Optional<Config> mayGetConfig(RepositorySystemSession repositorySystemSession) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        Config config = (Config) repositorySystemSession.getData().get(Config.class);
        if (config == null) {
            return Optional.empty();
        }
        return Optional.of(config);
    }

    public static Optional<NjordSession> mayGetNjordSession(RepositorySystemSession repositorySystemSession) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        NjordSession ns = (NjordSession) repositorySystemSession.getData().get(NjordSession.class);
        if (ns == null) {
            return Optional.empty();
        }
        return Optional.of(ns);
    }
}
