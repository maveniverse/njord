/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public interface ArtifactStoreWriter {
    /**
     * Exports store as directory hierarchy using Maven remote repository layout. Returns the root directory.
     */
    Path writeAsDirectory(ArtifactStore artifactStore, Path outputDirectory) throws IOException;

    /**
     * Writes the store as a ZIP bundle named {@code <store name>.zip} into the given directory. Returns the ZIP file.
     */
    Path writeAsBundle(ArtifactStore artifactStore, Path outputDirectory) throws IOException;

    /**
     * Like {@link #writeAsBundle(ArtifactStore, Path)}, but lets you choose the exact output file. When
     * {@code outputMayBeFile} is {@code true} and {@code output} is not an existing directory, the bundle is written
     * to that file (creating parent directories if needed); otherwise {@code output} is used as the target directory.
     * Returns the ZIP file.
     * <p>
     * The default implementation only handles the directory case; implementations that can write to a specific file
     * override it.
     */
    default Path writeAsBundle(ArtifactStore artifactStore, Path output, boolean outputMayBeFile) throws IOException {
        if (!outputMayBeFile || Files.isDirectory(output)) {
            return writeAsBundle(artifactStore, output);
        }
        throw new UnsupportedOperationException(
                "Writing a bundle to an explicit file is not supported by this ArtifactStoreWriter implementation");
    }
}
