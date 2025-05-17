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
    /**
     * Special flag to be used in Resolver session config properties: flags that Njord connector should not intervene,
     * and should remain dormant, "skip" connector creation and let other connectors do the work.
     */
    public static final String RESOLVER_SESSION_CONNECTOR_SKIP = SessionConfig.NAME + ".connector.skip";

    private NjordUtils() {}

    /**
     * Performs a "lazy init" of Njord, does not fail if config and session already exists within this Resolver session.
     * Returns the existing or newly created {@link Session} instance, never {@code null}.
     */
    public static synchronized Session lazyInit(
            SessionConfig sessionConfig, Function<SessionConfig, Session> sessionFactory) {
        requireNonNull(sessionConfig, "remoteRepositories");
        requireNonNull(sessionFactory, "sessionFactory");
        Session session = (Session) sessionConfig.session().getData().get(Session.class);
        if (session == null) {
            session = sessionFactory.apply(sessionConfig);
            sessionConfig.session().getData().set(Session.class, session);
        }
        return session;
    }

    /**
     * Returns Njord session instance, if inited in this Repository Session.
     */
    public static synchronized Optional<Session> mayGetNjordSession(RepositorySystemSession repositorySystemSession) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        return Optional.ofNullable((Session) repositorySystemSession.getData().get(Session.class));
    }
}
