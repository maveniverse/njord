/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.store;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.ComponentSupport;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMerger;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithm;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

public class DefaultArtifactStoreMerger extends ComponentSupport implements ArtifactStoreMerger {
    private final SessionConfig sessionConfig;
    private final RepositorySystem repositorySystem;
    private final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;

    public DefaultArtifactStoreMerger(
            SessionConfig sessionConfig,
            RepositorySystem repositorySystem,
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        this.sessionConfig = requireNonNull(sessionConfig);
        this.repositorySystem = requireNonNull(repositorySystem);
        this.checksumAlgorithmFactorySelector = requireNonNull(checksumAlgorithmFactorySelector);
    }

    @Override
    public void redeploy(ArtifactStore source, ArtifactStore target) throws IOException {
        requireNonNull(source);
        requireNonNull(target);

        if (source.repositoryMode() != target.repositoryMode()) {
            throw new IllegalArgumentException("Redeploy not possible; stores use different repository mode");
        }
        logger.info("Redeploying {} -> {}", source, target);
        String targetName = target.name();
        target.close();
        try (ArtifactStore from = source) {
            new ArtifactStoreDeployer(
                            repositorySystem,
                            sessionConfig.session(),
                            new RemoteRepository.Builder(targetName, "default", "njord:store:" + targetName).build())
                    .deploy(from);
        }
    }

    @Override
    public void merge(ArtifactStore source, ArtifactStore target) throws IOException {
        requireNonNull(source);
        requireNonNull(target);

        if (source.repositoryMode() != RepositoryMode.RELEASE || target.repositoryMode() != RepositoryMode.RELEASE) {
            throw new IllegalArgumentException("Merge not possible; one or both stores are not RELEASE");
        }
        logger.info("Merging {} -> {}", source, target);
        ArrayList<Artifact> toBeWritten = new ArrayList<>();
        for (Artifact sourceArtifact : source.artifacts()) {
            if (!target.artifactPresent(sourceArtifact)) {
                toBeWritten.add(sourceArtifact);
            } else {
                // must be same or error
                String sourceSha1 = null;
                String targetSha1 = null;
                Optional<InputStream> contentOptional;

                // must be present in source; comes from it
                contentOptional = source.artifactContent(sourceArtifact);
                try (InputStream content = contentOptional.orElseThrow()) {
                    sourceSha1 = checksumSha1(content);
                }
                // must be present in target; it told us so
                contentOptional = target.artifactContent(sourceArtifact);
                try (InputStream content = contentOptional.orElseThrow()) {
                    targetSha1 = checksumSha1(content);
                }

                if (!Objects.equals(sourceSha1, targetSha1)) {
                    throw new IOException(String.format(
                            "Conflict: both stores contains %s with different content (%s vs %s)",
                            ArtifactIdUtils.toId(sourceArtifact), sourceSha1, targetSha1));
                }
            }
        }

        String targetName = target.name();
        target.close();
        try (ArtifactStore from = source) {
            new ArtifactStoreDeployer(
                            repositorySystem,
                            sessionConfig.session(),
                            new RemoteRepository.Builder(targetName, "default", "njord:store:" + targetName).build())
                    .deploy(from, toBeWritten);
        }
    }

    private String checksumSha1(InputStream inputStream) throws IOException {
        ChecksumAlgorithm alg = checksumAlgorithmFactorySelector.select("SHA-1").getAlgorithm();
        final byte[] buffer = new byte[1024 * 32];
        for (; ; ) {
            int read = inputStream.read(buffer);
            if (read < 0) {
                break;
            }
            alg.update(ByteBuffer.wrap(buffer, 0, read));
        }
        return alg.checksum();
    }
}
