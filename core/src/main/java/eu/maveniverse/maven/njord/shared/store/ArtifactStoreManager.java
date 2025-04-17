/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.store;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

public interface ArtifactStoreManager {
    /**
     * Lists store "probable names". Not all element name may be a store, check with {@link #selectArtifactStore(String)}.
     */
    Collection<String> listArtifactStoreNames() throws IOException;

    /**
     * Selects artifact store. If selected (optional is not empty), caller must close it.
     */
    Optional<ArtifactStore> selectArtifactStore(String name) throws IOException;

    /**
     * Returns the default template.
     */
    ArtifactStoreTemplate defaultTemplate();

    /**
     * List templates.
     */
    Collection<ArtifactStoreTemplate> listTemplates();

    /**
     * Creates store based on template.
     */
    ArtifactStore createArtifactStore(ArtifactStoreTemplate template) throws IOException;

    /**
     * Fully deletes store.
     */
    boolean dropArtifactStore(String name) throws IOException;
}
