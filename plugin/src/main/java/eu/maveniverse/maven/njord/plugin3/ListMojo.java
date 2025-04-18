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
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * List all existing stores.
 */
@Mojo(name = "list", threadSafe = true, requiresProject = false)
public class ListMojo extends NjordMojoSupport {
    @Override
    protected void doExecute(NjordSession ns) throws IOException {
        logger.info("List of existing ArtifactStore:");
        Collection<String> storeNames = ns.artifactStoreManager().listArtifactStoreNames();
        for (String storeName : storeNames) {
            Optional<ArtifactStore> aso = ns.artifactStoreManager().selectArtifactStore(storeName);
            if (aso.isPresent()) {
                try (ArtifactStore store = aso.orElseThrow()) {
                    logger.info("- " + store);
                }
            }
        }
        logger.info("Total of {} ArtifactStore.", storeNames.size());
    }
}
