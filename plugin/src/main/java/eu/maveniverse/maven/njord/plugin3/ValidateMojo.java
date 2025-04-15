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
public class ValidateMojo extends NjordMojoSupport {
    @Parameter(required = true, property = "store")
    private String store;

    @Parameter(required = true, property = "target")
    private String target;

    @Parameter(required = true, property = "details", defaultValue = "false")
    private boolean details;

    @Override
    protected void doExecute(NjordSession ns) throws IOException, MojoFailureException {
        Optional<ArtifactStore> storeOptional = ns.artifactStoreManager().selectArtifactStore(store);
        if (storeOptional.isEmpty()) {
            logger.warn("ArtifactStore with given name not found: {}", store);
            return;
        }
        Optional<ArtifactStorePublisher> po = ns.availablePublishers().stream()
                .filter(p -> target.equals(p.name()))
                .findFirst();
        if (po.isPresent()) {
            try (ArtifactStore from = storeOptional.orElseThrow()) {
                ArtifactStorePublisher p = po.orElseThrow();
                Optional<ArtifactStoreValidator.ValidationResult> vro = p.validate(from);
                if (vro.isPresent()) {
                    ArtifactStoreValidator.ValidationResult vr = vro.orElseThrow();
                    if (details) {
                        logger.info("Validation results for {}", store);
                        dumpValidationResult("", vr);
                    }
                    if (!vr.isValid()) {
                        logger.error("ArtifactStore {} failed validation", from);
                        throw new MojoFailureException("ArtifactStore validation failed");
                    } else {
                        logger.info("ArtifactStore {} passed validation", from);
                    }
                } else {
                    logger.info("Not validated artifact store, no validator set for publisher {}", p.name());
                }
            }
        } else {
            throw new MojoFailureException("Publisher not found");
        }
    }

    private void dumpValidationResult(String prefix, ArtifactStoreValidator.ValidationResult vr) {
        logger.info("{} {}", prefix, vr.name());
        if (!vr.error().isEmpty()) {
            logger.error("{}  Errors:", prefix);
            for (String msg : vr.error()) {
                logger.error("{}    {}", prefix, msg);
            }
        }
        if (!vr.warning().isEmpty()) {
            logger.warn("{}  Errors:", prefix);
            for (String msg : vr.warning()) {
                logger.warn("{}    {}", prefix, msg);
            }
        }
        if (!vr.info().isEmpty()) {
            logger.info("{}  Info:", prefix);
            for (String msg : vr.info()) {
                logger.info("{}    {}", prefix, msg);
            }
        }
        for (ArtifactStoreValidator.ValidationResult child : vr.children()) {
            dumpValidationResult(prefix + "  ", child);
        }
    }
}
