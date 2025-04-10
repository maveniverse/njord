package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.repository.ArtifactStore;
import eu.maveniverse.maven.njord.shared.repository.ArtifactStoreManager;
import eu.maveniverse.maven.njord.shared.repository.ArtifactStorePublisher;
import java.io.IOException;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "publish", threadSafe = true, requiresProject = false)
public class PublishMojo extends NjordMojoSupport {
    @Parameter(required = true, property = "store")
    private String store;

    @Parameter(required = true, property = "target")
    private String target;

    @Parameter(required = true, property = "drop", defaultValue = "true")
    private boolean drop;

    @Override
    protected void doExecute(ArtifactStoreManager artifactStoreManager)
            throws IOException, MojoExecutionException, MojoFailureException {

        Optional<ArtifactStore> storeOptional = artifactStoreManager.selectArtifactStore(store);
        if (storeOptional.isEmpty()) {
            logger.warn("ArtifactStore with given name not found: {}", store);
            return;
        }

        ArtifactStorePublisher publisher = null;
        try (ArtifactStore from = storeOptional.orElseThrow()) {
            publisher.publish(from);
            if (drop) {
                artifactStoreManager.dropArtifactStore(from);
            }
        }
    }
}
