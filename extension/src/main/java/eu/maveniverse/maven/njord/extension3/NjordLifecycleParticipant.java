/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.extension3;

import static eu.maveniverse.maven.njord.shared.impl.Utils.toMap;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.NjordUtils;
import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.SessionFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lifecycle participant that creates Njord config.
 */
@Singleton
@Named
public class NjordLifecycleParticipant extends AbstractMavenLifecycleParticipant {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SessionFactory sessionFactory;

    @Inject
    public NjordLifecycleParticipant(SessionFactory sessionFactory) {
        this.sessionFactory = requireNonNull(sessionFactory);
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        try {
            SessionConfig.TopLevelProject topLevelProject = null;
            MavenProject currentProject = session.getTopLevelProject();
            if (currentProject != null
                    && !"org.apache.maven:standalone-pom"
                            .equals(currentProject.getGroupId() + ":" + currentProject.getArtifactId())) {
                Artifact tlpArtifact = RepositoryUtils.toArtifact(currentProject.getArtifact());
                Map<String, String> tlpProperties = Map.copyOf(toMap(currentProject.getProperties()));
                List<RemoteRepository> tlpRemoteRepositories =
                        List.copyOf(currentProject.getRemoteProjectRepositories());

                topLevelProject = new SessionConfig.TopLevelProject() {
                    @Override
                    public Artifact artifact() {
                        return tlpArtifact;
                    }

                    @Override
                    public Map<String, String> properties() {
                        return tlpProperties;
                    }

                    @Override
                    public List<RemoteRepository> remoteRepositories() {
                        return tlpRemoteRepositories;
                    }
                };
            }

            // collect all + top level POM (needed to build eff models, parent may be ext in some 3rd party repo)
            ArrayList<RemoteRepository> remoteRepositories =
                    new ArrayList<>(RepositoryUtils.toRepos(session.getRequest().getRemoteRepositories()));
            if (topLevelProject != null) {
                remoteRepositories.addAll(topLevelProject.remoteRepositories());
            }

            // session config
            SessionConfig sc = SessionConfig.defaults(session.getRepositorySession(), remoteRepositories)
                    .topLevelProject(topLevelProject)
                    .build();

            // we may need to customize session config
            if (topLevelProject != null) {
                if (sc.prefix().isEmpty()) {
                    String prefix = topLevelProject.properties().get(SessionConfig.CONFIG_PREFIX);
                    if (prefix == null && sc.autoPrefix()) {
                        prefix = topLevelProject.artifact().getArtifactId();
                    }
                    if (prefix != null) {
                        sc = sc.toBuilder().prefix(prefix).build();
                    }
                }
                if (sc.publisher().isEmpty()) {
                    String publisher = topLevelProject.properties().get(SessionConfig.CONFIG_PUBLISHER);
                    if (publisher != null) {
                        sc = sc.toBuilder().publisher(publisher).build();
                    }
                }
            }

            Session ns = NjordUtils.init(sc, sessionFactory::create);
            if (ns.config().enabled()) {
                logger.info("Njord {} session created", ns.config().version().orElse("UNKNOWN"));
            }
        } catch (Exception e) {
            throw new MavenExecutionException("Error enabling Njord", e);
        }
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        try {
            Optional<Session> ns = NjordUtils.mayGetNjordSession(session.getRepositorySession());
            if (ns.isPresent()) {
                Session njordSession = ns.orElseThrow();
                if (njordSession.config().enabled()) {
                    if (session.getResult().hasExceptions() && njordSession.dropSessionArtifactStores()) {
                        logger.warn("Session failed; dropped stores created in failed session");
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
