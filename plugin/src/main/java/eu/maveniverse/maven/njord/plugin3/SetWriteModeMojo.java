/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.store.WriteMode;
import java.io.IOException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Sets store write mode (by default to read-only).
 */
@Mojo(name = "set-write-mode", threadSafe = true, requiresProject = false, aggregator = true)
public class SetWriteModeMojo extends NjordMojoSupport {
    /**
     * The name of the store to set read-only.
     */
    @Parameter(required = true, property = SessionConfig.KEY_PREFIX + "store")
    private String store;

    /**
     * The write mode of the store to apply.
     */
    @Parameter(required = true, property = SessionConfig.KEY_PREFIX + "writeMode", defaultValue = "READ_ONLY")
    private String writeMode;

    @Override
    protected void doWithSession(Session ns) throws IOException {
        if (ns.artifactStoreManager().updateWriteModeArtifactStore(store, WriteMode.valueOf(writeMode))) {
            logger.info("ArtifactStore {} set read-only", store);
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
