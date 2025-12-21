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
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Publishes given store to given target.
 */
@Mojo(name = "publish", threadSafe = true, requiresProject = false, aggregator = true)
public class PublishMojo extends PublisherSupportMojo {
    /**
     * Whether source store should be dropped after successful operation. Defaults to {@code false}.
     */
    @Parameter(required = true, property = SessionConfig.KEY_PREFIX + "drop", defaultValue = "false")
    private boolean drop;

    @Override
    protected void doWithSession(Session ns) throws IOException, MojoFailureException {
        try (ArtifactStore from = getArtifactStore(ns)) {
            ArtifactStorePublisher publisher = getArtifactStorePublisher(ns);
            logger.info("Publishing {} with {}", from, publisher.name());
            try {
                publisher.publish(from);
            } catch (ArtifactStorePublisher.PublishFailedException e) {
                throw new MojoFailureException(e.getMessage(), e);
            }
        }
        if (drop) {
            logger.info("Dropping {}", store);
            ns.artifactStoreManager().dropArtifactStore(store);
        }
    }
}
