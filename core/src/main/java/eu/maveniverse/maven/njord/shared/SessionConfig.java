/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Session config holds all the session related data.
 */
public interface SessionConfig {
    /**
     * Resolver session, never {@code null}.
     */
    RepositorySystemSession session();

    /**
     * Remote repositories, never {@code null}.
     */
    List<RemoteRepository> remoteRepositories();

    /**
     * Njord config, never {@code null}.
     */
    Config config();

    /**
     * Returns this instance as builder.
     */
    Builder toBuilder();

    /**
     * Creates empty builder.
     */
    static Builder builder() {
        return new Builder();
    }

    class Builder {
        private RepositorySystemSession session;
        private List<RemoteRepository> remoteRepositories;
        private Config config;

        public Builder session(RepositorySystemSession session) {
            this.session = requireNonNull(session);
            return this;
        }

        public Builder remoteRepositories(List<RemoteRepository> remoteRepositories) {
            this.remoteRepositories = requireNonNull(remoteRepositories);
            return this;
        }

        public Builder config(Config config) {
            this.config = requireNonNull(config);
            return this;
        }

        public SessionConfig build() {
            return new Impl(session, remoteRepositories, config);
        }

        private static class Impl implements SessionConfig {
            private final RepositorySystemSession session;
            private final List<RemoteRepository> remoteRepositories;
            private final Config config;

            private Impl(RepositorySystemSession session, List<RemoteRepository> remoteRepositories, Config config) {
                this.session = requireNonNull(session);
                this.remoteRepositories = List.copyOf(requireNonNull(remoteRepositories));
                this.config = requireNonNull(config);
            }

            @Override
            public RepositorySystemSession session() {
                return session;
            }

            @Override
            public List<RemoteRepository> remoteRepositories() {
                return remoteRepositories;
            }

            @Override
            public Config config() {
                return config;
            }

            @Override
            public Builder toBuilder() {
                return new Builder()
                        .session(session)
                        .remoteRepositories(remoteRepositories)
                        .config(config);
            }
        }
    }
}
