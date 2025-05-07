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

    /**
     * Performs "eager init" of Njord, will fail if session already exists within this Resolver session.
     * Returns the newly created {@link Session} instance, never {@code null}.
     */
    public static Session init(SessionConfig sessionConfig, Function<SessionConfig, Session> sessionFactory) {
        requireNonNull(sessionConfig, "sessionConfig");
        requireNonNull(sessionFactory, "sessionFactory");
        Session ns = sessionFactory.apply(sessionConfig);
        setNjordSession(sessionConfig.session(), ns); // explodes if present
        return ns;
    }

    /**
     * Performs a "lazy init" of Njord, does not fail if config and session already exists within this Resolver session.
     * Returns the existing or newly created {@link Session} instance, never {@code null}.
     */
    public static Session lazyInit(SessionConfig sessionConfig, Function<SessionConfig, Session> sessionFactory) {
        requireNonNull(sessionConfig, "remoteRepositories");
        requireNonNull(sessionFactory, "sessionFactory");
        Optional<Session> ns = mayGetNjordSession(sessionConfig.session());
        if (ns.isEmpty()) {
            return init(sessionConfig, sessionFactory);
        } else {
            return ns.orElseThrow();
        }
    }

    /**
     * Returns Njord session instance, if inited in this Repository Session.
     */
    public static Optional<Session> mayGetNjordSession(RepositorySystemSession repositorySystemSession) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        Session ns = (Session) repositorySystemSession.getData().get(Session.class);
        if (ns == null) {
            return Optional.empty();
        }
        return Optional.of(ns);
    }

    private static void setNjordSession(RepositorySystemSession repositorySystemSession, Session session) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        requireNonNull(session, "njordSession");
        Session ns = (Session) repositorySystemSession.getData().get(Session.class);
        if (ns != null) {
            throw new IllegalStateException("Njord session already present");
        }
        repositorySystemSession.getData().set(Session.class, session);
    }
}
