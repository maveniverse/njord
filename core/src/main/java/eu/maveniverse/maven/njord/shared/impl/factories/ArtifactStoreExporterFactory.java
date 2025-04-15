package eu.maveniverse.maven.njord.shared.impl.factories;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreExporter;
import org.eclipse.aether.RepositorySystemSession;

public interface ArtifactStoreExporterFactory {
    /**
     * Creates exporter instance.
     */
    ArtifactStoreExporter create(RepositorySystemSession session, Config config);
}
