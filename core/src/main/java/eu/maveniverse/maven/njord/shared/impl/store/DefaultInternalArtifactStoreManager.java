/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.store;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.InternalArtifactStoreManager;
import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import eu.maveniverse.maven.shared.core.component.CloseableConfigSupport;
import eu.maveniverse.maven.shared.core.fs.DirectoryLocker;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import eu.maveniverse.maven.shared.core.maven.MavenUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.eclipse.aether.util.ConfigUtils;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

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
    public List<String> listArtifactStoreNames() throws IOException {
        checkClosed();
        if (Files.isDirectory(config.basedir())) {
            try (Stream<Path> stream = Files.list(config.basedir())) {
                return stream.filter(Files::isDirectory)
                        .filter(p -> Files.isRegularFile(metaRepositoryProperties(p)))
                        .map(p -> p.getFileName().toString())
                        .sorted(Comparator.naturalOrder())
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> listArtifactStoreNamesForPrefix(String prefix) throws IOException {
        checkClosed();
        ArrayList<String> result = new ArrayList<>();
        for (String storeName : listArtifactStoreNames()) {
            if (storeName.startsWith(prefix)) { // simple check but more is needed
                try (ArtifactStore store = loadExistingArtifactStore(storeName)) {
                    if (store != null && Objects.equals(prefix, store.template().prefix())) {
                        result.add(storeName);
                    }
                } catch (IOException e) {
                    logger.warn("Error listing store for prefix {}: {}", storeName, e.getMessage());
                }
            }
        }
        return J8Utils.copyOf(result);
    }

    @Override
    public Optional<ArtifactStore> selectArtifactStore(String name) throws IOException {
        requireNonNull(name);
        checkClosed();

        return Optional.ofNullable(loadExistingArtifactStore(name));
    }

    @Override
    public ArtifactStoreTemplate defaultTemplate(RepositoryMode repositoryMode) {
        checkClosed();
        requireNonNull(repositoryMode);

        switch (repositoryMode) {
            case RELEASE:
                return ArtifactStoreTemplate.RELEASE_SCA;
            case SNAPSHOT:
                return ArtifactStoreTemplate.SNAPSHOT_SCA;
            default:
                throw new IllegalArgumentException("Unsupported repository mode: " + repositoryMode);
        }
    }

    @Override
    public Collection<ArtifactStoreTemplate> listTemplates() {
        checkClosed();

        return J8Utils.copyOf(templates.values());
    }

    @Override
    public ArtifactStore createArtifactStore(ArtifactStoreTemplate template, Artifact originProjectArtifact)
            throws IOException {
        requireNonNull(template);
        checkClosed();

        return createNewArtifactStore(template, originProjectArtifact);
    }

    @Override
    public boolean dropArtifactStore(String name) throws IOException {
        requireNonNull(name);
        checkClosed();

        Path basedir = config.basedir().resolve(name);
        if (Files.isDirectory(basedir)) {
            DirectoryLocker.INSTANCE.lockDirectory(basedir, true);
            try {
                Path meta = metaRepositoryProperties(basedir);
                if (Files.exists(meta)) {
                    if (!config.dryRun()) {
                        FileUtils.deleteRecursively(basedir);
                    } else {
                        logger.info("Dry run; not dropping store {}", name);
                    }
                    return true;
                }
            } finally {
                DirectoryLocker.INSTANCE.unlockDirectory(basedir);
            }
        }
        return false;
    }

    @Override
    public void renumberArtifactStores() throws IOException {
        ArrayList<String> names = new ArrayList<>(listArtifactStoreNames());
        names.sort(Comparator.naturalOrder());
        Map<ArtifactStoreTemplate, TreeSet<String>> stores = new HashMap<>();
        for (String name : names) {
            Path basedir = config.basedir().resolve(name);
            Map<String, String> properties = loadStoreProperties(basedir);
            ArtifactStoreTemplate template = loadTemplateWithProperties(properties);
            stores.computeIfAbsent(template, k -> new TreeSet<>()).add(name);
        }
        for (ArtifactStoreTemplate template : stores.keySet()) {
            int num = 1;
            for (String name : stores.get(template)) {
                String formattedName = formatArtifactStoreName(template.prefix(), num++);
                if (!formattedName.equals(name)) {
                    Path basedir = config.basedir().resolve(name);
                    renameStore(basedir, formattedName);
                }
            }
        }
    }

    @Override
    public Path exportTo(ArtifactStore artifactStore, Path file) throws IOException {
        requireNonNull(artifactStore);
        requireNonNull(file);
        checkClosed();

        if (!(artifactStore instanceof PathArtifactStore)) {
            throw new IllegalArgumentException("Unsupported store type: " + artifactStore.getClass());
        }

        Path targetDirectory = FileUtils.canonicalPath(file);
        Path bundleFile = targetDirectory;
        if (Files.isDirectory(targetDirectory)) {
            bundleFile = targetDirectory.resolve(artifactStore.name() + ".ntb");
        } else if (!Files.isDirectory(targetDirectory.getFileName())) {
            throw new IllegalArgumentException("Target parent directory does not exists");
        }
        if (Files.exists(bundleFile)) {
            throw new IOException("Exporting to existing bundle ZIP not supported");
        }
        try (FileSystem fs =
                FileSystems.newFileSystem(URI.create("jar:" + bundleFile.toUri()), J8Utils.zipFsCreate(true), null)) {
            Path root = fs.getPath("/");
            if (!Files.isDirectory(root)) {
                throw new IOException("Directory does not exist");
            }
            FileUtils.copyRecursively(
                    ((PathArtifactStore) artifactStore).basedir(),
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
        Path storeSource = FileUtils.canonicalPath(file);
        String storeName;
        Path storeBasedir;
        try (FileSystem fs =
                FileSystems.newFileSystem(URI.create("jar:" + storeSource.toUri()), J8Utils.zipFsCreate(false), null)) {
            Path repositoryProperties = metaRepositoryProperties(fs.getPath("/"));
            if (Files.exists(repositoryProperties)) {
                Map<String, String> properties = loadStoreProperties(fs.getPath("/"));
                ArtifactStoreTemplate template = loadTemplateWithProperties(properties);
                Artifact originProjectArtifact = properties.containsKey("originProjectArtifact")
                        ? new DefaultArtifact(properties.get("originProjectArtifact"))
                        : null;
                try (PathArtifactStore artifactStore = createNewArtifactStore(template, originProjectArtifact)) {
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
                renameStore(storeBasedir, storeName);
            } else {
                throw new IOException("Unknown transportable bundle layout");
            }
        }
        return loadExistingArtifactStore(storeName);
    }

    private ArtifactStoreTemplate loadTemplateWithProperties(Map<String, String> properties) {
        ArtifactStoreTemplate template = templates.get(properties.get("templateName"));
        if (template == null) {
            throw new IllegalStateException("Template not found: " + properties.get("templateName"));
        }
        if (properties.containsKey("templatePrefix")) {
            template = template.withPrefix(properties.get("templatePrefix"));
        }
        return template;
    }

    private PathArtifactStore loadExistingArtifactStore(String name) throws IOException {
        Path basedir = config.basedir().resolve(name);
        if (Files.isDirectory(basedir)) {
            DirectoryLocker.INSTANCE.lockDirectory(basedir, false);
            Map<String, String> properties = loadStoreProperties(basedir);
            return new PathArtifactStore(
                    properties.get("name"),
                    loadTemplateWithProperties(properties),
                    Instant.ofEpochMilli(Long.parseLong(properties.get("created"))),
                    RepositoryMode.valueOf(properties.get("repositoryMode")),
                    Boolean.parseBoolean(properties.get("allowRedeploy")),
                    checksumAlgorithmFactorySelector.selectList(Arrays.stream(
                                    properties.get("checksumAlgorithmFactories").split(","))
                            .filter(s -> !s.trim().isEmpty())
                            .collect(toList())),
                    Arrays.stream(properties.get("omitChecksumsForExtensions").split(","))
                            .filter(s -> !s.trim().isEmpty())
                            .collect(toList()),
                    properties.containsKey("originProjectArtifact")
                            ? new DefaultArtifact(properties.get("originProjectArtifact"))
                            : null,
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

    private PathArtifactStore createNewArtifactStore(ArtifactStoreTemplate template, Artifact originProjectArtifact)
            throws IOException {
        String name = newArtifactStoreName(template.prefix());
        Path basedir = config.basedir().resolve(name);
        Files.createDirectories(basedir);
        DirectoryLocker.INSTANCE.lockDirectory(basedir, true);
        Instant created = Instant.now();
        RepositoryMode repositoryMode = template.repositoryMode();
        boolean allowRedeploy = template.allowRedeploy();
        List<ChecksumAlgorithmFactory> checksumAlgorithmFactories = template.checksumAlgorithmFactories()
                        .isPresent()
                ? checksumAlgorithmFactorySelector.selectList(
                        template.checksumAlgorithmFactories().orElseThrow(J8Utils.OET))
                : checksumAlgorithmFactorySelector.selectList(
                        ConfigUtils.parseCommaSeparatedUniqueNames(ConfigUtils.getString(
                                config.session(), DEFAULT_CHECKSUMS_ALGORITHMS, CONFIG_PROP_CHECKSUMS_ALGORITHMS)));
        List<String> omitChecksumsForExtensions =
                template.omitChecksumsForExtensions().isPresent()
                        ? template.omitChecksumsForExtensions().orElseThrow(J8Utils.OET)
                        : ConfigUtils.parseCommaSeparatedUniqueNames(ConfigUtils.getString(
                                config.session(),
                                DEFAULT_OMIT_CHECKSUMS_FOR_EXTENSIONS,
                                CONFIG_PROP_OMIT_CHECKSUMS_FOR_EXTENSIONS));

        HashMap<String, String> properties = new HashMap<>();
        properties.put("name", name);
        properties.put("templateName", template.name());
        if (!Objects.equals(template.name(), template.prefix())) {
            properties.put("templatePrefix", template.prefix());
        }
        properties.put("created", Long.toString(created.toEpochMilli()));
        properties.put("repositoryMode", repositoryMode.name());
        properties.put("allowRedeploy", Boolean.toString(allowRedeploy));
        properties.put(
                "checksumAlgorithmFactories",
                checksumAlgorithmFactories.stream()
                        .map(ChecksumAlgorithmFactory::getName)
                        .collect(Collectors.joining(",")));
        properties.put("omitChecksumsForExtensions", String.join(",", omitChecksumsForExtensions));
        if (originProjectArtifact != null) {
            properties.put("originProjectArtifact", ArtifactIdUtils.toId(originProjectArtifact));
        }
        saveStoreProperties(basedir, properties);

        return new PathArtifactStore(
                name,
                template,
                created,
                repositoryMode,
                allowRedeploy,
                checksumAlgorithmFactories,
                omitChecksumsForExtensions,
                originProjectArtifact,
                basedir);
    }

    private String newArtifactStoreName(String prefix) throws IOException {
        String prefixDash = prefix + "-";
        int num = 0;
        try (Stream<Path> candidates = Files.list(config.basedir())
                .filter(Files::isDirectory)
                .filter(d -> d.getFileName().toString().startsWith(prefixDash))
                .filter(d -> Files.isRegularFile(metaRepositoryProperties(d)))
                .sorted(Comparator.reverseOrder())) {
            Optional<Path> greatest = candidates.findFirst();
            if (greatest.isPresent()) {
                num = Integer.parseInt(greatest.orElseThrow(J8Utils.OET)
                        .getFileName()
                        .toString()
                        .substring(prefixDash.length()));
            }
        }
        return formatArtifactStoreName(prefix, num + 1);
    }

    private String formatArtifactStoreName(String prefix, int num) {
        return String.format("%s-%05d", prefix, num);
    }

    private Path metaRepositoryProperties(Path basedir) {
        return basedir.resolve(".meta").resolve("repository.properties");
    }

    private Map<String, String> loadStoreProperties(Path basedir) throws IOException {
        Properties properties = new Properties();
        Path metaStoreProperties = metaRepositoryProperties(basedir);
        try (InputStream in = Files.newInputStream(metaStoreProperties)) {
            properties.load(in);
        }
        return MavenUtils.toMap(properties);
    }

    private void saveStoreProperties(Path basedir, Map<String, String> properties) throws IOException {
        Properties prop = new Properties();
        properties.forEach(prop::setProperty);
        Path metaStoreProperties = metaRepositoryProperties(basedir);
        Files.createDirectories(metaStoreProperties.getParent());
        try (OutputStream out = Files.newOutputStream(metaStoreProperties, StandardOpenOption.CREATE)) {
            prop.store(out, null);
        }
    }

    private void renameStore(Path basedir, String newName) throws IOException {
        Map<String, String> props = loadStoreProperties(basedir);
        props.put("name", newName);
        saveStoreProperties(basedir, props);
        if (!basedir.getFileName().toString().equals(newName)) {
            Files.move(basedir, basedir.getParent().resolve(newName), StandardCopyOption.ATOMIC_MOVE);
        }
    }
}
