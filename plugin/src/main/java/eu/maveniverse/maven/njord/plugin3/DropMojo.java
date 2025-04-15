package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.NjordSession;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
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
    protected void doExecute(NjordSession ns) throws IOException {
        Optional<ArtifactStore> storeOptional = ns.artifactStoreManager().selectArtifactStore(store);
        if (storeOptional.isPresent()) {
            ArtifactStore store = storeOptional.orElseThrow();
            logger.info("Dropping ArtifactStore {}", store);
            ns.artifactStoreManager().dropArtifactStore(store);
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
