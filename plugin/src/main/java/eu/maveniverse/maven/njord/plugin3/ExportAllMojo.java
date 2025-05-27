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
import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Export out all stores as "transportable bundle" to given path.
 */
@Mojo(name = "export-all", threadSafe = true, requiresProject = false, aggregator = true)
public class ExportAllMojo extends NjordMojoSupport {
    /**
     * The path to export to. It may be a directory, then the file name will be same as store name, or some
     * custom file name. In latter case is recommended to use same extension as Njord does (".ntb").
     */
    @Parameter(required = true, property = "path", defaultValue = ".")
    private String path;

    @Override
    protected void doWithSession(Session ns) throws IOException, MojoExecutionException {
        Path targetPath = SessionConfig.getCanonicalPath(Paths.get(path).toAbsolutePath());
        Files.createDirectories(targetPath);
        for (String name : ns.artifactStoreManager().listArtifactStoreNames()) {
            Optional<ArtifactStore> storeOptional = ns.artifactStoreManager().selectArtifactStore(name);
            if (storeOptional.isPresent()) {
                logger.info("Exporting store {} to {}", name, targetPath);
                Path bundle = ns.artifactStoreManager().exportTo(storeOptional.orElseThrow(J8Utils.OET), targetPath);
                logger.info("Exported to " + bundle);
            }
        }
    }
}
