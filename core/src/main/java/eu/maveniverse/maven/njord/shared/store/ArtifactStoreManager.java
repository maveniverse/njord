/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.store;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;

public interface ArtifactStoreManager {
    /**
     * Lists store "probable names". Not all element name may be a store, check with {@link #selectArtifactStore(String)}.
     * The result is ordered in natural order (growing).
     */
    List<String> listArtifactStoreNames() throws IOException;

    /**
     * Lists store "probable names" for given prefix. Not all element name may be a store, check with {@link #selectArtifactStore(String)}.
     * The result is ordered in natural order (growing).
     */
    List<String> listArtifactStoreNamesForPrefix(String prefix) throws IOException;

    /**
     * Selects artifact store. If selected (optional is not empty), caller must close it.
     */
    Optional<ArtifactStore> selectArtifactStore(String name) throws IOException;

    /**
     * Returns the default template for given repository mode.
     */
    ArtifactStoreTemplate defaultTemplate(RepositoryMode repositoryMode);

    /**
     * List templates.
     */
    Collection<ArtifactStoreTemplate> listTemplates();

    /**
     * Creates store based on template. Optionally, the "origin project" may be given, or {@code null}.
     */
    ArtifactStore createArtifactStore(ArtifactStoreTemplate template, Artifact originProjectArtifact)
            throws IOException;

    /**
     * Fully deletes store.
     */
    boolean dropArtifactStore(String name) throws IOException;

    /**
     * Changes write mode of existing store and returns {@code true} on successful operation. Opened store cannot
     * be updated, as this operation requires exclusive lock on store.
     */
    boolean updateWriteModeArtifactStore(String name, WriteMode writeMode) throws IOException;

    /**
     * Renumbers artifact stores.
     */
    void renumberArtifactStores() throws IOException;

    /**
     * Exports store as "transportable" Njord bundle. The file may be existing directory, in which case name of the
     * resulting file will be store name, or non-existing file (with existing parents). Returns the bundle file.
     */
    Path exportTo(ArtifactStore artifactStore, Path file) throws IOException;

    /**
     * Imports the whole store to from "transportable" Njord bundle. The file must exist.
     */
    ArtifactStore importFrom(Path file) throws IOException;
}
