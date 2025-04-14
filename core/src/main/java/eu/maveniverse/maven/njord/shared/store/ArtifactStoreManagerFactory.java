package eu.maveniverse.maven.njord.shared.store;

import eu.maveniverse.maven.njord.shared.Config;

public interface ArtifactStoreManagerFactory {
    /**
     * Creates instance of artifact store manager.
     */
    ArtifactStoreManager create(Config config);
}
