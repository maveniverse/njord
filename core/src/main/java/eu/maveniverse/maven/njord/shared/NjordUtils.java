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
            SessionConfig sessionConfig, Function<SessionConfig, NjordSession> njordSessionFactory) {
        requireNonNull(sessionConfig, "sessionConfig");
        requireNonNull(njordSessionFactory, "njordSessionFactory");
        NjordSession oldSession = mayGetNjordSession(sessionConfig.session()).orElse(null);
        boolean result = false;
        if (oldSession == null) {
            if (sessionConfig.config().enabled()) {
                setNjordSession(sessionConfig.session(), njordSessionFactory.apply(sessionConfig));
                result = true;
            }
        }
        return result;
    }

    public static Optional<NjordSession> mayGetNjordSession(RepositorySystemSession repositorySystemSession) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        NjordSession ns = (NjordSession) repositorySystemSession.getData().get(NjordSession.class);
        if (ns == null) {
            return Optional.empty();
        }
        return Optional.of(ns);
    }

    private static void setNjordSession(RepositorySystemSession repositorySystemSession, NjordSession njordSession) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        requireNonNull(njordSession, "njordSession");
        NjordSession ns = (NjordSession) repositorySystemSession.getData().get(NjordSession.class);
        if (ns != null) {
            throw new IllegalStateException("Njord session already present");
        }
        repositorySystemSession.getData().set(NjordSession.class, njordSession);
    }
}
