package eu.maveniverse.maven.njord.shared.publisher;

import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;

public interface ArtifactStorePublisher {
    void publish(ArtifactStore artifactStore) throws IOException;
}
