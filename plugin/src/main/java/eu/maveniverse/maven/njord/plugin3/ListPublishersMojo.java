package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.NjordSession;
import java.util.Map;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Lists available publishers.
 */
@Mojo(name = "list-publishers", threadSafe = true, requiresProject = false)
public class ListPublishersMojo extends NjordMojoSupport {
    @Override
    protected void doExecute(NjordSession ns) {
        logger.info("Listing available publishers:");
        for (Map.Entry<String, String> publisher : ns.availablePublishers().entrySet()) {
            logger.info("- {}: {}", publisher.getKey(), publisher.getValue());
        }
    }
}
