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
import java.io.IOException;
import java.io.InputStream;
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
    private final Predicate<Artifact> filter;

    public FilteredArtifactStore(ArtifactStore delegate, Predicate<Artifact> filter) {
        this.delegate = requireNonNull(delegate);
        this.filter = requireNonNull(filter);
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
        return delegate.artifacts().stream().filter(filter).collect(Collectors.toList());
    }

    @Override
    public Collection<Metadata> metadata() {
        return delegate.metadata();
    }

    @Override
    public boolean artifactPresent(Artifact artifact) throws IOException {
        if (filter.test(artifact)) {
            return delegate.artifactPresent(artifact);
        } else {
            return false;
        }
    }

    @Override
    public boolean metadataPresent(Metadata metadata) throws IOException {
        return delegate.metadataPresent(metadata);
    }

    @Override
    public Optional<InputStream> artifactContent(Artifact artifact) throws IOException {
        if (filter.test(artifact)) {
            return delegate.artifactContent(artifact);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<InputStream> metadataContent(Metadata metadata) throws IOException {
        return delegate.metadataContent(metadata);
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
    public void writeTo(Path directory) throws IOException {
        for (Artifact artifact : artifacts()) {}

        delegate.writeTo(directory);
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
