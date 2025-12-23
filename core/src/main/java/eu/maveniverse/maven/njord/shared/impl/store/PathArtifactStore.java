/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.store;

import static eu.maveniverse.maven.njord.shared.impl.store.ArtifactStoreUtils.validateArtifactStoreName;
import static eu.maveniverse.maven.njord.shared.impl.store.ArtifactStoreUtils.validateName;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import eu.maveniverse.maven.njord.shared.store.WriteMode;
import eu.maveniverse.maven.shared.core.component.CloseableSupport;
import eu.maveniverse.maven.shared.core.fs.DirectoryLocker;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

/**
 * Artifact store backed by NIO2 {@link Path}. It completely lies on file system.
 */
public class PathArtifactStore extends CloseableSupport implements ArtifactStore {
    private final String name;
    private final ArtifactStoreTemplate template;
    private final Instant created;
    private final RepositoryMode repositoryMode;
    private final WriteMode writeMode;
    private final List<ChecksumAlgorithmFactory> checksumAlgorithmFactories;
    private final List<String> omitChecksumsForExtensions;
    private final Artifact originProjectArtifact;
    private final Path basedir;
    private final DefaultLayout storeLayout;

    public PathArtifactStore(
            String name,
            ArtifactStoreTemplate template,
            Instant created,
            RepositoryMode repositoryMode,
            WriteMode writeMode,
            List<ChecksumAlgorithmFactory> checksumAlgorithmFactories,
            List<String> omitChecksumsForExtensions,
            Artifact originProjectArtifact, // nullable
            Path basedir) {
        this.name = validateArtifactStoreName(name);
        this.template = requireNonNull(template);
        this.created = requireNonNull(created);
        this.repositoryMode = requireNonNull(repositoryMode);
        this.writeMode = requireNonNull(writeMode);
        this.checksumAlgorithmFactories = requireNonNull(checksumAlgorithmFactories);
        this.omitChecksumsForExtensions = requireNonNull(omitChecksumsForExtensions);
        this.originProjectArtifact = originProjectArtifact;
        this.basedir = requireNonNull(basedir);
        this.storeLayout = new DefaultLayout();
    }

    public Path basedir() {
        checkClosed();
        return basedir;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ArtifactStoreTemplate template() {
        return template;
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
    public WriteMode writeMode() {
        return writeMode;
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
    public Optional<Artifact> originProjectArtifact() {
        return Optional.ofNullable(originProjectArtifact);
    }

    @Override
    public Collection<Artifact> artifacts() throws IOException {
        checkClosed();
        return readIndex("artifacts", l -> {
            String[] split = l.split("=");
            return new DefaultArtifact(split[0])
                    .setFile(basedir.resolve(split[1]).toFile());
        });
    }

    @Override
    public Collection<Metadata> metadata() throws IOException {
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
    public boolean artifactPresent(Artifact artifact) throws IOException {
        requireNonNull(artifact);
        Path file = basedir.resolve(storeLayout.artifactPath(artifact));
        return Files.isRegularFile(file);
    }

    @Override
    public boolean metadataPresent(Metadata metadata) throws IOException {
        requireNonNull(metadata);
        Path file = basedir.resolve(storeLayout.metadataPath(metadata));
        return Files.isRegularFile(file);
    }

    @Override
    public Optional<InputStream> artifactContent(Artifact artifact) throws IOException {
        requireNonNull(artifact);
        Path file = basedir.resolve(storeLayout.artifactPath(artifact));
        if (Files.isRegularFile(file)) {
            return Optional.of(Files.newInputStream(file));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<InputStream> metadataContent(Metadata metadata) throws IOException {
        requireNonNull(metadata);
        Path file = basedir.resolve(storeLayout.metadataPath(metadata));
        if (Files.isRegularFile(file)) {
            return Optional.of(Files.newInputStream(file));
        } else {
            return Optional.empty();
        }
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
    public void writeTo(Path directory) throws IOException {
        requireNonNull(directory);
        if (!Files.isDirectory(directory)) {
            throw new IOException("Directory does not exist");
        }

        FileUtils.copyRecursively(
                basedir(),
                directory,
                p -> p.getFileName() == null || !p.getFileName().toString().startsWith("."),
                false);
    }

    @Override
    public Operation put(Collection<Artifact> artifacts, Collection<Metadata> metadata) throws IOException {
        requireNonNull(artifacts);
        requireNonNull(metadata);
        checkClosed();
        if (!writeMode.allowWrite()) {
            throw new IOException(String.format("Store %s: does not allow write operations.", name));
        }

        DirectoryLocker.INSTANCE.unlockDirectory(basedir);
        DirectoryLocker.INSTANCE.lockDirectory(basedir, true);
        // check files (set + exists)
        List<Artifact> nfa = artifacts.stream()
                .filter(a -> a.getFile() == null || !a.getFile().isFile())
                .collect(Collectors.toList());
        if (!nfa.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Store %s: PUT Artifacts missing backing file: %s", name, nfa));
        }
        List<Metadata> nfm = metadata.stream()
                .filter(m -> m.getFile() == null || !m.getFile().isFile())
                .collect(Collectors.toList());
        if (!nfm.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Store %s: PUT Metadata missing backing file: %s", name, nfm));
        }
        // check RepositoryMode (snapshot vs release)
        List<Artifact> mismatch;
        if (!(mismatch = artifacts.stream()
                        .filter(repositoryMode().predicate().negate())
                        .collect(Collectors.toList()))
                .isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Store %s: PUT Artifacts repository policy mismatch (release vs snapshot): %s", name, mismatch));
        }
        // check for redeploy (target already exists)
        List<Artifact> redeploys;
        if (!writeMode.allowUpdate()
                && !(redeploys = artifacts.stream()
                                .filter(a -> Files.isRegularFile(basedir.resolve(storeLayout.artifactPath(a))))
                                .collect(Collectors.toList()))
                        .isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Store %s: Update/redeploy is forbidden (artifacts already exists): %s", name, redeploys));
        }

        final AtomicBoolean canceled = new AtomicBoolean(false);
        final AtomicBoolean closed = new AtomicBoolean(false);
        return new Operation() {
            @Override
            public void cancel() {
                canceled.set(true);
            }

            @Override
            public void close() throws IOException {
                if (closed.compareAndSet(false, true)) {
                    try {
                        if (!canceled.get()) {
                            appendIndex(
                                    "artifacts",
                                    artifacts,
                                    a -> ArtifactIdUtils.toId(a) + "=" + storeLayout.artifactPath(a));
                            appendIndex(
                                    "metadata",
                                    metadata,
                                    m -> String.format(
                                                    "%s:%s:%s:%s",
                                                    m.getGroupId(), m.getArtifactId(), m.getVersion(), m.getType())
                                            + "=" + storeLayout.metadataPath(m));
                        }
                    } finally {
                        DirectoryLocker.INSTANCE.unlockDirectory(basedir);
                        DirectoryLocker.INSTANCE.lockDirectory(basedir, false);
                    }
                }
            }
        };
    }

    @Override
    public boolean isEmpty() throws IOException {
        return artifacts().isEmpty() && metadata().isEmpty();
    }

    @Override
    public Collection<String> attachments() throws IOException {
        checkClosed();

        ArrayList<String> result = new ArrayList<>();
        try (Stream<Path> paths = Files.list(attachmentsDir(false))) {
            paths.filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Objects::toString)
                    .forEach(result::add);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public boolean attachmentPresent(String attachmentName) throws IOException {
        validateName(attachmentName);
        checkClosed();

        Path attachment = attachmentsDir(false).resolve(attachmentName);
        return Files.isRegularFile(attachment);
    }

    @Override
    public Optional<InputStream> attachmentContent(String attachmentName) throws IOException {
        validateName(attachmentName);
        checkClosed();

        Path attachment = attachmentsDir(false).resolve(attachmentName);
        if (Files.isRegularFile(attachment)) {
            return Optional.of(Files.newInputStream(attachment));
        }
        return Optional.empty();
    }

    @Override
    public AttachmentOperation manageAttachment(String attachmentName) throws IOException {
        validateName(attachmentName);
        checkClosed();

        if (!writeMode.allowWrite()) {
            throw new IOException(String.format("Store %s: does not allow write operations.", name));
        }
        if (attachmentPresent(attachmentName)) {
            throw new IllegalArgumentException(String.format(
                    "Store %s: Update/redeploy is forbidden (attachment already exists): %s", name, attachmentName));
        }
        DirectoryLocker.INSTANCE.unlockDirectory(basedir);
        DirectoryLocker.INSTANCE.lockDirectory(basedir, true);

        final Path attachmentPath = attachmentsDir(true).resolve(attachmentName);
        final AtomicReference<InputStream> write = new AtomicReference<>(null);
        final AtomicBoolean delete = new AtomicBoolean(false);
        final AtomicBoolean canceled = new AtomicBoolean(false);
        final AtomicBoolean closed = new AtomicBoolean(false);
        return new AttachmentOperation() {
            @Override
            public void write(InputStream inputStream) throws IOException {
                if (canceled.get() || delete.get()) {
                    throw new IOException("Operation canceled or delete already invoked");
                }
                if (!write.compareAndSet(null, inputStream)) {
                    throw new IOException("Operation write may be invoked only once");
                }
            }

            @Override
            public void delete() throws IOException {
                if (canceled.get() || write.get() != null) {
                    throw new IOException("Operation canceled or write already invoked");
                }
                if (!delete.compareAndSet(false, true)) {
                    throw new IOException("Operation delete may be invoked only once");
                }
            }

            @Override
            public void cancel() {
                canceled.set(true);
            }

            @Override
            public void close() throws IOException {
                if (closed.compareAndSet(false, true)) {
                    try {
                        if (!canceled.get()) {
                            if (delete.get()) {
                                Files.delete(attachmentPath);
                            } else if (write.get() != null) {
                                FileUtils.writeFile(attachmentPath, p -> Files.copy(write.get(), p));
                            }
                        }
                    } finally {
                        DirectoryLocker.INSTANCE.unlockDirectory(basedir);
                        DirectoryLocker.INSTANCE.lockDirectory(basedir, false);
                    }
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
        String origin = originProjectArtifact == null
                ? ""
                : " staged from " + ArtifactIdUtils.toId(originProjectArtifact) + " ";
        if (closed.get()) {
            return String.format(
                    "%s%s(%s, %s, %s, closed)",
                    name(), origin, created(), repositoryMode().name(), template.name());
        } else {
            try {
                return String.format(
                        "%s%s(%s, %s, %s, %s artifacts)",
                        name(),
                        origin,
                        created(),
                        repositoryMode().name(),
                        template.name(),
                        artifacts().size());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private Path attachmentsDir(boolean write) throws IOException {
        Path attachmentsPath = basedir.resolve(".attachments");
        if (write) {
            Files.createDirectories(attachmentsPath);
        }
        return attachmentsPath;
    }

    private <E> Collection<E> readIndex(String what, Function<String, E> transform) throws IOException {
        Path index = basedir.resolve(".meta").resolve(what);
        if (Files.isRegularFile(index)) {
            try (Stream<String> lines = Files.readAllLines(index, StandardCharsets.UTF_8).stream()) {
                return lines.map(transform).collect(Collectors.toSet());
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
            List<String> lines = entries.stream().map(transform).collect(Collectors.toList());
            Files.write(index, lines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        }
    }
}
