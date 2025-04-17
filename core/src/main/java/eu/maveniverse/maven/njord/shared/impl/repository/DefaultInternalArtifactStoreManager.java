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

        return Optional.ofNullable(existingArtifactStore(name));
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

    // copied as while it is public in Resolver 2 is not in Resolver 1
    private static final String CONFIG_PROP_CHECKSUMS_ALGORITHMS = "aether.checksums.algorithms";
    private static final String DEFAULT_CHECKSUMS_ALGORITHMS = "SHA-1,MD5";

    private static final String CONFIG_PROP_OMIT_CHECKSUMS_FOR_EXTENSIONS =
            "aether.checksums.omitChecksumsForExtensions";
    private static final String DEFAULT_OMIT_CHECKSUMS_FOR_EXTENSIONS = ".asc,.sigstore";

    @Override
    public ArtifactStore createArtifactStore(ArtifactStoreTemplate template) throws IOException {
        requireNonNull(template);
        checkClosed();

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
        properties.put("name", name);
        properties.put("created", Long.toString(created.toEpochMilli()));
        properties.put("repositoryMode", repositoryMode.name());
        properties.put("allowRedeploy", Boolean.toString(allowRedeploy));
        properties.put(
                "checksumAlgorithmFactories",
                checksumAlgorithmFactories.stream()
                        .map(ChecksumAlgorithmFactory::getName)
                        .collect(Collectors.joining(",")));
        properties.put("omitChecksumsForExtensions", String.join(",", omitChecksumsForExtensions));
        Path meta = basedir.resolve(".meta").resolve("repository.properties");
        Files.createDirectories(meta.getParent());
        try (OutputStream out = Files.newOutputStream(meta, StandardOpenOption.CREATE_NEW)) {
            properties.store(out, null);
        }

        return new DefaultArtifactStore(
                name,
                created,
                repositoryMode,
                allowRedeploy,
                checksumAlgorithmFactories,
                omitChecksumsForExtensions,
                basedir);
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

    private DefaultArtifactStore existingArtifactStore(String name) throws IOException {
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
