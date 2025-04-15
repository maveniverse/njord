package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.NjordSession;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * List all existing stores.
 */
@Mojo(name = "list", threadSafe = true, requiresProject = false)
public class ListMojo extends NjordMojoSupport {
    @Override
    protected void doExecute(NjordSession ns) throws IOException {
        logger.info("List of existing ArtifactStore:");
        Collection<String> storeNames = ns.artifactStoreManager().listArtifactStoreNames();
        for (String storeName : storeNames) {
            Optional<ArtifactStore> aso = ns.artifactStoreManager().selectArtifactStore(storeName);
            if (aso.isPresent()) {
                try (ArtifactStore store = aso.orElseThrow()) {
                    logger.info("- " + store);
                }
            }
        }
        logger.info("Total of {} ArtifactStore.", storeNames.size());
    }
}
