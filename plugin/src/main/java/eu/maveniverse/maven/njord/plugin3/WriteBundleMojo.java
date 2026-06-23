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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Write out a store as "bundle" ZIP. The ZIP file has remote repository layout and contains all the artifacts and
 * metadata.
 * <p>
 * If the {@code store} parameter is not given, the latest (newest) store staged by the current project is used. The
 * bundle may be written into a directory (parameter {@code directory}, using {@code <store name>.zip} as file name),
 * or to an exact file (parameter {@code file}).
 */
@Mojo(name = "write-bundle", threadSafe = true, requiresProject = false, aggregator = true)
public class WriteBundleMojo extends NjordMojoSupport {
    /**
     * The name of the store to be written out. If not given, the latest (newest) store staged by the current
     * project is used.
     */
    @Parameter(property = SessionConfig.KEY_PREFIX + "store")
    private String store;

    /**
     * The directory to write out the bundle file into; the bundle file will be named {@code <store name>.zip}.
     * Ignored when {@link #file} is set. Either this or {@link #file} must be set.
     */
    @Parameter(property = SessionConfig.KEY_PREFIX + "directory")
    private String directory;

    /**
     * The exact file to write out the bundle to. Takes precedence over {@link #directory}; any missing parent
     * directories are created. Either this or {@link #directory} must be set.
     */
    @Parameter(property = SessionConfig.KEY_PREFIX + "file")
    private String file;

    @Override
    protected void doWithSession(Session ns) throws IOException, MojoExecutionException {
        if ((file == null || file.isEmpty()) && directory == null) {
            throw new MojoExecutionException("One of 'file' or 'directory' parameters must be set");
        }
        String storeName = store;
        if (storeName == null && ns.config().prefix().isPresent()) {
            List<String> candidates = ns.artifactStoreManager()
                    .listArtifactStoreNamesForPrefix(ns.config().prefix().orElseThrow(J8Utils.OET));
            if (!candidates.isEmpty()) {
                storeName = candidates.get(candidates.size() - 1);
                logger.info("No store specified; using latest staged store '{}'", storeName);
            }
        }
        if (storeName == null) {
            logger.warn("ArtifactStore not specified and none could be found");
            return;
        }
        Optional<ArtifactStore> storeOptional = ns.artifactStoreManager().selectArtifactStore(storeName);
        if (storeOptional.isPresent()) {
            try (ArtifactStore artifactStore = storeOptional.orElseThrow(J8Utils.OET)) {
                Path output = (file != null ? Paths.get(file) : Paths.get(directory)).toAbsolutePath();
                logger.info("Writing store {} as bundle to {}", artifactStore.name(), output);
                Path result = ns.artifactStoreWriter().writeAsBundle(artifactStore, output, file != null);
                logger.info("Written to " + result);
            }
        } else {
            logger.warn("ArtifactStore with given name not found: {}", storeName);
        }
    }
}
