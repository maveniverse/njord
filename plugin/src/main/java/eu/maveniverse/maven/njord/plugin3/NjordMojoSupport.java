/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.NjordUtils;
import eu.maveniverse.maven.njord.shared.Session;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NjordMojoSupport extends AbstractMojo {
    protected final Logger logger = LoggerFactory.getLogger(NjordMojoSupport.class);

    @Inject
    protected MavenSession mavenSession;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Optional<Session> njordSession = NjordUtils.mayGetNjordSession(mavenSession.getRepositorySession());
        if (njordSession.isEmpty()) {
            throw new MojoExecutionException("Njord extension is not installed");
        }
        try (Session ns = njordSession.orElseThrow()) {
            if (ns.config().enabled()) {
                doExecute(ns);
            } else {
                logger.info("Njord is disabled");
            }
        } catch (IOException e) {
            throw new MojoFailureException(e);
        }
    }

    protected abstract void doExecute(Session ns) throws IOException, MojoExecutionException, MojoFailureException;
}
