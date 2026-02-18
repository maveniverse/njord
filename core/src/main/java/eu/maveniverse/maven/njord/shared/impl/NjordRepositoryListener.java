/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Njord specific repository listener: it may be silent by logging to DEBUG.
 */
public class NjordRepositoryListener extends AbstractRepositoryListener implements Closeable {
    public enum Mode {
        PER_EVENT_SILENT(true, false),
        PER_EVENT(false, false),
        AGGREGATED_SILENT(true, true),
        AGGREGATED(false, true);

        private final boolean silent;
        private final boolean aggregated;

        Mode(boolean silent, boolean aggregated) {
            this.silent = silent;
            this.aggregated = aggregated;
        }
    }

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Mode mode;
    private final ConcurrentMap<ArtifactRepository, List<Artifact>> events;

    public NjordRepositoryListener(Mode mode) {
        this.mode = requireNonNull(mode);
        this.events = new ConcurrentHashMap<>();
    }

    @Override
    public void close() {
        if (mode.aggregated) {
            int total = 0;
            for (Map.Entry<ArtifactRepository, List<Artifact>> entry : events.entrySet()) {
                ArtifactRepository repository = entry.getKey();
                List<Artifact> artifacts = entry.getValue();
                total += artifacts.size();
                if (mode.silent) {
                    logger.debug("Published {} artifact(s) to {}", artifacts.size(), repository.getId());
                } else {
                    logger.info("Published {} artifact(s) to {}", artifacts.size(), repository.getId());
                }
            }
            if (events.size() > 1) {
                if (mode.silent) {
                    logger.debug("Published total {} artifact(s) to {} repositories", total, events.size());
                } else {
                    logger.info("Published total {} artifact(s) to {} repositories", total, events.size());
                }
            }
        }
    }

    @Override
    public void artifactDeployed(RepositoryEvent event) {
        if (mode.aggregated) {
            events.compute(event.getRepository(), (k, v) -> {
                if (v == null) {
                    v = new ArrayList<>();
                }
                v.add(event.getArtifact());
                return v;
            });
        } else {
            if (mode.silent) {
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
    }

    @Override
    public void artifactInstalled(RepositoryEvent event) {
        if (mode.aggregated) {
            events.compute(event.getRepository(), (k, v) -> {
                if (v == null) {
                    v = new ArrayList<>();
                }
                v.add(event.getArtifact());
                return v;
            });
        } else {
            if (mode.silent) {
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
}
