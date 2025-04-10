package eu.maveniverse.maven.njord.shared.impl.repository;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.impl.FileUtils;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultArtifactStoreManager implements ArtifactStoreManager {
    private final Config config;
    private final AtomicBoolean closed;
    private final Map<String, ArtifactStoreTemplate> templates;

    public DefaultArtifactStoreManager(Config config) {
        this.config = requireNonNull(config);
        this.closed = new AtomicBoolean(false);
        this.templates = new HashMap<>();
        templates.put(ArtifactStoreTemplate.RELEASE.name(), ArtifactStoreTemplate.RELEASE);
        templates.put(ArtifactStoreTemplate.RELEASE_REDEPLOY.name(), ArtifactStoreTemplate.RELEASE_REDEPLOY);
        templates.put(ArtifactStoreTemplate.SNAPSHOT.name(), ArtifactStoreTemplate.SNAPSHOT);

        templates.put("default", ArtifactStoreTemplate.RELEASE);
    }

    @Override
    public Collection<String> listArtifactStoreNames() throws IOException {
        checkClosed();
        if (Files.isDirectory(config.basedir())) {
            try (Stream<Path> stream = Files.list(config.basedir())) {
                return stream.filter(Files::isDirectory)
                        .filter(p -> Files.isRegularFile(p.resolve(".meta").resolve("repository.properties")))
                        .map(p -> p.getFileName().toString())
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    @Override
    public Optional<ArtifactStore> selectArtifactStore(String name) throws IOException {
        requireNonNull(name);
        checkClosed();
        Path artifactStoreBasedir = config.basedir().resolve(name);
        if (Files.isDirectory(artifactStoreBasedir)) {
            return Optional.of(new DefaultArtifactStore(artifactStoreBasedir));
        }
        return Optional.empty();
    }

    @Override
    public ArtifactStore createArtifactStore(String templateName) throws IOException {
        requireNonNull(templateName);
        checkClosed();
        ArtifactStoreTemplate template = templates.get(templateName);
        if (template == null) {
            throw new IllegalArgumentException("Unknown template " + templateName);
        }
        return createArtifactStore(template);
    }

    @Override
    public ArtifactStore createArtifactStore(ArtifactStoreTemplate template) throws IOException {
        requireNonNull(template);
        checkClosed();
        String name = template.prefix() + "-" + UUID.randomUUID();
        return new DefaultArtifactStore(
                name,
                template.repositoryMode(),
                template.allowRedeploy(),
                config.basedir().resolve(name));
    }

    @Override
    public void dropArtifactStore(ArtifactStore artifactStore) throws IOException {
        requireNonNull(artifactStore);
        checkClosed();
        Path storeDir = artifactStore.basedir();
        artifactStore.close();
        FileUtils.deleteRecursively(storeDir);
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            // nothing yet
        }
    }

    private void checkClosed() {
        if (closed.get()) {
            throw new IllegalStateException("ArtifactStoreManager is closed");
        }
    }
}
