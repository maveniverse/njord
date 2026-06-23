/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.store;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreWriter;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultArtifactStoreWriter extends ComponentSupport implements ArtifactStoreWriter {
    @Override
    public Path writeAsDirectory(ArtifactStore artifactStore, Path outputDirectory) throws IOException {
        requireNonNull(artifactStore);
        requireNonNull(outputDirectory);

        Path targetDirectory = FileUtils.canonicalPath(outputDirectory);
        if (Files.exists(targetDirectory)) {
            throw new IOException("Exporting to existing directory not supported");
        }
        Files.createDirectories(targetDirectory);
        artifactStore.writeTo(targetDirectory);
        return targetDirectory;
    }

    @Override
    public Path writeAsBundle(ArtifactStore artifactStore, Path outputDirectory) throws IOException {
        return writeAsBundle(artifactStore, outputDirectory, false);
    }

    @Override
    public Path writeAsBundle(ArtifactStore artifactStore, Path output, boolean outputMayBeFile) throws IOException {
        requireNonNull(artifactStore);
        requireNonNull(output);

        Path target = FileUtils.canonicalPath(output);
        Path bundleFile;
        if (!outputMayBeFile || Files.isDirectory(target)) {
            // directory semantics: write <target>/<store name>.zip
            if (!Files.isDirectory(target)) {
                Files.createDirectories(target);
            }
            bundleFile = target.resolve(artifactStore.name() + ".zip");
        } else {
            // file semantics: write to the exact file, creating missing parent directories
            bundleFile = target;
            Path parent = bundleFile.getParent();
            if (parent != null && !Files.isDirectory(parent)) {
                Files.createDirectories(parent);
            }
        }
        if (Files.exists(bundleFile)) {
            throw new IOException("Exporting to existing bundle ZIP not supported");
        }
        try (FileSystem fs =
                FileSystems.newFileSystem(URI.create("jar:" + bundleFile.toUri()), J8Utils.zipFsCreate(true), null)) {
            Path root = fs.getPath("/");
            artifactStore.writeTo(root);
        }
        return bundleFile;
    }
}
