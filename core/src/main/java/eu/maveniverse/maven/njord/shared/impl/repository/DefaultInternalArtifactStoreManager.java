/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.repository;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.CloseableConfigSupport;
import eu.maveniverse.maven.njord.shared.impl.DirectoryLocker;
import eu.maveniverse.maven.njord.shared.impl.FileUtils;
import eu.maveniverse.maven.njord.shared.impl.InternalArtifactStoreManager;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.eclipse.aether.util.ConfigUtils;

public class DefaultInternalArtifactStoreManager extends CloseableConfigSupport<SessionConfig>
        implements InternalArtifactStoreManager {
    private final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;
    private final Map<String, ArtifactStoreTemplate> templates;

    public DefaultInternalArtifactStoreManager(
            SessionConfig sessionConfig, ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        super(sessionConfig);
        this.checksumAlgorithmFactorySelector = requireNonNull(checksumAlgorithmFactorySelector);
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
        if (Files.isDirectory(config.config().basedir())) {
            try (Stream<Path> stream = Files.list(config.config().basedir())) {
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

        return Optional.ofNullable(loadExistingArtifactStore(name));
    }

    @Override
    public ArtifactStoreTemplate defaultTemplate() {
        checkClosed();

        return ArtifactStoreTemplate.RELEASE_SCA;
    }

    @Override
    public Collection<ArtifactStoreTemplate> listTemplates() {
        checkClosed();

        return List.copyOf(templates.values());
    }

    @Override
    public ArtifactStore createArtifactStore(ArtifactStoreTemplate template) throws IOException {
        requireNonNull(template);
        checkClosed();

        return createNewArtifactStore(template);
    }

    @Override
    public boolean dropArtifactStore(String name) throws IOException {
        requireNonNull(name);
        checkClosed();

        Path basedir = config.config().basedir().resolve(name);
        if (Files.isDirectory(basedir)) {
            DirectoryLocker.INSTANCE.lockDirectory(basedir, true);
            try {
                Path meta = basedir.resolve(".meta").resolve("repository.properties");
                if (Files.exists(meta)) {
                    FileUtils.deleteRecursively(basedir);
                    return true;
                }
            } finally {
                DirectoryLocker.INSTANCE.unlockDirectory(basedir);
            }
        }
        return false;
    }

    @Override
    public Path exportTo(ArtifactStore artifactStore, Path file) throws IOException {
        requireNonNull(artifactStore);
        requireNonNull(file);
        checkClosed();

        if (!(artifactStore instanceof DefaultArtifactStore)) {
            throw new IllegalArgumentException("Unsupported store type: " + artifactStore.getClass());
        }

        Path targetDirectory = Config.getCanonicalPath(file);
        Path bundleFile = targetDirectory;
        if (Files.isDirectory(targetDirectory)) {
            bundleFile = targetDirectory.resolve(artifactStore.name() + ".ntb");
        } else if (!Files.isDirectory(targetDirectory.getFileName())) {
            throw new IllegalArgumentException("Target parent directory does not exists");
        }
        if (Files.exists(bundleFile)) {
            throw new IOException("Exporting to existing bundle ZIP not supported");
        }
        try (FileSystem fs = FileSystems.newFileSystem(bundleFile, Map.of("create", "true"), null)) {
            Path root = fs.getPath("/");
            if (!Files.isDirectory(root)) {
                throw new IOException("Directory does not exist");
            }
            FileUtils.copyRecursively(
                    ((DefaultArtifactStore) artifactStore).basedir(),
                    root,
                    p -> p.getFileName() == null || !p.getFileName().toString().startsWith(".lock"),
                    false);
        }
        return bundleFile;
    }

    @Override
    public ArtifactStore importFrom(Path file) throws IOException {
        requireNonNull(file);
        checkClosed();

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("File does not exist");
        }
        Path storeSource = Config.getCanonicalPath(file);
        String storeName;
        Path storeBasedir;
        try (FileSystem fs = FileSystems.newFileSystem(storeSource, Map.of("create", "false"), null)) {
            Path repositoryProperties = fs.getPath("/", ".meta", "repository.properties");
            if (Files.exists(repositoryProperties)) {
                Properties properties = new Properties();
                try (InputStream in = Files.newInputStream(repositoryProperties)) {
                    properties.load(in);
                }
                ArtifactStoreTemplate template = templates.get(properties.getProperty("templateName"));
                if (template == null) {
                    throw new IOException("Template not found: " + properties.getProperty("templateName"));
                }
                try (DefaultArtifactStore artifactStore = createNewArtifactStore(template)) {
                    storeName = artifactStore.name();
                    storeBasedir = artifactStore.basedir();
                    FileUtils.copyRecursively(
                            fs.getPath("/"),
                            artifactStore.basedir(),
                            p -> p.getFileName() == null
                                    || !p.getFileName().toString().startsWith(".lock"),
                            true);
                }
                // fix name
                properties.clear();
                Path newStoreProperties = storeBasedir.resolve(".meta").resolve("repository.properties");
                try (InputStream in = Files.newInputStream(newStoreProperties)) {
                    properties.load(in);
                }
                properties.setProperty("name", storeName);
                try (OutputStream out =
                        Files.newOutputStream(newStoreProperties, StandardOpenOption.TRUNCATE_EXISTING)) {
                    properties.store(out, null);
                }
            } else {
                throw new IOException("Unknown transportable bundle layout");
            }
        }
        return loadExistingArtifactStore(storeName);
    }

    private DefaultArtifactStore loadExistingArtifactStore(String name) throws IOException {
        Path basedir = config.config().basedir().resolve(name);
        if (Files.isDirectory(basedir)) {
            DirectoryLocker.INSTANCE.lockDirectory(basedir, false);
            Properties properties = new Properties();
            Path meta = basedir.resolve(".meta").resolve("repository.properties");
            try (InputStream in = Files.newInputStream(meta)) {
                properties.load(in);
            }

            return new DefaultArtifactStore(
                    properties.getProperty("name"),
                    templates.get(properties.getProperty("templateName")),
                    Instant.ofEpochMilli(Long.parseLong(properties.getProperty("created"))),
                    RepositoryMode.valueOf(properties.getProperty("repositoryMode")),
                    Boolean.parseBoolean(properties.getProperty("allowRedeploy")),
                    checksumAlgorithmFactorySelector.selectList(Arrays.stream(properties
                                    .getProperty("checksumAlgorithmFactories")
                                    .split(","))
                            .filter(s -> !s.trim().isEmpty())
                            .collect(toList())),
                    Arrays.stream(properties
                                    .getProperty("omitChecksumsForExtensions")
                                    .split(","))
                            .filter(s -> !s.trim().isEmpty())
                            .collect(toList()),
                    basedir);
        }
        return null;
    }

    // copied as while it is public in Resolver 2 is not in Resolver 1
    private static final String CONFIG_PROP_CHECKSUMS_ALGORITHMS = "aether.checksums.algorithms";
    private static final String DEFAULT_CHECKSUMS_ALGORITHMS = "SHA-1,MD5";

    private static final String CONFIG_PROP_OMIT_CHECKSUMS_FOR_EXTENSIONS =
            "aether.checksums.omitChecksumsForExtensions";
    private static final String DEFAULT_OMIT_CHECKSUMS_FOR_EXTENSIONS = ".asc,.sigstore";

    private DefaultArtifactStore createNewArtifactStore(ArtifactStoreTemplate template) throws IOException {
        String name = newArtifactStoreName(template.prefix());
        Path basedir = config.config().basedir().resolve(name);
        Files.createDirectories(basedir);
        DirectoryLocker.INSTANCE.lockDirectory(basedir, true);
        Instant created = Instant.now();
        RepositoryMode repositoryMode = template.repositoryMode();
        boolean allowRedeploy = template.allowRedeploy();
        List<ChecksumAlgorithmFactory> checksumAlgorithmFactories = template.checksumAlgorithmFactories()
                        .isPresent()
                ? checksumAlgorithmFactorySelector.selectList(
                        template.checksumAlgorithmFactories().orElseThrow())
                : checksumAlgorithmFactorySelector.selectList(
                        ConfigUtils.parseCommaSeparatedUniqueNames(ConfigUtils.getString(
                                config.session(), DEFAULT_CHECKSUMS_ALGORITHMS, CONFIG_PROP_CHECKSUMS_ALGORITHMS)));
        List<String> omitChecksumsForExtensions =
                template.omitChecksumsForExtensions().isPresent()
                        ? template.omitChecksumsForExtensions().orElseThrow()
                        : ConfigUtils.parseCommaSeparatedUniqueNames(ConfigUtils.getString(
                                config.session(),
                                DEFAULT_OMIT_CHECKSUMS_FOR_EXTENSIONS,
                                CONFIG_PROP_OMIT_CHECKSUMS_FOR_EXTENSIONS));

        Properties properties = new Properties();
        properties.setProperty("name", name);
        properties.setProperty("templateName", template.name());
        properties.setProperty("created", Long.toString(created.toEpochMilli()));
        properties.setProperty("repositoryMode", repositoryMode.name());
        properties.setProperty("allowRedeploy", Boolean.toString(allowRedeploy));
        properties.setProperty(
                "checksumAlgorithmFactories",
                checksumAlgorithmFactories.stream()
                        .map(ChecksumAlgorithmFactory::getName)
                        .collect(Collectors.joining(",")));
        properties.setProperty("omitChecksumsForExtensions", String.join(",", omitChecksumsForExtensions));
        Path meta = basedir.resolve(".meta").resolve("repository.properties");
        Files.createDirectories(meta.getParent());
        try (OutputStream out = Files.newOutputStream(meta, StandardOpenOption.CREATE_NEW)) {
            properties.store(out, null);
        }

        return new DefaultArtifactStore(
                name,
                template,
                created,
                repositoryMode,
                allowRedeploy,
                checksumAlgorithmFactories,
                omitChecksumsForExtensions,
                basedir);
    }

    private String newArtifactStoreName(String prefix) throws IOException {
        String prefixDash = prefix + "-";
        int num = 0;
        try (Stream<Path> candidates = Files.list(config.config().basedir())
                .filter(Files::isDirectory)
                .filter(d -> d.getFileName().toString().startsWith(prefixDash))
                .filter(d -> Files.isRegularFile(d.resolve(".meta").resolve("repository.properties")))
                .sorted(Comparator.reverseOrder())) {
            Optional<Path> greatest = candidates.findFirst();
            if (greatest.isPresent()) {
                num = Integer.parseInt(
                        greatest.orElseThrow().getFileName().toString().substring(prefixDash.length()));
            }
        }
        return prefixDash + String.format("%05d", num + 1);
    }
}
