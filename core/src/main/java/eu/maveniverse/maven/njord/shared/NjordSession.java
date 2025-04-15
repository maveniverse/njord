package eu.maveniverse.maven.njord.shared;

import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreExporter;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMerger;
import java.io.Closeable;
import java.util.Collection;

public interface NjordSession extends Closeable {
    /**
     * Returns the effective configuration.
     */
    Config config();

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
     * Returns a collection of available (configured) publishers.
     */
    Collection<ArtifactStorePublisher> availablePublishers();
}
