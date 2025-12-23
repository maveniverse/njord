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
import java.util.Collection;
import java.util.Optional;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Lists attachments of the given store.
 */
@Mojo(name = "attachment-list", threadSafe = true, requiresProject = false, aggregator = true)
public class AttachmentListMojo extends NjordMojoSupport {
    /**
     * The name of the store to list attachments from. Mandatory parameter.
     */
    @Parameter(required = true, property = SessionConfig.KEY_PREFIX + "store")
    private String store;

    @Override
    protected void doWithSession(Session ns) throws MojoFailureException, IOException {
        Optional<ArtifactStore> aso = ns.artifactStoreManager().selectArtifactStore(store);
        if (aso.isPresent()) {
            try (ArtifactStore artifactStore = aso.orElseThrow(J8Utils.OET)) {
                Collection<String> attachments = artifactStore.attachments();
                if (attachments.isEmpty()) {
                    logger.info("Store {} has no attachments", store);
                } else {
                    logger.info("Store {} has {} attachments:", store, attachments.size());
                    for (String attachment : attachments) {
                        logger.info(" * {}", attachment);
                    }
                }
            }
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
