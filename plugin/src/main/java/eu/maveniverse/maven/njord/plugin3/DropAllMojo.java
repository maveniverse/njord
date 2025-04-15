/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.NjordSession;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Tabula rasa: drops all stores. For safety reasons, you need extra {@code -Dyes}.
 */
@Mojo(name = "drop-all", threadSafe = true, requiresProject = false)
public class DropAllMojo extends NjordMojoSupport {
    @Parameter(required = true, property = "yes")
    private boolean yes;

    @Override
    protected void doExecute(NjordSession ns) throws IOException {
        if (yes) {
            logger.info("Dropping all ArtifactStore");
            AtomicInteger count = new AtomicInteger();
            for (String name : ns.artifactStoreManager().listArtifactStoreNames()) {
                Optional<ArtifactStore> artifactStore =
                        ns.artifactStoreManager().selectArtifactStore(name);
                if (artifactStore.isPresent()) {
                    ArtifactStore store = artifactStore.orElseThrow();
                    logger.info("{}. dropping {}", count.incrementAndGet(), store);
                    ns.artifactStoreManager().dropArtifactStore(store);
                }
            }
            logger.info("Dropped total of {} ArtifactStore", count.get());
        } else {
            logger.warn("Not dropping all: you must add extra `-Dyes` to agree on consequences");
        }
    }
}
