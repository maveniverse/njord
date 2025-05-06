/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreComparator;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Lists available comparators.
 */
@Mojo(name = "list-comparators", threadSafe = true, requiresProject = false)
public class ListComparatorsMojo extends NjordMojoSupport {
    @Override
    protected void doExecute(Session ns) {
        logger.info("Listing available comparators:");
        for (ArtifactStoreComparator comparator : ns.availableComparators()) {
            logger.info("- '{}' - {}", comparator.name(), comparator.description());
        }
    }
}
