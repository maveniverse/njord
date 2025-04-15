package eu.maveniverse.maven.njord.shared.impl;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.NjordSession;
import eu.maveniverse.maven.njord.shared.impl.factories.ArtifactStoreExporterFactory;
import eu.maveniverse.maven.njord.shared.impl.factories.ArtifactStoreMergerFactory;
import eu.maveniverse.maven.njord.shared.impl.factories.InternalArtifactStoreManagerFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreExporter;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMerger;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.aether.RepositorySystemSession;

public class DefaultNjordSession extends CloseableConfigSupport<Config> implements NjordSession {
    private final RepositorySystemSession session;
    private final InternalArtifactStoreManager internalArtifactStoreManager;
    private final ArtifactStoreExporterFactory artifactStoreExporterFactory;
    private final ArtifactStoreMergerFactory artifactStoreMergerFactory;
    private final Map<String, ArtifactStorePublisherFactory> artifactStorePublisherFactories;

    public DefaultNjordSession(
            RepositorySystemSession session,
            Config config,
            InternalArtifactStoreManagerFactory internalArtifactStoreManagerFactory,
            ArtifactStoreExporterFactory artifactStoreExporterFactory,
            ArtifactStoreMergerFactory artifactStoreMergerFactory,
            Map<String, ArtifactStorePublisherFactory> artifactStorePublisherFactories) {
        super(config);
        this.session = requireNonNull(session);
        this.internalArtifactStoreManager = internalArtifactStoreManagerFactory.create(config);
        this.artifactStoreExporterFactory = requireNonNull(artifactStoreExporterFactory);
        this.artifactStoreMergerFactory = requireNonNull(artifactStoreMergerFactory);
        this.artifactStorePublisherFactories = requireNonNull(artifactStorePublisherFactories);
    }

    @Override
    public ArtifactStoreManager artifactStoreManager() {
        checkClosed();
        return internalArtifactStoreManager;
    }

    @Override
    public ArtifactStoreExporter createArtifactStoreExporter() {
        checkClosed();
        return artifactStoreExporterFactory.create(session, config);
    }

    @Override
    public ArtifactStoreMerger createArtifactStoreMerger() {
        checkClosed();
        return artifactStoreMergerFactory.create(session, config);
    }

    @Override
    public Map<String, String> availablePublishers() {
        checkClosed();
        return artifactStorePublisherFactories.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().description()))
                .collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    @Override
    public Optional<ArtifactStorePublisher> createArtifactStorePublisher(String target) {
        requireNonNull(target);
        checkClosed();

        ArtifactStorePublisherFactory publisherFactory = artifactStorePublisherFactories.get(target);
        if (publisherFactory == null) {
            return Optional.empty();
        } else {
            return Optional.of(publisherFactory.create(session, config));
        }
    }

    @Override
    protected void doClose() throws IOException {
        internalArtifactStoreManager.close();
    }
}
