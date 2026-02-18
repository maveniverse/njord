/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Njord specific repository listener: it may be silent by logging to DEBUG.
 */
public class NjordRepositoryListener extends AbstractRepositoryListener {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final boolean silent;

    public NjordRepositoryListener(boolean silent) {
        this.silent = silent;
    }

    @Override
    public void artifactDeployed(RepositoryEvent event) {
        if (silent) {
            logger.debug(
                    "Deployed {} to {}",
                    ArtifactIdUtils.toId(event.getArtifact()),
                    event.getRepository().getId());
        } else {
            logger.info(
                    "Deployed {} to {}",
                    ArtifactIdUtils.toId(event.getArtifact()),
                    event.getRepository().getId());
        }
    }

    @Override
    public void artifactInstalled(RepositoryEvent event) {
        if (silent) {
            logger.debug(
                    "Installed {} to local repository at {}",
                    ArtifactIdUtils.toId(event.getArtifact()),
                    event.getSession().getLocalRepository().getBasedir());
        } else {
            logger.info(
                    "Installed {} to local repository at {}",
                    ArtifactIdUtils.toId(event.getArtifact()),
                    event.getSession().getLocalRepository().getBasedir());
        }
    }
}
