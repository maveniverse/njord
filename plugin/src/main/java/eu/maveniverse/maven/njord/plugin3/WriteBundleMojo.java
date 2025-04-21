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
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Write out a store as "bundle" ZIP to given path. The ZIP file has remote repository layout and contains all the
 * artifacts and metadata.
 */
@Mojo(name = "write-bundle", threadSafe = true, requiresProject = false)
public class WriteBundleMojo extends NjordMojoSupport {
    /**
     * The name of the store to be written out.
     */
    @Parameter(required = true, property = "store")
    private String store;

    /**
     * The directory to write out the bundle file.
     */
    @Parameter(required = true, property = "directory")
    private String directory;

    @Override
    protected void doExecute(NjordSession ns) throws IOException, MojoExecutionException {
        Optional<ArtifactStore> storeOptional = ns.artifactStoreManager().selectArtifactStore(store);
        if (storeOptional.isPresent()) {
            Path targetDirectory = Config.getCanonicalPath(Path.of(directory).toAbsolutePath());
            if (!Files.isDirectory(targetDirectory)) {
                Files.createDirectories(targetDirectory);
            }
            Path result;
            logger.info("Writing store {} as bundle to {}", store, directory);
            try (ArtifactStoreWriter artifactStoreWriter = ns.createArtifactStoreWriter()) {
                result = artifactStoreWriter.writeAsBundle(storeOptional.orElseThrow(), targetDirectory);
            }
            logger.info("Written to " + result);
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
