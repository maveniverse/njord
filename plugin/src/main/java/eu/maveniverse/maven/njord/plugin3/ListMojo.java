package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.repository.ArtifactStore;
import eu.maveniverse.maven.njord.shared.repository.ArtifactStoreManager;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "list", threadSafe = true, requiresProject = false)
public class ListMojo extends NjordMojoSupport {
    @Override
    protected void doExecute(ArtifactStoreManager artifactStoreManager)
            throws IOException, MojoExecutionException, MojoFailureException {
        logger.info("List of existing ArtifactStore:");
        Collection<String> storeNames = artifactStoreManager.listArtifactStoreNames();
        for (String storeName : storeNames) {
            Optional<ArtifactStore> aso = artifactStoreManager.selectArtifactStore(storeName);
            if (aso.isPresent()) {
                try (ArtifactStore store = aso.orElseThrow()) {
                    logger.info("- " + store);
                }
            }
        }
        logger.info("Total of {} ArtifactStore.", storeNames.size());
    }
}
