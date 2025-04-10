package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import java.io.IOException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Publishes given store to given target.
 */
@Mojo(name = "publish", threadSafe = true, requiresProject = false)
public class PublishMojo extends NjordMojoSupport {
    @Parameter(required = true, property = "store")
    private String store;

    @Parameter(required = true, property = "target")
    private String target;

    @Parameter(required = true, property = "drop", defaultValue = "true")
    private boolean drop;

    @Override
    protected void doExecute(ArtifactStoreManager artifactStoreManager) throws IOException {
        throw new RuntimeException("Not implemented yet");
    }
}
