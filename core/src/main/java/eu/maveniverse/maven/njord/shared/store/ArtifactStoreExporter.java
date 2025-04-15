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
