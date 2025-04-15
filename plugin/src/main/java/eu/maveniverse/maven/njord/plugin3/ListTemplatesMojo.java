package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.NjordSession;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import java.io.IOException;
import java.util.Collection;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * List all existing templates.
 */
@Mojo(name = "list-templates", threadSafe = true, requiresProject = false)
public class ListTemplatesMojo extends NjordMojoSupport {
    @Override
    protected void doExecute(NjordSession ns) throws IOException {
        logger.info("List of existing ArtifactStoreTemplate:");
        Collection<ArtifactStoreTemplate> templates = ns.artifactStoreManager().listTemplates();
        ArtifactStoreTemplate defaultTemplate = ns.artifactStoreManager().defaultTemplate();
        for (ArtifactStoreTemplate template : templates) {
            logger.info("- {} {}", template.name(), template == defaultTemplate ? " (default)" : " ");
            logger.info("    Prefix: {}", template.prefix());
            logger.info("    Repository Mode: {}", template.repositoryMode());
            logger.info("    Allow redeploy: {}", template.allowRedeploy());
            logger.info(
                    "    Checksum Factories: {}",
                    template.checksumAlgorithmFactories().isPresent()
                            ? template.checksumAlgorithmFactories().orElseThrow()
                            : "Globally configured");
            logger.info(
                    "    Omit checksums for: {}",
                    template.checksumAlgorithmFactories().isPresent()
                            ? template.checksumAlgorithmFactories().orElseThrow()
                            : "Globally configured");
        }
        logger.info("Total of {} ArtifactStoreTemplate.", templates.size());
    }
}
