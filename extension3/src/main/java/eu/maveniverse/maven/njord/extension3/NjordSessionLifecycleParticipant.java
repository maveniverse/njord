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
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystemSession;
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
            if (isExplicitlyDisabled(session)) {
                logger.debug("Njord disabled via properties, skipping session initialization");
                return;
            }
            // session config
            SessionConfig sc = SessionConfig.defaults(
                            session.getRepositorySession(),
                            RepositoryUtils.toRepos(session.getRequest().getRemoteRepositories()))
                    .currentProject(SessionConfig.fromMavenProject(session.getTopLevelProject()))
                    .build();
            if (sc.enabled()) {
                NjordUtils.lazyInit(
                        session.getRepositorySession(),
                        () -> sessionFactoryProvider.get().create(sc));
            } else {
                logger.info("Njord {} disabled", sc.version());
            }
        } catch (Exception e) {
            if ("com.google.inject.ProvisionException".equals(e.getClass().getName())) {
                logger.error("Njord session creation failed", e); // here runtime req will kick in
            } else {
                throw new MavenExecutionException("Error enabling Njord", e);
            }
        }
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        requireNonNull(session);
        RepositorySystemSession repositorySession = session.getRepositorySession();
        try {
            Optional<Session> ns = NjordUtils.mayGetNjordSession(repositorySession);
            if (ns.isPresent()) {
                try (Session njordSession = ns.orElseThrow(J8Utils.OET)) {
                    if (njordSession.config().autoPublish()) {
                        if (session.getResult().hasExceptions()) {
                            if (njordSession.config().autoDrop()) {
                                int dropped = njordSession.dropSessionArtifactStores();
                                if (dropped != 0) {
                                    logger.warn(
                                            "Njord auto publish: Session failed; dropped {} stores created in this session",
                                            dropped);
                                } else {
                                    logger.warn(
                                            "Njord auto publish: Session failed; no stores created in this session");
                                }
                            } else {
                                logger.warn(
                                        "Njord auto publish: Session failed; stores created in this session, if any, are not dropped");
                            }
                        } else {
                            try {
                                int published = njordSession.publishSessionArtifactStores();
                                if (published != 0) {
                                    logger.info(
                                            "Njord auto publish: Published {} stores created in this session",
                                            published);
                                    if (njordSession.config().autoDrop()) {
                                        int dropped = njordSession.dropSessionArtifactStores();
                                        if (dropped != 0) {
                                            logger.info(
                                                    "Njord auto publish: Dropped {} auto published stores", dropped);
                                        }
                                    } else {
                                        logger.info(
                                                "Njord auto publish: The {} published stores are not dropped",
                                                published);
                                    }
                                } else {
                                    logger.info("Njord auto publish: No stores created in this session");
                                }
                            } catch (ArtifactStorePublisher.PublishFailedException e) {
                                throw new MavenExecutionException(e.getMessage(), e);
                            }
                        }
                    }
                }
                logger.info("Njord session closed");
            }
        } catch (IOException e) {
            throw new MavenExecutionException("Error closing Njord", e);
        } finally {
            NjordUtils.removeNjordSession(repositorySession);
        }
    }

    private static boolean isExplicitlyDisabled(MavenSession session) {
        String userProperty = session.getUserProperties().getProperty(SessionConfig.CONFIG_ENABLED);
        if (userProperty != null) {
            return !Boolean.parseBoolean(userProperty);
        }
        String systemProperty = session.getSystemProperties().getProperty(SessionConfig.CONFIG_ENABLED);
        if (systemProperty != null) {
            return !Boolean.parseBoolean(systemProperty);
        }
        return false;
    }
}
