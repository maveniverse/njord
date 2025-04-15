package eu.maveniverse.maven.njord.shared.impl.repository;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.impl.factories.ArtifactStoreExporterFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreExporter;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;

@Singleton
@Named
public class DefaultArtifactStoreExporterFactory implements ArtifactStoreExporterFactory {
    @Override
    public ArtifactStoreExporter create(RepositorySystemSession session, Config config) {
        return new DefaultArtifactStoreExporter(config);
    }
}
