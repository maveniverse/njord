package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.impl.FileUtils;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
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
    protected void doExecute(ArtifactStoreManager artifactStoreManager) throws IOException, MojoExecutionException {
        Optional<ArtifactStore> storeOptional = artifactStoreManager.selectArtifactStore(store);
        if (storeOptional.isPresent()) {
            Path targetDirectory = Config.getCanonicalPath(Path.of(directory).toAbsolutePath());
            if (!Files.isDirectory(targetDirectory)) {
                Files.createDirectories(targetDirectory);
            }
            Path bundleFile = targetDirectory.resolve(store + ".zip");
            if (Files.exists(bundleFile)) {
                throw new MojoExecutionException("Exporting to existing bundle not supported");
            }
            try (FileSystem fs = FileSystems.newFileSystem(bundleFile, Map.of("create", "true"), null)) {
                Path root = fs.getPath("/");
                try (ArtifactStore store = storeOptional.orElseThrow()) {
                    logger.info("Exporting ArtifactStore {} to {}", store, bundleFile);
                    FileUtils.copyRecursively(store.basedir(), root, p -> !p.getFileName()
                            .toString()
                            .startsWith("."));
                }
            }
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }
}
