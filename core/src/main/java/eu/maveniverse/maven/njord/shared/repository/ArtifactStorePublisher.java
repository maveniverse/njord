package eu.maveniverse.maven.njord.shared.repository;

import java.io.IOException;

public interface ArtifactStorePublisher {
    void publish(ArtifactStore artifactStore) throws IOException;
}
