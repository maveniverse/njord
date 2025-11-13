/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.publisher.ArtifactPublisherRedirector;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreComparator;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMerger;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreWriter;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

public interface Session extends Closeable {
    /**
     * Returns this session configuration.
     */
    SessionConfig config();

    /**
     * Returns store manager.
     */
    ArtifactStoreManager artifactStoreManager();

    /**
     * Returns store writer.
     */
    ArtifactStoreWriter artifactStoreWriter();

    /**
     * Returns store merger.
     */
    ArtifactStoreMerger artifactStoreMerger();

    /**
     * Artifact store publisher redirector.
     */
    ArtifactPublisherRedirector artifactPublisherRedirector();

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
     * Returns a collection of available comparator names.
     */
    Collection<ArtifactStoreComparator> availableComparators();

    /**
     * Selects the publisher by {@link ArtifactStoreComparator#name()}.
     */
    default Optional<ArtifactStoreComparator> selectArtifactStoreComparator(String name) {
        requireNonNull(name);
        return availableComparators().stream()
                .filter(p -> name.equals(p.name()))
                .findFirst();
    }

    /**
     * Reads the effective model of given artifact. The artifact does not have to be POM artifact. The repositories
     * must be given, and caller must ensure that provided list of repositories makes artifact model buildable,
     * like parents are resolvable.
     */
    Optional<Model> readEffectiveModel(Artifact artifact, List<RemoteRepository> remoteRepositories);

    /**
     * Selects template based on provided URL (see {@link #getOrCreateSessionArtifactStore(RemoteRepository, String)} method for syntax).
     * For existing stores it will return the template of the store.
     */
    ArtifactStoreTemplate selectSessionArtifactStoreTemplate(String uri);

    /**
     * Creates session-bound artifact store and memoize it during session.
     * {@code repoId::njord:}
     * {@code repoId::njord:template:templateName}
     * {@code repoId::njord:store:storeName}
     */
    ArtifactStore getOrCreateSessionArtifactStore(RemoteRepository repository, String uri);

    /**
     * Publishes all session-bound artifact stores created in this session. Session publishes all own created
     * stores. Hence, top level session publishes all created stores in given
     * Maven session. Returns the count of published stores.
     */
    int publishSessionArtifactStores() throws IOException;

    /**
     * Drops all session-bound artifact stores created in this session. Session drops all own created
     * stores. Hence, top level session drops all created stores in given
     * Maven session. Returns the count of dropped stores.
     * <p>
     * Cleans up "best effort" and reports failures as warnings.
     */
    int dropSessionArtifactStores();
}
