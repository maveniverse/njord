package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.impl.repository.DeployingArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.repository.ArtifactStore;
import eu.maveniverse.maven.njord.shared.repository.ArtifactStoreManager;
import java.io.IOException;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.repository.RemoteRepository;

@Mojo(name = "merge", threadSafe = true, requiresProject = false)
public class MergeMojo extends NjordMojoSupport {
    @Parameter(required = true, property = "from")
    private String from;

    @Parameter(required = true, property = "to")
    private String to;

    @Parameter(required = true, property = "drop", defaultValue = "true")
    private boolean drop;

    @Override
    protected void doExecute(ArtifactStoreManager artifactStoreManager)
            throws IOException, MojoExecutionException, MojoFailureException {

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

        toOptional.orElseThrow().close();
        try (ArtifactStore from = fromOptional.orElseThrow()) {
            new DeployingArtifactStorePublisher(
                            repositorySystem,
                            mavenSession.getRepositorySession(),
                            new RemoteRepository.Builder(to, "default", "njord:repository:" + to).build())
                    .publish(from);
            if (drop) {
                artifactStoreManager.dropArtifactStore(from);
            }
        }
    }
}
