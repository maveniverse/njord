/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.store;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.shared.core.component.CloseableSupport;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

public class FilteredArtifactStore extends CloseableSupport implements ArtifactStore {
    private final ArtifactStore delegate;
    private final Predicate<Artifact> artifactfilter;
    private final Predicate<Metadata> metadataFilter;

    public FilteredArtifactStore(
            ArtifactStore delegate, Predicate<Artifact> artifactfilter, Predicate<Metadata> metadataFilter) {
        this.delegate = requireNonNull(delegate);
        this.artifactfilter = requireNonNull(artifactfilter);
        this.metadataFilter = requireNonNull(metadataFilter);
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public ArtifactStoreTemplate template() {
        return delegate.template();
    }

    @Override
    public Instant created() {
        return delegate.created();
    }

    @Override
    public RepositoryMode repositoryMode() {
        return delegate.repositoryMode();
    }

    @Override
    public boolean allowRedeploy() {
        return delegate.allowRedeploy();
    }

    @Override
    public List<ChecksumAlgorithmFactory> checksumAlgorithmFactories() {
        return delegate.checksumAlgorithmFactories();
    }

    @Override
    public List<String> omitChecksumsForExtensions() {
        return delegate.omitChecksumsForExtensions();
    }

    @Override
    public Collection<Artifact> artifacts() {
        return delegate.artifacts().stream().filter(artifactfilter).collect(Collectors.toList());
    }

    @Override
    public Collection<Metadata> metadata() {
        return delegate.metadata().stream().filter(metadataFilter).collect(Collectors.toList());
    }

    @Override
    public boolean artifactPresent(Artifact artifact) throws IOException {
        if (artifactfilter.test(artifact)) {
            return delegate.artifactPresent(artifact);
        } else {
            return false;
        }
    }

    @Override
    public boolean metadataPresent(Metadata metadata) throws IOException {
        if (metadataFilter.test(metadata)) {
            return delegate.metadataPresent(metadata);
        } else {
            return false;
        }
    }

    @Override
    public Optional<InputStream> artifactContent(Artifact artifact) throws IOException {
        if (artifactfilter.test(artifact)) {
            return delegate.artifactContent(artifact);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<InputStream> metadataContent(Metadata metadata) throws IOException {
        if (metadataFilter.test(metadata)) {
            return delegate.metadataContent(metadata);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public RepositorySystemSession storeRepositorySession(RepositorySystemSession session) {
        return delegate.storeRepositorySession(session);
    }

    @Override
    public RemoteRepository storeRemoteRepository() {
        return delegate.storeRemoteRepository();
    }

    @Override
    public void writeTo(Path directory, Layout layout) throws IOException {
        requireNonNull(directory);
        if (!Files.isDirectory(directory)) {
            throw new IOException("Directory does not exist");
        }
        for (Artifact artifact : artifacts()) {
            String artifactPath = layout.artifactPath(artifact);
            Path targetPath = directory.resolve(artifactPath);
            Optional<InputStream> artifactContent = artifactContent(artifact);
            if (artifactContent.isPresent()) {
                try (InputStream content = artifactContent.orElseThrow()) {
                    FileUtils.writeFile(targetPath, p -> Files.copy(content, p));
                }
            }
        }
        for (Metadata metadata : metadata()) {
            String metadataPath = layout.metadataPath(metadata);
            Path targetPath = directory.resolve(metadataPath);
            Optional<InputStream> metadataContent = metadataContent(metadata);
            if (metadataContent.isPresent()) {
                try (InputStream content = metadataContent.orElseThrow()) {
                    FileUtils.writeFile(targetPath, p -> Files.copy(content, p));
                }
            }
        }
    }

    @Override
    public ArtifactStore.Operation put(Collection<Artifact> artifacts, Collection<Metadata> metadata)
            throws IOException {
        return delegate.put(artifacts, metadata);
    }

    @Override
    protected void doClose() throws IOException {
        delegate.close();
    }
}
