package eu.maveniverse.maven.njord.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.NjordSession;
import eu.maveniverse.maven.njord.shared.NjordSessionFactory;
import eu.maveniverse.maven.njord.shared.impl.factories.ArtifactStoreExporterFactory;
import eu.maveniverse.maven.njord.shared.impl.factories.ArtifactStoreMergerFactory;
import eu.maveniverse.maven.njord.shared.impl.factories.InternalArtifactStoreManagerFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;

@Singleton
@Named
public class DefaultNjordSessionFactory<C> implements NjordSessionFactory {
    private final InternalArtifactStoreManagerFactory internalArtifactStoreManagerFactory;
    private final ArtifactStoreExporterFactory artifactStoreExporterFactory;
    private final ArtifactStoreMergerFactory artifactStoreMergerFactory;
    private final Map<String, ArtifactStorePublisherFactory> artifactStorePublisherFactories;

    @Inject
    public DefaultNjordSessionFactory(
            InternalArtifactStoreManagerFactory internalArtifactStoreManagerFactory,
            ArtifactStoreExporterFactory artifactStoreExporterFactory,
            ArtifactStoreMergerFactory artifactStoreMergerFactory,
            Map<String, ArtifactStorePublisherFactory> artifactStorePublisherFactories) {
        this.internalArtifactStoreManagerFactory = requireNonNull(internalArtifactStoreManagerFactory);
        this.artifactStoreExporterFactory = requireNonNull(artifactStoreExporterFactory);
        this.artifactStoreMergerFactory = requireNonNull(artifactStoreMergerFactory);
        this.artifactStorePublisherFactories = requireNonNull(artifactStorePublisherFactories);
    }

    @Override
    public NjordSession create(RepositorySystemSession session, Config config) {
        return new DefaultNjordSession(
                session,
                config,
                internalArtifactStoreManagerFactory,
                artifactStoreExporterFactory,
                artifactStoreMergerFactory,
                artifactStorePublisherFactories);
    }
}
