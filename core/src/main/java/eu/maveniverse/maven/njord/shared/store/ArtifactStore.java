/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.store;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

public interface ArtifactStore extends Closeable {
    /**
     * Store name, never {@code null}.
     */
    String name();

    /**
     * The template used to create this store.
     */
    ArtifactStoreTemplate template();

    /**
     * Timestamp when this store was created, never {@code null}.
     */
    Instant created();

    /**
     * Store repository mode, never {@code null}.
     */
    RepositoryMode repositoryMode();

    /**
     * Store write mode, never {@code null}.
     */
    WriteMode writeMode();

    /**
     * The checksum algorithm factories this store uses.
     */
    List<ChecksumAlgorithmFactory> checksumAlgorithmFactories();

    /**
     * The extensions to omit checksums for, like ".asc" (prefixed with dot).
     */
    List<String> omitChecksumsForExtensions();

    /**
     * The origin project artifact, that was used to create this store instance, if applicable.
     */
    Optional<Artifact> originProjectArtifact();

    /**
     * Index of artifacts in this store (except checksums), never {@code null}.
     */
    Collection<Artifact> artifacts();

    /**
     * Index of metadata in this store (except checksums), never {@code null}.
     */
    Collection<Metadata> metadata();

    /**
     * Returns {@code true} if exists.
     */
    boolean artifactPresent(Artifact artifact) throws IOException;

    /**
     * Returns {@code true} if exists.
     */
    boolean metadataPresent(Metadata metadata) throws IOException;

    /**
     * Returns the artifact content, if exists.
     */
    Optional<InputStream> artifactContent(Artifact artifact) throws IOException;

    /**
     * Returns the artifact content, if exists.
     */
    Optional<InputStream> metadataContent(Metadata metadata) throws IOException;

    /**
     * Customizes the session to access this store.
     *
     * @see #storeRemoteRepository()
     */
    RepositorySystemSession storeRepositorySession(RepositorySystemSession session);

    /**
     * The Resolver remote repository access to underlying store.
     *
     * @see #storeRepositorySession(RepositorySystemSession)
     */
    RemoteRepository storeRemoteRepository();

    /**
     * Writes out the whole store to given directory using store layout. The directory must exist.
     */
    void writeTo(Path directory) throws IOException;

    /**
     * Content modifying operation handle. Caller must close this instance, even if operation is canceled.
     */
    interface Operation extends Closeable {
        /**
         * Cancels the operation.
         */
        void cancel();
    }

    /**
     * Prepares artifact and metadata writes/puts and returns the handle. After write, caller must close the handle
     * to apply changes. If store is not allowing writes at all, this method throws {@link IOException}.
     */
    Operation put(Collection<Artifact> artifacts, Collection<Metadata> metadata) throws IOException;

    /**
     * @return {@code true} if store contains neither artifacts nor metadata, {@code false} otherwise.
     * @see #artifacts()
     * @see #metadata()
     * @since 0.8.7
     */
    boolean isEmpty();
}
