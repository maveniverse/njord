package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.NjordSession;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreExporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Exports a store as "bundle" ZIP to given path. The ZIP file has remote repository layout and contains all the
 * artifacts and metadata.
 */
@Mojo(name = "export-bundle", threadSafe = true, requiresProject = false)
public class ExportBundleMojo extends NjordMojoSupport {
    @Parameter(required = true, property = "store")
    private String store;

    @Parameter(required = true, property = "directory")
    private String directory;

    @Override
    protected void doExecute(NjordSession ns) throws IOException, MojoExecutionException {
        Optional<ArtifactStore> storeOptional = ns.artifactStoreManager().selectArtifactStore(store);
        if (storeOptional.isPresent()) {
            Path targetDirectory = Config.getCanonicalPath(Path.of(directory).toAbsolutePath());
            if (!Files.isDirectory(targetDirectory)) {
                Files.createDirectories(targetDirectory);
            }
            try (ArtifactStoreExporter artifactStoreExporter = ns.createArtifactStoreExporter()) {
                artifactStoreExporter.exportAsBundle(storeOptional.orElseThrow(), targetDirectory);
            }
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
