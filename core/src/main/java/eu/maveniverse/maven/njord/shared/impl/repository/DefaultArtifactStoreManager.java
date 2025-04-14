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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.eclipse.aether.util.ConfigUtils;

public class DefaultArtifactStoreManager implements ArtifactStoreManager {
    private final Config config;
    private final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;

    private final AtomicBoolean closed;
    private final Map<String, ArtifactStoreTemplate> templates;

    public DefaultArtifactStoreManager(
            Config config, ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        this.config = requireNonNull(config);
        this.checksumAlgorithmFactorySelector = requireNonNull(checksumAlgorithmFactorySelector);

        this.closed = new AtomicBoolean(false);
        this.templates = new LinkedHashMap<>();
        templates.put(ArtifactStoreTemplate.RELEASE.name(), ArtifactStoreTemplate.RELEASE);
        templates.put(ArtifactStoreTemplate.RELEASE_SCA.name(), ArtifactStoreTemplate.RELEASE_SCA);
        templates.put(ArtifactStoreTemplate.RELEASE_REDEPLOY.name(), ArtifactStoreTemplate.RELEASE_REDEPLOY);
        templates.put(ArtifactStoreTemplate.RELEASE_REDEPLOY_SCA.name(), ArtifactStoreTemplate.RELEASE_REDEPLOY_SCA);
        templates.put(ArtifactStoreTemplate.SNAPSHOT.name(), ArtifactStoreTemplate.SNAPSHOT);
        templates.put(ArtifactStoreTemplate.SNAPSHOT_SCA.name(), ArtifactStoreTemplate.SNAPSHOT_SCA);
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
            return Optional.of(new DefaultArtifactStore(artifactStoreBasedir, checksumAlgorithmFactorySelector));
        }
        return Optional.empty();
    }

    @Override
    public ArtifactStoreTemplate defaultTemplate() {
        return ArtifactStoreTemplate.RELEASE_SCA;
    }

    @Override
    public Collection<ArtifactStoreTemplate> listTemplates() {
        return List.copyOf(templates.values());
    }

    public static final String CONFIG_PROP_CHECKSUMS_ALGORITHMS = "aether.checksums.algorithms";
    private static final String DEFAULT_CHECKSUMS_ALGORITHMS = "SHA-1,MD5";

    public static final String CONFIG_PROP_OMIT_CHECKSUMS_FOR_EXTENSIONS =
            "aether.checksums.omitChecksumsForExtensions";
    private static final String DEFAULT_OMIT_CHECKSUMS_FOR_EXTENSIONS = ".asc,.sigstore";

    @Override
    public ArtifactStore createArtifactStore(RepositorySystemSession session, ArtifactStoreTemplate template)
            throws IOException {
        requireNonNull(template);
        checkClosed();
        String name = template.prefix() + "-" + UUID.randomUUID();
        return new DefaultArtifactStore(
                name,
                template.repositoryMode(),
                template.allowRedeploy(),
                template.checksumAlgorithmFactories().isPresent()
                        ? checksumAlgorithmFactorySelector.selectList(
                                template.checksumAlgorithmFactories().orElseThrow())
                        : checksumAlgorithmFactorySelector.selectList(
                                ConfigUtils.parseCommaSeparatedUniqueNames(ConfigUtils.getString(
                                        session, DEFAULT_CHECKSUMS_ALGORITHMS, CONFIG_PROP_CHECKSUMS_ALGORITHMS))),
                template.omitChecksumsForExtensions().isPresent()
                        ? template.omitChecksumsForExtensions().orElseThrow()
                        : ConfigUtils.parseCommaSeparatedUniqueNames(ConfigUtils.getString(
                                session,
                                DEFAULT_OMIT_CHECKSUMS_FOR_EXTENSIONS,
                                CONFIG_PROP_OMIT_CHECKSUMS_FOR_EXTENSIONS)),
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
