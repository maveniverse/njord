package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.repository.ArtifactStoreManager;
import java.io.IOException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Exports a store to given path (ie for archival purposes or processing or whatever).
 */
@Mojo(name = "export", threadSafe = true, requiresProject = false)
public class ExportMojo extends NjordMojoSupport {
    @Parameter(required = true, property = "store")
    private String store;

    @Parameter(required = true, property = "path")
    private String path;

    @Override
    protected void doExecute(ArtifactStoreManager artifactStoreManager) throws IOException {
        throw new RuntimeException("Not implemented yet");
    }
}
