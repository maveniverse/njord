/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.NjordSession;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreValidator;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.util.Optional;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Validate given store against given target.
 */
@Mojo(name = "validate", threadSafe = true, requiresProject = false)
public class ValidateMojo extends PublisherSupportMojo {
    /**
     * Show detailed validation report.
     */
    @Parameter(required = true, property = "details", defaultValue = "false")
    private boolean details;

    @Override
    protected void doExecute(NjordSession ns) throws IOException, MojoFailureException {
        ArtifactStorePublisher p = getArtifactStorePublisher(ns);
        try (ArtifactStore from = getArtifactStore(ns)) {
            Optional<ArtifactStoreValidator> v = p.validatorFor(from);
            if (v.isPresent()) {
                ArtifactStoreValidator.ValidationResult vr = v.orElseThrow().validate(from);
                if (details) {
                    logger.info("Validation results for {}", from.name());
                    dumpValidationResult("", vr);
                }
                if (!vr.isValid()) {
                    logger.error("ArtifactStore {} failed validation", from);
                    throw new MojoFailureException("ArtifactStore validation failed");
                } else {
                    int warnings = vr.warningCount();
                    if (warnings > 0) {
                        logger.warn(
                                "ArtifactStore {} passed {} validation with {} warnings",
                                from,
                                vr.checkCount(),
                                warnings);
                    } else {
                        logger.info("ArtifactStore {} passed {} validation", from, vr.checkCount());
                    }
                }
            } else {
                logger.info("Not validated artifact store, no applicable validator set for publisher {}", p.name());
            }
        }
    }

    private void dumpValidationResult(String prefix, ArtifactStoreValidator.ValidationResult vr) {
        logger.info("{} {}", prefix, vr.name());
        if (!vr.error().isEmpty()) {
            for (String msg : vr.error()) {
                logger.error("{}    {}", prefix, msg);
            }
        }
        if (!vr.warning().isEmpty()) {
            for (String msg : vr.warning()) {
                logger.warn("{}    {}", prefix, msg);
            }
        }
        if (!vr.info().isEmpty()) {
            for (String msg : vr.info()) {
                logger.info("{}    {}", prefix, msg);
            }
        }
        for (ArtifactStoreValidator.ValidationResult child : vr.children()) {
            dumpValidationResult(prefix + "  ", child);
        }
    }
}
