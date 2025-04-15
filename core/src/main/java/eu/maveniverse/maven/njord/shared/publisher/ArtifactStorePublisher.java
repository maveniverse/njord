package eu.maveniverse.maven.njord.shared.publisher;

import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.aether.repository.RemoteRepository;

public interface ArtifactStorePublisher {
    /**
     * Publisher name.
     */
    String name();

    /**
     * Returns short description of publisher.
     */
    String description();

    /**
     * The remote repository where release artifacts will become available after publishing succeeded.
     */
    Optional<RemoteRepository> targetReleaseRepository();

    /**
     * The remote repository where snapshot artifacts will become available after publishing succeeded.
     */
    Optional<RemoteRepository> targetSnapshotRepository();

    /**
     * The remote repository where release artifacts will be published.
     */
    Optional<RemoteRepository> serviceReleaseRepository();

    /**
     * The remote repository where snapshot artifacts will be published.
     */
    Optional<RemoteRepository> serviceSnapshotRepository();

    /**
     * The validator that must be applied to store before publishing.
     */
    Optional<ArtifactStoreValidator> validator();

    /**
     * Performs the publishing.
     */
    void publish(ArtifactStore artifactStore) throws IOException;
}
