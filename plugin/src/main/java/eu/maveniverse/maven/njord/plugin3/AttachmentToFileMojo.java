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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Writes out attachment of the given store to a file.
 */
@Mojo(name = "attachment-to-file", threadSafe = true, requiresProject = false, aggregator = true)
public class AttachmentToFileMojo extends NjordMojoSupport {
    /**
     * The name of the store to write out attachment from. Mandatory parameter.
     */
    @Parameter(required = true, property = SessionConfig.KEY_PREFIX + "store")
    private String store;

    /**
     * The file to write attachment to. Mandatory parameter. If file exists, will be overwritten.
     */
    @Parameter(required = true, property = SessionConfig.KEY_PREFIX + "attachmentFile")
    private File attachmentFile;

    /**
     * The attachment name. Optional parameter, if not present, attachment file name will be used.
     */
    @Parameter(property = SessionConfig.KEY_PREFIX + "attachmentName")
    private String attachmentName;

    @Override
    protected void doWithSession(Session ns) throws IOException {
        Path attachment = attachmentFile.toPath();
        Files.createDirectories(attachment.getParent());
        if (attachmentName == null) {
            attachmentName = attachment.getFileName().toString();
        }
        Optional<ArtifactStore> aso = ns.artifactStoreManager().selectArtifactStore(store);
        if (aso.isPresent()) {
            try (ArtifactStore artifactStore = aso.orElseThrow(J8Utils.OET)) {
                Optional<InputStream> ato = artifactStore.attachmentContent(attachmentName);
                if (ato.isPresent()) {
                    Files.copy(ato.get(), attachment, StandardCopyOption.REPLACE_EXISTING);
                    logger.info(
                            "Written attachment {} from ArtifactStore {} to {}", attachmentName, store, attachmentFile);
                } else {
                    logger.warn("ArtifactStore {} has no attachment with name {}", store, attachmentName);
                }
            }
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
