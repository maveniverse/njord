package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.NjordUtils;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Publishes given store to given target.
 */
@Mojo(name = "publish", threadSafe = true, requiresProject = false)
public class PublishMojo extends NjordMojoSupport {
    @Inject
    private Map<String, ArtifactStorePublisherFactory> publishers;

    @Parameter(required = true, property = "store")
    private String store;

    @Parameter(required = true, property = "target")
    private String target;

    @Parameter(required = true, property = "drop", defaultValue = "true")
    private boolean drop;

    @Override
    protected void doExecute(ArtifactStoreManager artifactStoreManager) throws IOException {
        Optional<ArtifactStore> storeOptional = artifactStoreManager.selectArtifactStore(store);
        if (storeOptional.isEmpty()) {
            logger.warn("ArtifactStore with given name not found: {}", store);
            return;
        }
        ArtifactStorePublisherFactory publisherFactory = publishers.get(target);
        if (publisherFactory == null) {
            logger.warn("Target {} not found", target);
            return;
        }
        try (ArtifactStore from = storeOptional.orElseThrow()) {
            publisherFactory
                    .create(
                            mavenSession.getRepositorySession(),
                            NjordUtils.mayGetConfig(mavenSession.getRepositorySession())
                                    .orElseThrow())
                    .publish(from);
            if (drop) {
                logger.info("Dropping {}", from);
                artifactStoreManager.dropArtifactStore(from);
            }
        }
    }
}
