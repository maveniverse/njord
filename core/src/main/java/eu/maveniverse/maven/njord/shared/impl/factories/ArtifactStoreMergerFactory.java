package eu.maveniverse.maven.njord.shared.impl.factories;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMerger;
import org.eclipse.aether.RepositorySystemSession;

public interface ArtifactStoreMergerFactory {
    /**
     * Creates merger instance.
     */
    ArtifactStoreMerger create(RepositorySystemSession session, Config config);
}
