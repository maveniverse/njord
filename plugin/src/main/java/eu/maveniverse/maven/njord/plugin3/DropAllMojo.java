package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.repository.ArtifactStore;
import eu.maveniverse.maven.njord.shared.repository.ArtifactStoreManager;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Tabula rasa: removes all stores. To make it work you need extra {@code -Dyes}.
 */
@Mojo(name = "drop-all", threadSafe = true, requiresProject = false)
public class DropAllMojo extends NjordMojoSupport {
    @Parameter(required = true, property = "yes")
    private boolean yes;

    @Override
    protected void doExecute(ArtifactStoreManager artifactStoreManager) throws IOException {
        if (yes) {
            logger.info("Dropping all ArtifactStore");
            AtomicInteger count = new AtomicInteger();
            for (String name : artifactStoreManager.listArtifactStoreNames()) {
                Optional<ArtifactStore> artifactStore = artifactStoreManager.selectArtifactStore(name);
                if (artifactStore.isPresent()) {
                    ArtifactStore store = artifactStore.orElseThrow();
                    logger.info("{}. dropping {}", count.incrementAndGet(), store);
                    artifactStoreManager.dropArtifactStore(store);
                }
            }
            logger.info("Dropped total of {} ArtifactStore", count.get());
        } else {
            logger.warn("Not dropping all: you must add extra `-Dyes` to agree on consequences");
        }
    }
}
