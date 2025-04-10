package eu.maveniverse.maven.njord.publisher.sonatype;

import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;

public class SonatypeNx2Publisher implements ArtifactStorePublisher {
    @Override
    public void publish(ArtifactStore artifactStore) throws IOException {
        throw new RuntimeException("Not yet implemented");
    }
}
