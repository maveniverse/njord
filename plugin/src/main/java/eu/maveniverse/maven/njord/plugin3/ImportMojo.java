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
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Imports a store from "transportable bundle".
 */
@Mojo(name = "import", threadSafe = true, requiresProject = false)
public class ImportMojo extends NjordMojoSupport {
    /**
     * The bundle file to import.
     */
    @Parameter(required = true, property = "file")
    private String file;

    @Override
    protected void doWithSession(Session ns) throws IOException, MojoExecutionException {
        Path source = SessionConfig.getCanonicalPath(Path.of(file).toAbsolutePath());
        if (!Files.exists(source)) {
            throw new MojoExecutionException("Import file not found: " + file);
        }
        logger.info("Importing store from {}", source);
        try (ArtifactStore artifactStore = ns.artifactStoreManager().importFrom(source)) {
            logger.info("Imported to " + artifactStore);
        }
    }
}
