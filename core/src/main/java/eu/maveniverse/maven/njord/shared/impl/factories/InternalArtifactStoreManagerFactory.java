package eu.maveniverse.maven.njord.shared.impl.factories;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.impl.InternalArtifactStoreManager;

public interface InternalArtifactStoreManagerFactory {
    /**
     * Creates instance of artifact store manager.
     */
    InternalArtifactStoreManager create(Config config);
}
