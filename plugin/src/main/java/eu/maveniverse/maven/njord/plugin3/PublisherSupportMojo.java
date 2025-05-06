/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Publisher support mojo.
 */
public abstract class PublisherSupportMojo extends NjordMojoSupport {
    /**
     * The name of the store to publish.
     */
    @Parameter(property = "store")
    protected String store;

    /**
     * The name of the publisher to publish to.
     */
    @Parameter(property = "publisher")
    protected String publisher;

    protected ArtifactStore getArtifactStore(Session ns) throws IOException, MojoFailureException {
        if (store == null && ns.config().prefix().isPresent()) {
            String prefix = ns.config().prefix().orElseThrow();
            logger.info("No store name specified but prefix is set to '{}'; using heuristic to find store", prefix);
            List<String> storeNames = ns.artifactStoreManager().listArtifactStoreNamesForPrefix(prefix);
            if (!storeNames.isEmpty()) {
                if (storeNames.size() == 1) {
                    store = storeNames.get(0);
                    logger.info("Found one store, using it: '{}'", store);
                } else {
                    store = storeNames.get(storeNames.size() - 1);
                    logger.info("Found multiple stores, using latest: '{}'", store);
                }
            }
        }
        if (store == null) {
            throw new MojoFailureException("ArtifactStore name was not specified nor could be found");
        }

        Optional<ArtifactStore> storeOptional = ns.artifactStoreManager().selectArtifactStore(store);
        if (storeOptional.isEmpty()) {
            logger.warn("ArtifactStore with given name not found: {}", store);
            throw new MojoFailureException("ArtifactStore with given name not found: " + store);
        }
        return storeOptional.orElseThrow();
    }

    protected ArtifactStorePublisher getArtifactStorePublisher(Session ns) throws MojoFailureException {
        if (publisher == null && ns.config().publisher().isPresent()) {
            publisher = ns.config().publisher().orElseThrow();
            logger.info("No publisher specified but session publisher is set to '{}'; using it", publisher);
        }
        if (publisher == null) {
            throw new MojoFailureException("Publisher name was not specified nor could be found");
        }

        Optional<ArtifactStorePublisher> po = ns.selectArtifactStorePublisher(publisher);
        if (po.isEmpty()) {
            throw new MojoFailureException("Publisher not found: " + publisher);
        }
        return po.orElseThrow();
    }
}
