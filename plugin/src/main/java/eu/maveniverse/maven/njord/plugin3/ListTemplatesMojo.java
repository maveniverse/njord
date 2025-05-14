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
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.util.Collection;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * List all existing templates.
 */
@Mojo(name = "list-templates", threadSafe = true, requiresProject = false, aggregator = true)
public class ListTemplatesMojo extends NjordMojoSupport {
    @Override
    protected void doWithSession(Session ns) throws IOException {
        logger.info("List of existing ArtifactStoreTemplate:");
        Collection<ArtifactStoreTemplate> templates = ns.artifactStoreManager().listTemplates();
        ArtifactStoreTemplate defaultReleaseTemplate =
                ns.artifactStoreManager().defaultTemplate(RepositoryMode.RELEASE);
        ArtifactStoreTemplate defaultSnapshotTemplate =
                ns.artifactStoreManager().defaultTemplate(RepositoryMode.SNAPSHOT);
        for (ArtifactStoreTemplate template : templates) {
            printTemplate(template, template == defaultReleaseTemplate || template == defaultSnapshotTemplate);
        }
        logger.info("Total of {} ArtifactStoreTemplate.", templates.size());
    }
}
