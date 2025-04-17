/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.NjordSession;
import java.io.IOException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Drops given store.
 */
@Mojo(name = "drop", threadSafe = true, requiresProject = false)
public class DropMojo extends NjordMojoSupport {
    @Parameter(required = true, property = "store")
    private String store;

    @Override
    protected void doExecute(NjordSession ns) throws IOException {
        if (ns.artifactStoreManager().dropArtifactStore(store)) {
            logger.info("Dropped ArtifactStore {}", store);
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
