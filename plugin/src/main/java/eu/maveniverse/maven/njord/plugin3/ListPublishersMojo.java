/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.NjordSession;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Lists available publishers.
 */
@Mojo(name = "list-publishers", threadSafe = true, requiresProject = false)
public class ListPublishersMojo extends NjordMojoSupport {
    @Override
    protected void doExecute(NjordSession ns) {
        logger.info("Listing available publishers:");
        for (ArtifactStorePublisher publisher : ns.availablePublishers()) {
            logger.info("- '{}' -> {}", publisher.name(), publisher.description());
            if (publisher.targetReleaseRepository().isPresent()
                    || publisher.targetSnapshotRepository().isPresent()) {
                logger.info("  Published artifacts will be available from:");
                logger.info(
                        "    RELEASES:  {}",
                        fmt(publisher.targetReleaseRepository().orElse(null)));
                logger.info(
                        "    SNAPSHOTS: {}",
                        fmt(publisher.targetSnapshotRepository().orElse(null)));
            }
            logger.info("  Service endpoints:");
            logger.info(
                    "    RELEASES:  {}",
                    fmt(publisher.serviceReleaseRepository().orElse(null)));
            logger.info(
                    "    SNAPSHOTS: {}",
                    fmt(publisher.serviceSnapshotRepository().orElse(null)));
        }
    }

    private String fmt(RemoteRepository repo) {
        if (repo == null) {
            return "n/a";
        } else {
            return repo.getId() + " @ " + repo.getUrl();
        }
    }
}
