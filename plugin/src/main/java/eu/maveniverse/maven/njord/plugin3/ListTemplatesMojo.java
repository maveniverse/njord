/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import java.io.IOException;
import java.util.Collection;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * List all existing templates.
 */
@Mojo(name = "list-templates", threadSafe = true, requiresProject = false)
public class ListTemplatesMojo extends NjordMojoSupport {
    @Override
    protected void doExecute(Session ns) throws IOException {
        logger.info("List of existing ArtifactStoreTemplate:");
        Collection<ArtifactStoreTemplate> templates = ns.artifactStoreManager().listTemplates();
        ArtifactStoreTemplate defaultTemplate = ns.artifactStoreManager().defaultTemplate();
        for (ArtifactStoreTemplate template : templates) {
            logger.info("- {} {}", template.name(), template == defaultTemplate ? " (default)" : " ");
            logger.info("    Default prefix: '{}'", template.prefix());
            logger.info("    Allow redeploy: {}", template.allowRedeploy());
            logger.info(
                    "    Checksum Factories: {}",
                    template.checksumAlgorithmFactories().isPresent()
                            ? template.checksumAlgorithmFactories().orElseThrow()
                            : "Globally configured");
            logger.info(
                    "    Omit checksums for: {}",
                    template.checksumAlgorithmFactories().isPresent()
                            ? template.checksumAlgorithmFactories().orElseThrow()
                            : "Globally configured");
        }
        logger.info("Total of {} ArtifactStoreTemplate.", templates.size());
    }
}
