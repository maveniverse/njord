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
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Writes out a store as "remote Maven repository" directory structure to given directory.
 */
@Mojo(name = "write", threadSafe = true, requiresProject = false, aggregator = true)
public class WriteMojo extends NjordMojoSupport {
    /**
     * The name of the store to be written out.
     */
    @Parameter(required = true, property = SessionConfig.KEY_PREFIX + "store")
    private String store;

    /**
     * The directory to write out the store to.
     */
    @Parameter(required = true, property = SessionConfig.KEY_PREFIX + "directory")
    private String directory;

    @Override
    protected void doWithSession(Session ns) throws IOException, MojoExecutionException {
        Optional<ArtifactStore> storeOptional = ns.artifactStoreManager().selectArtifactStore(store);
        if (storeOptional.isPresent()) {
            Path targetDirectory = FileUtils.canonicalPath(Paths.get(directory).toAbsolutePath());
            if (Files.exists(targetDirectory)) {
                throw new MojoExecutionException("Exporting to existing directory not supported");
            }
            logger.info("Writing store {} as directory to {}", store, directory);
            Path result =
                    ns.artifactStoreWriter().writeAsDirectory(storeOptional.orElseThrow(J8Utils.OET), targetDirectory);
            logger.info("Written to " + result);
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
