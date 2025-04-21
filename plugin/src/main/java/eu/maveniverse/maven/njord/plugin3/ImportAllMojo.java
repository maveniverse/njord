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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Imports all stores from "transportable bundle" files found in given directory.
 */
@Mojo(name = "import-all", threadSafe = true, requiresProject = false)
public class ImportAllMojo extends NjordMojoSupport {
    /**
     * The directory to import from, by default is current directory.
     */
    @Parameter(required = true, property = "dir", defaultValue = ".")
    private String dir;

    @Override
    protected void doExecute(NjordSession ns) throws IOException, MojoExecutionException {
        Path sourceDirectory = Config.getCanonicalPath(Path.of(dir).toAbsolutePath());
        if (!Files.isDirectory(sourceDirectory)) {
            throw new MojoExecutionException("Import directory does not exist");
        }
        logger.info("Importing stores from {}", sourceDirectory);
        List<Path> bundles;
        try (Stream<Path> stream = Files.list(sourceDirectory)
                .filter(p -> p.getFileName().toString().endsWith(".ntb") && Files.isRegularFile(p))) {
            bundles = stream.toList();
        }
        for (Path bundle : bundles) {
            try (ArtifactStore artifactStore = ns.artifactStoreManager().importFrom(bundle)) {
                logger.info("Imported to " + artifactStore);
            }
        }
    }
}
