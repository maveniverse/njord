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
import java.util.Optional;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Deletes attachment from the given store.
 */
@Mojo(name = "attachment-delete", threadSafe = true, requiresProject = false, aggregator = true)
public class AttachmentDeleteMojo extends NjordMojoSupport {
    /**
     * The name of the store to delete attachment from. Mandatory parameter.
     */
    @Parameter(required = true, property = SessionConfig.KEY_PREFIX + "store")
    private String store;

    /**
     * The attachment name to delete. Mandatory parameter.
     */
    @Parameter(required = true, property = SessionConfig.KEY_PREFIX + "attachmentName")
    private String attachmentName;

    @Override
    protected void doWithSession(Session ns) throws MojoFailureException, IOException {
        Optional<ArtifactStore> aso = ns.artifactStoreManager().selectArtifactStore(store);
        if (aso.isPresent()) {
            try (ArtifactStore artifactStore = aso.orElseThrow(J8Utils.OET)) {
                if (artifactStore.attachmentPresent(attachmentName)) {
                    try (ArtifactStore.AttachmentOperation op = artifactStore.manageAttachment(attachmentName)) {
                        op.delete();
                    }
                } else {
                    logger.warn("ArtifactStore {} has no attachment with name {}", store, attachmentName);
                }
            }
            logger.info("Deleted attachment {} from ArtifactStore {}", attachmentName, store);
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
