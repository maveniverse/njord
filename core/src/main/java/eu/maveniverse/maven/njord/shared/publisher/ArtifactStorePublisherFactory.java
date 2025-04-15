package eu.maveniverse.maven.njord.shared.publisher;

import eu.maveniverse.maven.njord.shared.Config;
import org.eclipse.aether.RepositorySystemSession;

public interface ArtifactStorePublisherFactory {
    /**
     * Returns short description of publisher.
     */
    String description();

    /**
     * Creates publisher instance. Returned instance must be closed, ideally in try-with-resource.
     */
    ArtifactStorePublisher create(RepositorySystemSession session, Config config);
}
