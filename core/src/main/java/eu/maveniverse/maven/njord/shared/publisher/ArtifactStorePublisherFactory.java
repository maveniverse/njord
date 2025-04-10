package eu.maveniverse.maven.njord.shared.publisher;

import eu.maveniverse.maven.njord.shared.Config;
import java.io.IOException;
import org.eclipse.aether.RepositorySystemSession;

public interface ArtifactStorePublisherFactory {
    ArtifactStorePublisher create(RepositorySystemSession session, Config config) throws IOException;
}
