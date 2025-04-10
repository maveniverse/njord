/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.extension3;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.NjordUtils;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystemSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lifecycle participant that creates Njord config.
 */
@Singleton
@Named
public class NjordLifecycleParticipant extends AbstractMavenLifecycleParticipant {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
        try {
            RepositorySystemSession repoSession = session.getRepositorySession();
            if (NjordUtils.lazyInitConfig(repoSession, () -> Config.defaults()
                    .userProperties(repoSession.getUserProperties())
                    .systemProperties(repoSession.getSystemProperties())
                    .build())) {
                logger.info("Njord enabled");
            }
        } catch (Exception e) {
            throw new MavenExecutionException("Error enabling Njord", e);
        }
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        try {
            Optional<ArtifactStoreManager> asm = NjordUtils.mayGetArtifactStoreManager(session.getRepositorySession());
            if (asm.isPresent()) {
                asm.orElseThrow().close();
            }
        } catch (Exception e) {
            throw new MavenExecutionException("Error closing Njord", e);
        }
    }
}
