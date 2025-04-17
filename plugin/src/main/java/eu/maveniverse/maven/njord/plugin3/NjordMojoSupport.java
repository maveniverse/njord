/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.NjordSession;
import eu.maveniverse.maven.njord.shared.NjordSessionFactory;
import eu.maveniverse.maven.njord.shared.NjordUtils;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.aether.RepositorySystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NjordMojoSupport extends AbstractMojo {
    protected final Logger logger = LoggerFactory.getLogger(NjordMojoSupport.class);

    @Inject
    protected MavenSession mavenSession;

    @Inject
    protected RepositorySystem repositorySystem;

    @Inject
    protected NjordSessionFactory njordSessionFactory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Config config = Config.defaults()
                .userProperties(mavenSession.getRepositorySession().getUserProperties())
                .systemProperties(mavenSession.getRepositorySession().getSystemProperties())
                .build();
        SessionConfig sessionConfig = SessionConfig.builder()
                .session(mavenSession.getRepositorySession())
                .remoteRepositories(
                        RepositoryUtils.toRepos(mavenSession.getRequest().getRemoteRepositories()))
                .config(config)
                .build();
        if (NjordUtils.lazyInit(sessionConfig, njordSessionFactory::create)) {
            logger.info("Njord {} session created", config.version().orElse("UNKNOWN"));
        }
        Optional<NjordSession> njordSession = NjordUtils.mayGetNjordSession(mavenSession.getRepositorySession());
        if (njordSession.isEmpty()) {
            logger.warn("Njord not configured or explicitly disabled");
            return;
        }
        try (NjordSession ns = njordSession.orElseThrow()) {
            doExecute(ns);
        } catch (IOException e) {
            throw new MojoFailureException(e);
        }
    }

    protected abstract void doExecute(NjordSession ns) throws IOException, MojoExecutionException, MojoFailureException;
}
