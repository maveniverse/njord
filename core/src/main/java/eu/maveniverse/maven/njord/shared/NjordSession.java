/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreExporter;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMerger;
import java.io.Closeable;
import java.util.Collection;
import java.util.Optional;

public interface NjordSession extends Closeable {
    /**
     * Returns the session configuration.
     */
    SessionConfig sessionConfig();

    /**
     * Returns store manager.
     */
    ArtifactStoreManager artifactStoreManager();

    /**
     * Creates store exporter. Returned instance must be closed, ideally in try-with-resource.
     */
    ArtifactStoreExporter createArtifactStoreExporter();

    /**
     * Creates store merger. Returned instance must be closed, ideally in try-with-resource.
     */
    ArtifactStoreMerger createArtifactStoreMerger();

    /**
     * Returns a collection of available publisher names.
     */
    Collection<ArtifactStorePublisher> availablePublishers();

    /**
     * Selects the publisher by {@link ArtifactStorePublisher#name()}.
     */
    default Optional<ArtifactStorePublisher> selectArtifactStorePublisher(String name) {
        requireNonNull(name);
        return availablePublishers().stream().filter(p -> name.equals(p.name())).findFirst();
    }

    /**
     * Creates session-bound artifact store and memoize it during session.
     */
    ArtifactStore getOrCreateSessionArtifactStore(String uri);

    /**
     * Drops all session-bound artifact stores.
     */
    boolean dropSessionArtifactStores();
}
