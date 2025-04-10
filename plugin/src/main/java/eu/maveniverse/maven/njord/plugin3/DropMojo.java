package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.repository.ArtifactStore;
import eu.maveniverse.maven.njord.shared.repository.ArtifactStoreManager;
import java.io.IOException;
import java.util.Optional;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Drops given store.
 */
@Mojo(name = "drop", threadSafe = true, requiresProject = false)
public class DropMojo extends NjordMojoSupport {
    @Parameter(required = true, property = "store")
    private String store;

    @Override
    protected void doExecute(ArtifactStoreManager artifactStoreManager) throws IOException {
        Optional<ArtifactStore> storeOptional = artifactStoreManager.selectArtifactStore(store);
        if (storeOptional.isPresent()) {
            artifactStoreManager.dropArtifactStore(storeOptional.orElseThrow());
            logger.info("Dropped ArtifactStore {}", store);
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
