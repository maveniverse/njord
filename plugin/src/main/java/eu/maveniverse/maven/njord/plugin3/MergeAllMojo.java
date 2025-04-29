/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.NjordSession;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMerger;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Merges all stores onto one store, dropping all merged stores.
 * <p>
 * This is a special Mojo meant to be used in automation mostly. It assumes that Njord contains two or more
 * stores (probably imported) and all of them were created using same template. This mojo will gather all those stores
 * and merge them into one, resetting store name, so user will end up with one (merged) store named as
 * <pre>prefix-00001</pre>. In any other case, this mojo will fail and report error.
 */
@Mojo(name = "merge-all", threadSafe = true, requiresProject = false)
public class MergeAllMojo extends NjordMojoSupport {
    /**
     * Fail if no store got merged, by default {@code true}.
     */
    @Parameter(required = true, property = "failIfNothingDone", defaultValue = "true")
    private boolean failIfNothingDone;

    @Override
    protected void doExecute(NjordSession ns) throws IOException, MojoExecutionException {
        ArtifactStoreTemplate template = null;
        HashSet<String> names = new HashSet<>();
        for (String name : ns.artifactStoreManager().listArtifactStoreNames()) {
            Optional<ArtifactStore> so = ns.artifactStoreManager().selectArtifactStore(name);
            if (so.isPresent()) {
                names.add(name);
                try (ArtifactStore store = so.orElseThrow()) {
                    if (template == null) {
                        template = store.template();
                    } else if (!template.equals(store.template())) {
                        throw new MojoExecutionException("Conflicting templates used");
                    }
                }
            }
        }
        if (template == null) {
            if (failIfNothingDone) {
                throw new MojoExecutionException("Nothing to merge");
            } else {
                return;
            }
        }

        String targetName;
        try (ArtifactStore target = ns.artifactStoreManager().createArtifactStore(template)) {
            logger.info("Created target store {}", target);
            targetName = target.name();
        }
        try (ArtifactStoreMerger merger = ns.createArtifactStoreMerger()) {
            for (String name : names) {
                Optional<ArtifactStore> so = ns.artifactStoreManager().selectArtifactStore(name);
                if (so.isPresent()) {
                    try (ArtifactStore source = so.orElseThrow();
                            ArtifactStore target = ns.artifactStoreManager()
                                    .selectArtifactStore(targetName)
                                    .orElseThrow()) {
                        merger.merge(source, target);
                    }
                    logger.info("Dropping {}", name);
                    ns.artifactStoreManager().dropArtifactStore(name);
                } else {
                    throw new MojoExecutionException("Once found store is gone: " + name);
                }
            }
        }
        logger.info("Renumbering stores");
        ns.artifactStoreManager().renumberArtifactStores();
    }
}
