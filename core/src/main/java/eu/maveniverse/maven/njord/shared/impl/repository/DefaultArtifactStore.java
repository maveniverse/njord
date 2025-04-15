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

import eu.maveniverse.maven.njord.shared.impl.CloseableSupport;
import eu.maveniverse.maven.njord.shared.impl.DirectoryLocker;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

public class DefaultArtifactStore extends CloseableSupport implements ArtifactStore {
    private final String name;
    private final Instant created;
    private final RepositoryMode repositoryMode;
    private final boolean allowRedeploy;
    private final List<ChecksumAlgorithmFactory> checksumAlgorithmFactories;
    private final List<String> omitChecksumsForExtensions;
    private final Path basedir;

    /**
     * Creates instance out for existing store.
     */
    public DefaultArtifactStore(Path basedir, ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector)
            throws IOException {
        DirectoryLocker.INSTANCE.lockDirectory(basedir, false);
        Properties properties = new Properties();
        Path meta = basedir.resolve(".meta").resolve("repository.properties");
        try (InputStream in = Files.newInputStream(meta)) {
            properties.load(in);
        }

        this.name = requireNonNull(properties.getProperty("name"), "Name not provided");
        this.created = Instant.ofEpochMilli(
                Long.parseLong(requireNonNull(properties.getProperty("created"), "Created not provided")));
        this.repositoryMode = requireNonNull(
                RepositoryMode.valueOf(properties.getProperty("repositoryMode")), "RepositoryMode not provided");
        this.allowRedeploy = Boolean.parseBoolean(properties.getProperty("allowRedeploy"));
        this.checksumAlgorithmFactories = checksumAlgorithmFactorySelector.selectList(Arrays.stream(
                        properties.getProperty("checksumAlgorithmFactories").split(","))
                .filter(s -> !s.trim().isEmpty())
                .collect(toList()));
        this.omitChecksumsForExtensions = Arrays.stream(
                        properties.getProperty("omitChecksumsForExtensions").split(","))
                .filter(s -> !s.trim().isEmpty())
                .collect(toList());
        this.basedir = requireNonNull(basedir);
    }

    /**
     * Creates and inits brand-new store.
     */
    public DefaultArtifactStore(
            String name,
            RepositoryMode repositoryMode,
            boolean allowRedeploy,
            List<ChecksumAlgorithmFactory> checksumAlgorithmFactories,
            List<String> omitChecksumsForExtensions,
            Path basedir)
            throws IOException {
        Files.createDirectories(basedir); // if brand new, we must create it first
        DirectoryLocker.INSTANCE.lockDirectory(basedir, true);
        try {
            this.name = requireNonNull(name);
            this.created = Instant.now();
            this.repositoryMode = requireNonNull(repositoryMode);
            this.allowRedeploy = allowRedeploy;
            this.checksumAlgorithmFactories = requireNonNull(checksumAlgorithmFactories);
            this.omitChecksumsForExtensions = requireNonNull(omitChecksumsForExtensions);
            this.basedir = requireNonNull(basedir);

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
        } finally {
            DirectoryLocker.INSTANCE.unlockDirectory(basedir);
            DirectoryLocker.INSTANCE.lockDirectory(basedir, false);
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Instant created() {
        return created;
    }

    @Override
    public RepositoryMode repositoryMode() {
        return repositoryMode;
    }

    @Override
    public boolean allowRedeploy() {
        return allowRedeploy;
    }

    @Override
    public List<ChecksumAlgorithmFactory> checksumAlgorithmFactories() {
        return checksumAlgorithmFactories;
    }

    @Override
    public List<String> omitChecksumsForExtensions() {
        return omitChecksumsForExtensions;
    }

    @Override
    public Collection<Artifact> artifacts() {
        checkClosed();
        return readIndex("artifacts", l -> {
            String[] split = l.split("=");
            return new DefaultArtifact(split[0])
                    .setFile(basedir.resolve(split[1]).toFile());
        });
    }

    @Override
    public Collection<Metadata> metadata() {
        checkClosed();
        return readIndex("metadata", l -> {
            String[] split = l.split("=");
            String[] coord = split[0].split(":");
            return new DefaultMetadata(
                            coord[0],
                            coord[1],
                            coord[2],
                            coord[3],
                            repositoryMode() == RepositoryMode.RELEASE
                                    ? Metadata.Nature.RELEASE
                                    : Metadata.Nature.SNAPSHOT)
                    .setFile(basedir.resolve(split[1]).toFile());
        });
    }

    @Override
    public Optional<InputStream> artifactContent(Artifact artifact) throws IOException {
        requireNonNull(artifact);
        Path file = basedir.resolve(artifactPath(artifact));
        if (Files.isRegularFile(file)) {
            return Optional.of(Files.newInputStream(file));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<InputStream> metadataContent(Metadata metadata) throws IOException {
        requireNonNull(metadata);
        Path file = basedir.resolve(metadataPath(metadata));
        if (Files.isRegularFile(file)) {
            return Optional.of(Files.newInputStream(file));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Path basedir() {
        checkClosed();
        return basedir;
    }

    @Override
    public RepositorySystemSession storeRepositorySession(RepositorySystemSession session) {
        checkClosed();
        requireNonNull(session);
        DefaultRepositorySystemSession session2 = new DefaultRepositorySystemSession(session);

        // checksums
        String caf = checksumAlgorithmFactories().stream()
                .map(ChecksumAlgorithmFactory::getName)
                .collect(Collectors.joining(","));

        // resolver 1 and 2
        session2.setConfigProperty("aether.checksums.algorithms", caf);
        session2.setConfigProperty("aether.layout.maven2.checksumAlgorithms", caf);

        session2.setConfigProperty(
                "aether.checksums.omitChecksumsForExtensions", String.join(",", omitChecksumsForExtensions()));
        return session2;
    }

    @Override
    public RemoteRepository storeRemoteRepository() {
        return new RemoteRepository.Builder(name(), "default", "file://" + basedir()).build();
    }

    @Override
    public Operation put(Collection<Artifact> artifacts, Collection<Metadata> metadata) throws IOException {
        requireNonNull(artifacts);
        requireNonNull(metadata);
        checkClosed();

        DirectoryLocker.INSTANCE.unlockDirectory(basedir);
        DirectoryLocker.INSTANCE.lockDirectory(basedir, true);
        // check files (set + exists)
        if (artifacts.stream().anyMatch(a -> a.getFile() == null || !a.getFile().isFile())) {
            throw new IllegalArgumentException("Passed in artifacts must have set existing file");
        }
        if (metadata.stream().anyMatch(m -> m.getFile() == null || !m.getFile().isFile())) {
            throw new IllegalArgumentException("Passed in metadata must have set existing file");
        }
        // check RepositoryMode (snapshot vs release)
        boolean expected = repositoryMode() != RepositoryMode.RELEASE;
        if (artifacts.stream().anyMatch(a -> a.isSnapshot() != expected)) {
            throw new IllegalArgumentException("Passed in artifacts repository policy mismatch");
        }
        // check DeployMode (already exists)
        if (!allowRedeploy()
                && artifacts.stream().anyMatch(a -> Files.isRegularFile(basedir.resolve(artifactPath(a))))) {
            throw new IllegalArgumentException("Redeployment is forbidden (artifact already exists)");
        }

        return new Operation() {
            private final AtomicBoolean canceled = new AtomicBoolean(false);

            @Override
            public void cancel() {
                canceled.set(true);
            }

            @Override
            public void close() throws IOException {
                try {
                    if (!canceled.get()) {
                        appendIndex("artifacts", artifacts, a -> ArtifactIdUtils.toId(a) + "=" + artifactPath(a));
                        appendIndex(
                                "metadata",
                                metadata,
                                m -> String.format(
                                                "%s:%s:%s:%s",
                                                m.getGroupId(), m.getArtifactId(), m.getVersion(), m.getType())
                                        + "=" + metadataPath(m));
                    }
                } finally {
                    DirectoryLocker.INSTANCE.unlockDirectory(basedir);
                    DirectoryLocker.INSTANCE.lockDirectory(basedir, false);
                }
            }
        };
    }

    @Override
    protected void doClose() throws IOException {
        DirectoryLocker.INSTANCE.unlockDirectory(basedir);
    }

    @Override
    public String toString() {
        if (closed.get()) {
            return String.format(
                    "%s (%s, %s, closed)", name(), created(), repositoryMode().name());
        } else {
            return String.format(
                    "%s (%s, %s, %s artifacts)",
                    name(), created(), repositoryMode().name(), artifacts().size());
        }
    }

    private String artifactPath(Artifact artifact) {
        StringBuilder path = new StringBuilder(128);
        path.append(artifact.getGroupId().replace('.', '/')).append('/');
        path.append(artifact.getArtifactId()).append('/');
        path.append(artifact.getBaseVersion()).append('/');
        path.append(artifact.getArtifactId()).append('-').append(artifact.getVersion());
        if (!artifact.getClassifier().isEmpty()) {
            path.append('-').append(artifact.getClassifier());
        }
        if (!artifact.getExtension().isEmpty()) {
            path.append('.').append(artifact.getExtension());
        }
        return path.toString();
    }

    private String metadataPath(Metadata metadata) {
        StringBuilder path = new StringBuilder(128);
        if (!metadata.getGroupId().isEmpty()) {
            path.append(metadata.getGroupId().replace('.', '/')).append('/');
            if (!metadata.getArtifactId().isEmpty()) {
                path.append(metadata.getArtifactId()).append('/');
                if (!metadata.getVersion().isEmpty()) {
                    path.append(metadata.getVersion()).append('/');
                }
            }
        }
        path.append(metadata.getType());
        return path.toString();
    }

    private <E> Collection<E> readIndex(String what, Function<String, E> transform) {
        Path index = basedir.resolve(".meta").resolve(what);
        if (Files.isRegularFile(index)) {
            try (Stream<String> lines = Files.readAllLines(index, StandardCharsets.UTF_8).stream()) {
                return lines.map(transform).collect(Collectors.toSet());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            return Collections.emptySet();
        }
    }

    private <E> void appendIndex(String what, Collection<E> entries, Function<E, String> transform) throws IOException {
        if (!entries.isEmpty()) {
            Path index = basedir.resolve(".meta").resolve(what);
            if (!Files.isRegularFile(index)) {
                Files.createDirectories(index.getParent());
                Files.createFile(index);
            }
            List<String> lines = entries.stream().map(transform).toList();
            Files.write(index, lines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        }
    }
}
