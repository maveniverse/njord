/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.repository;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.impl.CloseableConfigSupport;
import eu.maveniverse.maven.njord.shared.impl.FileUtils;
import eu.maveniverse.maven.njord.shared.impl.InternalArtifactStoreManager;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.eclipse.aether.util.ConfigUtils;

public class DefaultInternalArtifactStoreManager extends CloseableConfigSupport<Config>
        implements InternalArtifactStoreManager {
    private final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;
    private final Map<String, ArtifactStoreTemplate> templates;

    public DefaultInternalArtifactStoreManager(
            Config config, ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        super(config);
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
}
