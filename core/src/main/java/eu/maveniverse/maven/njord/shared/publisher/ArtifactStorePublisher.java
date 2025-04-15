package eu.maveniverse.maven.njord.shared.publisher;

import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.Closeable;
import java.io.IOException;

public interface ArtifactStorePublisher extends Closeable {
    /**
     * Performs the publishing.
     */
    void publish(ArtifactStore artifactStore) throws IOException;
}
