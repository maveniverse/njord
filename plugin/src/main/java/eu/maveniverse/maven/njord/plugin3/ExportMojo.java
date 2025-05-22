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
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Export out a store as "transportable bundle" to given path.
 */
@Mojo(name = "export", threadSafe = true, requiresProject = false, aggregator = true)
public class ExportMojo extends NjordMojoSupport {
    /**
     * The name of the store to export.
     */
    @Parameter(required = true, property = "store")
    private String store;

    /**
     * The path to export to. It may be a directory, then the file name will be same as store name, or some
     * custom file name. In latter case is recommended to use same extension as Njord does (".ntb").
     */
    @Parameter(required = true, property = "path", defaultValue = ".")
    private String path;

    @Override
    protected void doWithSession(Session ns) throws IOException, MojoExecutionException {
        Optional<ArtifactStore> storeOptional = ns.artifactStoreManager().selectArtifactStore(store);
        if (storeOptional.isPresent()) {
            Path targetPath = SessionConfig.getCanonicalPath(Paths.get(path).toAbsolutePath());
            logger.info("Exporting store {} to {}", store, targetPath);
            Path bundle = ns.artifactStoreManager().exportTo(storeOptional.orElseThrow(J8Utils.OET), targetPath);
            logger.info("Exported to " + bundle);
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
