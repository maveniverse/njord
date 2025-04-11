package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.impl.repository.ArtifactStoreDeployer;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import java.io.IOException;
import java.util.Optional;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Merges {@code from} store onto {@code to} store, eventually dropping {@code from} store.
 */
@Mojo(name = "merge", threadSafe = true, requiresProject = false)
public class MergeMojo extends NjordMojoSupport {
    @Parameter(required = true, property = "from")
    private String from;

    @Parameter(required = true, property = "to")
    private String to;

    @Parameter(required = true, property = "drop", defaultValue = "true")
    private boolean drop;

    @Override
    protected void doExecute(ArtifactStoreManager artifactStoreManager) throws IOException {
        Optional<ArtifactStore> fromOptional = artifactStoreManager.selectArtifactStore(from);
        Optional<ArtifactStore> toOptional = artifactStoreManager.selectArtifactStore(to);
        if (fromOptional.isEmpty()) {
            logger.warn("ArtifactStore with given name not found: {}", from);
            return;
        }
        if (toOptional.isEmpty()) {
            logger.warn("ArtifactStore with given name not found: {}", to);
            return;
        }

        logger.info("Merging {} -> {}", fromOptional.orElseThrow(), toOptional.orElseThrow());
        toOptional.orElseThrow().close();
        try (ArtifactStore from = fromOptional.orElseThrow()) {
            new ArtifactStoreDeployer(
                            repositorySystem,
                            mavenSession.getRepositorySession(),
                            new RemoteRepository.Builder(to, "default", "njord:store:" + to).build())
                    .deploy(from);
            if (drop) {
                logger.info("Dropping {}", from);
                artifactStoreManager.dropArtifactStore(from);
            }
        }
    }
}
