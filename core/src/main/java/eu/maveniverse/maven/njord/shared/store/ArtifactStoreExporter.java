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
import java.nio.file.Path;

public interface ArtifactStoreExporter extends Closeable {
    /**
     * Exports store as directory hierarchy using Maven remote repository layout.
     */
    void exportAsDirectory(ArtifactStore artifactStore, Path outputDirectory) throws IOException;

    /**
     * Exports store as ZIP bundle.
     */
    void exportAsBundle(ArtifactStore artifactStore, Path outputDirectory) throws IOException;
}
