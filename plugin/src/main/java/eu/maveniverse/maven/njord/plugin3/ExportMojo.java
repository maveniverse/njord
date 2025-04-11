package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.impl.FileUtils;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Exports a store as "remote Maven repository" directory structure to given directory.
 */
@Mojo(name = "export", threadSafe = true, requiresProject = false)
public class ExportMojo extends NjordMojoSupport {
    @Parameter(required = true, property = "store")
    private String store;

    @Parameter(required = true, property = "directory")
    private String directory;

    @Override
    protected void doExecute(ArtifactStoreManager artifactStoreManager) throws IOException, MojoExecutionException {
        Optional<ArtifactStore> storeOptional = artifactStoreManager.selectArtifactStore(store);
        if (storeOptional.isPresent()) {
            Path targetDirectory = Config.getCanonicalPath(Path.of(directory).toAbsolutePath());
            if (Files.exists(targetDirectory)) {
                throw new MojoExecutionException("Exporting to existing directory not supported");
            }
            try (ArtifactStore store = storeOptional.orElseThrow()) {
                logger.info("Exporting ArtifactStore {} to {}", store, targetDirectory);
                FileUtils.copyRecursively(store.basedir(), targetDirectory, p -> !p.getFileName()
                        .toString()
                        .startsWith("."));
            }
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
