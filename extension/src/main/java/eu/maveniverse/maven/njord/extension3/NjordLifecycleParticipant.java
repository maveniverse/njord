/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.extension3;

import static eu.maveniverse.maven.njord.shared.Config.DEFAULT_NJORD_AUTOPREFIX;
import static eu.maveniverse.maven.njord.shared.Config.NJORD_AUTOPREFIX;
import static eu.maveniverse.maven.njord.shared.Config.NJORD_PREFIX;
import static eu.maveniverse.maven.njord.shared.Config.NJORD_TARGET;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.NjordSession;
import eu.maveniverse.maven.njord.shared.NjordSessionFactory;
import eu.maveniverse.maven.njord.shared.NjordUtils;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import java.util.HashMap;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lifecycle participant that creates Njord config.
 */
@Singleton
@Named
public class NjordLifecycleParticipant extends AbstractMavenLifecycleParticipant {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final NjordSessionFactory njordSessionFactory;

    @Inject
    public NjordLifecycleParticipant(NjordSessionFactory njordSessionFactory) {
        this.njordSessionFactory = requireNonNull(njordSessionFactory);
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        try {
            HashMap<String, String> userProperties =
                    new HashMap<>(session.getRepositorySession().getUserProperties());
            HashMap<String, String> systemProperties =
                    new HashMap<>(session.getRepositorySession().getSystemProperties());
            MavenProject currentProject = session.getTopLevelProject();
            if (currentProject != null
                    && !"org.apache.maven:standalone-pom"
                            .equals(currentProject.getGroupId() + ":" + currentProject.getArtifactId())) {
                if (!userProperties.containsKey(NJORD_PREFIX) && !systemProperties.containsKey(NJORD_PREFIX)) {
                    boolean autoPrefix = Boolean.parseBoolean(
                            userProperties.getOrDefault(NJORD_AUTOPREFIX, DEFAULT_NJORD_AUTOPREFIX));
                    String prefix = currentProject.getProperties().getProperty(NJORD_PREFIX);
                    if (autoPrefix && prefix == null) {
                        prefix = currentProject.getArtifactId();
                    }
                    if (prefix != null) {
                        userProperties.put(NJORD_PREFIX, prefix);
                    }
                }
                if (!userProperties.containsKey(NJORD_TARGET) && !systemProperties.containsKey(NJORD_TARGET)) {
                    String target = currentProject.getProperties().getProperty(NJORD_TARGET);
                    if (target != null) {
                        userProperties.put(NJORD_TARGET, target);
                    }
                }
            }
            Config config = Config.defaults()
                    .userProperties(userProperties)
                    .systemProperties(systemProperties)
                    .build();
            SessionConfig sessionConfig = SessionConfig.builder()
                    .session(session.getRepositorySession())
                    .remoteRepositories(
                            RepositoryUtils.toRepos(session.getRequest().getRemoteRepositories()))
                    .config(config)
                    .build();
            if (NjordUtils.lazyInit(sessionConfig, njordSessionFactory::create)) {
                logger.info("Njord {} session created", config.version().orElse("UNKNOWN"));
            }
        } catch (Exception e) {
            throw new MavenExecutionException("Error enabling Njord", e);
        }
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        try {
            Optional<NjordSession> ns = NjordUtils.mayGetNjordSession(session.getRepositorySession());
            if (ns.isPresent()) {
                NjordSession njordSession = ns.orElseThrow();
                if (session.getResult().hasExceptions() && njordSession.dropSessionArtifactStores()) {
                    logger.warn("Session failed; dropped stores created in failed session");
                }
                logger.info("Njord session closed");
                njordSession.close();
            }
        } catch (Exception e) {
            throw new MavenExecutionException("Error closing Njord", e);
        }
    }
}
