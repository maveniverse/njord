/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.NjordSession;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.util.Optional;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Publishes given store to given target.
 */
@Mojo(name = "publish", threadSafe = true, requiresProject = false)
public class PublishMojo extends NjordMojoSupport {
    @Parameter(required = true, property = "store")
    private String store;

    @Parameter(required = true, property = "target")
    private String target;

    @Parameter(required = true, property = "drop", defaultValue = "false")
    private boolean drop;

    @Override
    protected void doExecute(NjordSession ns) throws IOException, MojoFailureException {
        Optional<ArtifactStore> storeOptional = ns.artifactStoreManager().selectArtifactStore(store);
        if (storeOptional.isEmpty()) {
            logger.warn("ArtifactStore with given name not found: {}", store);
            return;
        }
        Optional<ArtifactStorePublisher> po = ns.selectArtifactStorePublisher(target);
        if (po.isPresent()) {
            try (ArtifactStore from = storeOptional.orElseThrow()) {
                po.orElseThrow().publish(from);
            }
            if (drop) {
                logger.info("Dropping {}", store);
                ns.artifactStoreManager().dropArtifactStore(store);
            }
        } else {
            throw new MojoFailureException("Publisher not found");
        }
    }
}
