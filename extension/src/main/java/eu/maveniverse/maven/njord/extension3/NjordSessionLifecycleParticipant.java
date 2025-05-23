/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.extension3;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.NjordUtils;
import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.SessionFactory;
import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lifecycle participant that creates Njord session.
 */
@Singleton
@Named
public class NjordSessionLifecycleParticipant extends AbstractMavenLifecycleParticipant {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Provider<SessionFactory> sessionFactoryProvider;

    @Inject
    public NjordSessionLifecycleParticipant(Provider<SessionFactory> sessionFactoryProvider) {
        this.sessionFactoryProvider = requireNonNull(sessionFactoryProvider);
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        requireNonNull(session);
        try {
            // session config
            SessionConfig sc = SessionConfig.defaults(
                            session.getRepositorySession(),
                            RepositoryUtils.toRepos(session.getRequest().getRemoteRepositories()))
                    .currentProject(SessionConfig.fromMavenProject(session.getTopLevelProject()))
                    .build();

            Session ns = NjordUtils.lazyInit(sc, sessionFactoryProvider.get()::create);
            if (ns.config().enabled()) {
                logger.info("Njord {} session created", ns.config().version().orElse("UNKNOWN"));
            }
        } catch (Exception e) {
            throw new MavenExecutionException("Error enabling Njord", e);
        }
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        requireNonNull(session);
        try {
            Optional<Session> ns = NjordUtils.mayGetNjordSession(session.getRepositorySession());
            if (ns.isPresent()) {
                Session njordSession = ns.orElseThrow(J8Utils.OET);
                if (njordSession.config().enabled()) {
                    if (session.getResult().hasExceptions()) {
                        int dropped = njordSession.dropSessionArtifactStores();
                        if (dropped != 0) {
                            logger.warn("Session failed; dropped {} stores created in this session", dropped);
                        }
                    } else if (njordSession.config().autoPublish()) {
                        int published = njordSession.publishSessionArtifactStores();
                        if (published != 0) {
                            logger.info("Auto publish: Published {} stores created in this session", published);
                        } else {
                            logger.info("Auto publish: No stores created in this session");
                        }
                    }
                    logger.info("Njord session closed");
                }
                njordSession.close();
            }
        } catch (Exception e) {
            throw new MavenExecutionException("Error closing Njord", e);
        }
    }
}
