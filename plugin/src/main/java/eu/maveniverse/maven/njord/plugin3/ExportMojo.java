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
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreExporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Exports a store as "remote Maven repository" directory structure to given directory.
 */
@Mojo(name = "export", threadSafe = true, requiresProject = false)
public class ExportMojo extends NjordMojoSupport {
    @Parameter(required = true, property = "store")
    private String store;

    @Parameter(required = true, property = "directory")
    private String directory;

    @Override
    protected void doExecute(NjordSession ns) throws IOException, MojoExecutionException {
        Optional<ArtifactStore> storeOptional = ns.artifactStoreManager().selectArtifactStore(store);
        if (storeOptional.isPresent()) {
            Path targetDirectory = Config.getCanonicalPath(Path.of(directory).toAbsolutePath());
            if (Files.exists(targetDirectory)) {
                throw new MojoExecutionException("Exporting to existing directory not supported");
            }
            try (ArtifactStoreExporter artifactStoreExporter = ns.createArtifactStoreExporter()) {
                artifactStoreExporter.exportAsDirectory(storeOptional.orElseThrow(), targetDirectory);
            }
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
