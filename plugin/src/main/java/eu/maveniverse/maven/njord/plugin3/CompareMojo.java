/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreComparator;
import java.io.IOException;
import java.util.Optional;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Compares two given stores against each other.
 */
@Mojo(name = "compare", threadSafe = true, requiresProject = false)
public class CompareMojo extends NjordMojoSupport {
    /**
     * The name of the first store to compare.
     */
    @Parameter(required = true, property = "store1")
    private String store1;

    /**
     * The name of the second store to compare.
     */
    @Parameter(required = true, property = "store2")
    private String store2;

    /**
     * The name of the comparator to use to compare.
     */
    @Parameter(required = true, property = "comparator", defaultValue = "bitwise")
    private String comparator;

    /**
     * Show detailed validation report.
     */
    @Parameter(required = true, property = "details", defaultValue = "false")
    private boolean details;

    @Override
    protected void doExecute(Session ns) throws IOException, MojoFailureException {
        Optional<ArtifactStore> store1Optional = ns.artifactStoreManager().selectArtifactStore(store1);
        if (store1Optional.isEmpty()) {
            logger.warn("ArtifactStore with given name not found: {}", store1);
            return;
        }
        Optional<ArtifactStore> store2Optional = ns.artifactStoreManager().selectArtifactStore(store2);
        if (store2Optional.isEmpty()) {
            logger.warn("ArtifactStore with given name not found: {}", store2);
            return;
        }
        Optional<ArtifactStoreComparator> po = ns.selectArtifactStoreComparator(comparator);
        if (po.isPresent()) {
            ArtifactStoreComparator p = po.orElseThrow();
            try (ArtifactStore one = store1Optional.orElseThrow();
                    ArtifactStore two = store2Optional.orElseThrow()) {
                ArtifactStoreComparator.ComparisonResult vr = p.compare(one, two);
                if (details) {
                    logger.info("Comparison results for {} vs {}", store1, store2);
                    dumpComparisonResult("", vr);
                }
                if (!vr.isEqual()) {
                    logger.error("ArtifactStore {} and {} are DIFFERENT", store1, store2);
                    throw new MojoFailureException("ArtifactStore comparison failed");
                } else {
                    logger.info("ArtifactStore {} and {} are EQUAL", store1, store2);
                }
            }
        } else {
            throw new MojoFailureException("Comparator not found");
        }
    }

    private void dumpComparisonResult(String prefix, ArtifactStoreComparator.ComparisonResult vr) {
        logger.info("{} {}", prefix, vr.name());
        if (!vr.differences().isEmpty()) {
            for (String msg : vr.differences()) {
                logger.warn("{}    {}", prefix, msg);
            }
        }
        if (!vr.equalities().isEmpty()) {
            for (String msg : vr.equalities()) {
                logger.info("{}    {}", prefix, msg);
            }
        }
        for (ArtifactStoreComparator.ComparisonResult child : vr.children()) {
            dumpComparisonResult(prefix + "  ", child);
        }
    }
}
