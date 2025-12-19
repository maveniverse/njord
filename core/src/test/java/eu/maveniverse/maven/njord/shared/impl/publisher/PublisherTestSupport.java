/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.extensions.mmr.MavenModelReader;
import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.SessionFactory;
import eu.maveniverse.maven.njord.shared.impl.DefaultSessionFactory;
import eu.maveniverse.maven.njord.shared.impl.store.DefaultArtifactStoreMergerFactory;
import eu.maveniverse.maven.njord.shared.impl.store.DefaultArtifactStoreWriterFactory;
import eu.maveniverse.maven.njord.shared.impl.store.DefaultInternalArtifactStoreManagerFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreValidator;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.internal.impl.checksum.DefaultChecksumAlgorithmFactorySelector;
import org.eclipse.aether.internal.impl.checksum.Md5ChecksumAlgorithmFactory;
import org.eclipse.aether.internal.impl.checksum.Sha1ChecksumAlgorithmFactory;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

public class PublisherTestSupport {
    protected final Map<String, ChecksumAlgorithmFactory> checksumAlgorithmFactories;
    protected final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;

    public PublisherTestSupport() {
        this.checksumAlgorithmFactories = new HashMap<>();
        this.checksumAlgorithmFactories.put(Sha1ChecksumAlgorithmFactory.NAME, new Sha1ChecksumAlgorithmFactory());
        this.checksumAlgorithmFactories.put(Md5ChecksumAlgorithmFactory.NAME, new Md5ChecksumAlgorithmFactory());
        this.checksumAlgorithmFactorySelector =
                new DefaultChecksumAlgorithmFactorySelector(this.checksumAlgorithmFactories);
    }

    protected Path basedir() throws IOException {
        Path basedir = Paths.get("target/test-base/" + getClass().getSimpleName());
        Files.createDirectories(basedir);
        return basedir;
    }

    protected Path userHome() throws IOException {
        Path dir = basedir().resolve("home");
        Files.createDirectories(dir);
        return dir;
    }

    protected Path cwd() throws IOException {
        Path dir = basedir().resolve("cwd");
        Files.createDirectories(dir);
        return dir;
    }

    protected ArtifactStorePublisher sonatypeCp() {
        return new ArtifactStorePublisher() {
            @Override
            public String name() {
                return "sonatype-cp";
            }

            @Override
            public String description() {
                return "The Sonatype Central Portal";
            }

            @Override
            public boolean isConfigured() {
                return false;
            }

            @Override
            public Optional<RemoteRepository> targetReleaseRepository() {
                return Optional.empty();
            }

            @Override
            public Optional<RemoteRepository> targetSnapshotRepository() {
                return Optional.empty();
            }

            @Override
            public Optional<RemoteRepository> serviceReleaseRepository() {
                return Optional.empty();
            }

            @Override
            public Optional<RemoteRepository> serviceSnapshotRepository() {
                return Optional.empty();
            }

            @Override
            public ArtifactStoreRequirements artifactStoreRequirements() {
                return ArtifactStoreRequirements.NONE;
            }

            @Override
            public Optional<ArtifactStoreValidator.ValidationResult> validate(ArtifactStore artifactStore)
                    throws IOException {
                return Optional.empty();
            }

            @Override
            public void publish(ArtifactStore artifactStore) throws IOException {
                throw new UnsupportedOperationException();
            }
        };
    }

    protected Session createSession(Context context, SessionConfig config) {
        requireNonNull(config);
        HashMap<String, ArtifactStorePublisherFactory> publishers = new HashMap<>();
        publishers.put("sonatype-cp", c -> sonatypeCp());
        SessionFactory factory = new DefaultSessionFactory(
                new RuntimeInformation() {
                    @Override
                    public String getMavenVersion() {
                        return "3.9.11";
                    }

                    @Override
                    public boolean isMavenVersion(String s) {
                        return getMavenVersion().equals(s);
                    }
                },
                new DefaultInternalArtifactStoreManagerFactory(checksumAlgorithmFactorySelector),
                new DefaultArtifactStoreWriterFactory(),
                new DefaultArtifactStoreMergerFactory(context.repositorySystem(), checksumAlgorithmFactorySelector),
                new DefaultArtifactPublisherRedirectorFactory(context.repositorySystem()),
                publishers,
                Collections.emptyMap(),
                new MavenModelReader(context).getImpl());
        return factory.create(config);
    }

    protected RemoteRepository njordRemoteRepository() {
        return new RemoteRepository.Builder("njord", "default", "njord:").build();
    }

    protected ArtifactStore artifactStore(RemoteRepository repository, Artifact... artifacts) {
        Instant now = Instant.now();
        Map<String, Artifact> contents =
                Arrays.stream(artifacts).collect(Collectors.toMap(ArtifactIdUtils::toId, a -> a));
        return new ArtifactStore() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public ArtifactStoreTemplate template() {
                return ArtifactStoreTemplate.RELEASE_SCA;
            }

            @Override
            public Instant created() {
                return now;
            }

            @Override
            public RepositoryMode repositoryMode() {
                return RepositoryMode.RELEASE;
            }

            @Override
            public boolean allowRedeploy() {
                return false;
            }

            @Override
            public List<ChecksumAlgorithmFactory> checksumAlgorithmFactories() {
                return Collections.singletonList(new Sha1ChecksumAlgorithmFactory());
            }

            @Override
            public List<String> omitChecksumsForExtensions() {
                return Collections.singletonList(".asc");
            }

            @Override
            public Optional<Artifact> originProjectArtifact() {
                return Optional.empty();
            }

            @Override
            public Collection<Artifact> artifacts() {
                return Collections.unmodifiableCollection(contents.values());
            }

            @Override
            public Collection<Metadata> metadata() {
                return Collections.emptyList();
            }

            @Override
            public boolean artifactPresent(Artifact artifact) throws IOException {
                return contents.containsKey(ArtifactIdUtils.toId(artifact));
            }

            @Override
            public boolean metadataPresent(Metadata metadata) throws IOException {
                return false;
            }

            @Override
            public Optional<InputStream> artifactContent(Artifact artifact) throws IOException {
                Artifact c = contents.get(ArtifactIdUtils.toId(artifact));
                if (c != null) {
                    return Optional.of(Files.newInputStream(c.getFile().toPath()));
                }
                return Optional.empty();
            }

            @Override
            public Optional<InputStream> metadataContent(Metadata metadata) throws IOException {
                return Optional.empty();
            }

            @Override
            public RepositorySystemSession storeRepositorySession(RepositorySystemSession session) {
                return session;
            }

            @Override
            public RemoteRepository storeRemoteRepository() {
                return repository;
            }

            @Override
            public void writeTo(Path directory) throws IOException {
                throw new RuntimeException("not implemented");
            }

            @Override
            public Operation put(Collection<Artifact> artifacts, Collection<Metadata> metadata) throws IOException {
                throw new RuntimeException("not implemented");
            }

            @Override
            public void close() throws IOException {}

            @Override
            public boolean isEmpty() {
                return contents.isEmpty();
            }
        };
    }
}
