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
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

/**
 * Lists content of given store.
 */
@Mojo(name = "list-content", threadSafe = true, requiresProject = false, aggregator = true)
public class ListContentMojo extends NjordMojoSupport {
    /**
     * The name of the store to have content listed.
     */
    @Parameter(required = true, property = SessionConfig.KEY_PREFIX + "store")
    private String store;

    /**
     * Optional; file to write GAVs (each on new line). Useful for automations where the list of artifacts
     * is needed.
     *
     * @since 0.8.0
     */
    @Parameter(property = SessionConfig.KEY_PREFIX + "file")
    private String file;

    @Override
    protected void doWithSession(Session ns) throws IOException {
        Optional<ArtifactStore> storeOptional = ns.artifactStoreManager().selectArtifactStore(store);
        if (storeOptional.isPresent()) {
            try (ArtifactStore store = storeOptional.orElseThrow(J8Utils.OET)) {
                logger.info("List contents of ArtifactStore {}", store);
                AtomicInteger counter = new AtomicInteger();
                try (BufferedWriter writer =
                        file != null ? Files.newBufferedWriter(Paths.get(file), StandardCharsets.UTF_8) : null) {
                    for (Artifact artifact : store.artifacts()) {
                        logger.info("{}. {}", counter.incrementAndGet(), ArtifactIdUtils.toId(artifact));
                        if (writer != null) {
                            writer.write(ArtifactIdUtils.toId(artifact));
                            writer.newLine();
                        }
                    }
                }
            }
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
