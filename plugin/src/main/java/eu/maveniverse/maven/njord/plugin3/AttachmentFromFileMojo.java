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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Adds attachment to the given store.
 */
@Mojo(name = "attachment-from-file", threadSafe = true, requiresProject = false, aggregator = true)
public class AttachmentFromFileMojo extends NjordMojoSupport {
    /**
     * The name of the store to add attachment to. Mandatory parameter.
     */
    @Parameter(required = true, property = SessionConfig.KEY_PREFIX + "store")
    private String store;

    /**
     * The attachment file. Mandatory parameter, must point to existing regular file.
     */
    @Parameter(required = true, property = SessionConfig.KEY_PREFIX + "attachmentFile")
    private File attachmentFile;

    /**
     * The attachment name. Optional parameter, if not present, attachment file name will be used.
     */
    @Parameter(property = SessionConfig.KEY_PREFIX + "attachmentName")
    private String attachmentName;

    @Override
    protected void doWithSession(Session ns) throws MojoFailureException, IOException {
        Path attachment = attachmentFile.toPath();
        if (!Files.isRegularFile(attachment)) {
            throw new MojoFailureException("Attachment is not a regular file");
        }
        if (attachmentName == null) {
            attachmentName = attachment.getFileName().toString();
        }
        Optional<ArtifactStore> aso = ns.artifactStoreManager().selectArtifactStore(store);
        if (aso.isPresent()) {
            try (ArtifactStore artifactStore = aso.orElseThrow(J8Utils.OET);
                    ArtifactStore.AttachmentOperation op = artifactStore.manageAttachment(attachmentName)) {
                op.write(Files.newInputStream(attachment));
            }
            logger.info("Added attachment {} to ArtifactStore {}", attachmentName, store);
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
