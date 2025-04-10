package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

/**
 * Lists content of given store.
 */
@Mojo(name = "list-content", threadSafe = true, requiresProject = false)
public class ListContentMojo extends NjordMojoSupport {
    @Parameter(required = true, property = "store")
    private String store;

    @Override
    protected void doExecute(ArtifactStoreManager artifactStoreManager) throws IOException {
        Optional<ArtifactStore> storeOptional = artifactStoreManager.selectArtifactStore(store);
        if (storeOptional.isPresent()) {
            try (ArtifactStore store = storeOptional.orElseThrow()) {
                logger.info("List contents of ArtifactStore {}", store);
                AtomicInteger counter = new AtomicInteger();
                for (Artifact artifact : store.artifacts()) {
                    logger.info("{}. {}", counter.incrementAndGet(), ArtifactIdUtils.toId(artifact));
                }
            }
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
