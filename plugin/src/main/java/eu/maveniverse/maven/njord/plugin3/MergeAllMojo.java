/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMerger;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Merges all stores onto one store, dropping all merged stores.
 * <p>
 * This is a special Mojo meant to be used in automation mostly. It assumes that Njord contains two or more
 * stores (probably imported) and all of them were created using same template. This mojo will gather all those stores
 * and merge them into one, resetting store name, so user will end up with one (merged) store named as
 * <pre>prefix-00001</pre>. In any other case, this mojo will fail and report error.
 */
@Mojo(name = "merge-all", threadSafe = true, requiresProject = false, aggregator = true)
public class MergeAllMojo extends NjordMojoSupport {
    /**
     * Fail if no store got merged, by default {@code true}.
     */
    @Parameter(required = true, property = "failIfNothingDone", defaultValue = "true")
    private boolean failIfNothingDone;

    /**
     * Parameter that may explicitly set the merge result store "origin project artifact". If this parameter
     * is not set (is left {@code null}), the first found origin project artifact will be used which is
     * usually what user wants.
     */
    @Parameter(property = "originProjectGav")
    private String originProjectGav;

    @Override
    protected void doWithSession(Session ns) throws IOException, MojoExecutionException {
        ArtifactStoreTemplate template = null;
        Artifact originProjectArtifact = originProjectGav != null ? new DefaultArtifact(originProjectGav) : null;
        HashSet<String> names = new HashSet<>();
        for (String name : ns.artifactStoreManager().listArtifactStoreNames()) {
            Optional<ArtifactStore> so = ns.artifactStoreManager().selectArtifactStore(name);
            if (so.isPresent()) {
                names.add(name);
                try (ArtifactStore store = so.orElseThrow(J8Utils.OET)) {
                    if (template == null) {
                        template = store.template();
                    } else if (!template.equals(store.template())) {
                        throw new MojoExecutionException("Conflicting templates used");
                    }
                    if (originProjectArtifact == null) {
                        originProjectArtifact = store.originProjectArtifact().orElse(null);
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
        try (ArtifactStore target = ns.artifactStoreManager().createArtifactStore(template, originProjectArtifact)) {
            logger.info("Created target store {}", target);
            targetName = target.name();
        }
        ArtifactStoreMerger merger = ns.artifactStoreMerger();
        for (String name : names) {
            Optional<ArtifactStore> so = ns.artifactStoreManager().selectArtifactStore(name);
            if (so.isPresent()) {
                try (ArtifactStore source = so.orElseThrow(J8Utils.OET);
                        ArtifactStore target = ns.artifactStoreManager()
                                .selectArtifactStore(targetName)
                                .orElseThrow(J8Utils.OET)) {
                    merger.merge(source, target);
                }
                logger.info("Dropping {}", name);
                ns.artifactStoreManager().dropArtifactStore(name);
            } else {
                throw new MojoExecutionException("Once found store is gone: " + name);
            }
        }
        logger.info("Renumbering stores");
        ns.artifactStoreManager().renumberArtifactStores();
    }
}
