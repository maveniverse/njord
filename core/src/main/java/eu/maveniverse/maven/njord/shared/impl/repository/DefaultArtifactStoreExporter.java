/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.repository;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.impl.CloseableConfigSupport;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreExporter;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class DefaultArtifactStoreExporter extends CloseableConfigSupport<Config> implements ArtifactStoreExporter {
    public DefaultArtifactStoreExporter(Config config) {
        super(config);
    }

    @Override
    public Path exportAsDirectory(ArtifactStore artifactStore, Path directory) throws IOException {
        requireNonNull(artifactStore);
        requireNonNull(directory);
        checkClosed();

        Path targetDirectory = Config.getCanonicalPath(directory);
        if (Files.exists(targetDirectory)) {
            throw new IOException("Exporting to existing directory not supported");
        }
        artifactStore.writeTo(targetDirectory);
        return targetDirectory;
    }

    @Override
    public Path exportAsBundle(ArtifactStore artifactStore, Path directory) throws IOException {
        requireNonNull(artifactStore);
        requireNonNull(directory);
        checkClosed();

        Path targetDirectory = Config.getCanonicalPath(directory);
        if (!Files.isDirectory(targetDirectory)) {
            Files.createDirectories(targetDirectory);
        }
        Path bundleFile = targetDirectory.resolve(artifactStore.name() + ".zip");
        if (Files.exists(bundleFile)) {
            throw new IOException("Exporting to existing bundle ZIP not supported");
        }
        try (FileSystem fs = FileSystems.newFileSystem(bundleFile, Map.of("create", "true"), null)) {
            Path root = fs.getPath("/");
            artifactStore.writeTo(root);
        }
        return bundleFile;
    }
}
