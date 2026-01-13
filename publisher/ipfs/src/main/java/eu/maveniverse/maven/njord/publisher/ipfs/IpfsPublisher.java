/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.ipfs;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherSupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import io.ipfs.api.IPFS;
import io.ipfs.api.KeyInfo;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

@SuppressWarnings("rawtypes")
public class IpfsPublisher extends ArtifactStorePublisherSupport {
    private final IpfsPublisherConfig config;

    public IpfsPublisher(
            Session session,
            RepositorySystem repositorySystem,
            IpfsPublisherConfig config,
            ArtifactStoreRequirements artifactStoreRequirements) {
        super(
                session,
                repositorySystem,
                IpfsPublisherFactory.NAME,
                "Publishes to IPFS",
                config.targetReleaseRepository(),
                config.targetSnapshotRepository(),
                config.targetReleaseRepository(),
                config.targetSnapshotRepository(),
                artifactStoreRequirements);
        this.config = requireNonNull(config);
    }

    @Override
    public boolean isConfigured() {
        return connect().isPresent();
    }

    private Optional<IPFS> connect() {
        try {
            IPFS ipfs = new IPFS(config.multiaddr());
            Map id = ipfs.id();
            logger.debug("Connected to IPFS w/ ID={} node at '{}'", id.get("ID"), config.multiaddr());
            return Optional.of(ipfs);
        } catch (Exception e) {
            logger.warn("Could not connect to IPFS node at '{}'", config.multiaddr(), e);
            return Optional.empty();
        }
    }

    private Optional<String> getFilesPathCid(IPFS ipfs, String path) {
        try {
            Map stat = ipfs.files.stat(path);
            return Optional.of((String) stat.get("Hash"));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<KeyInfo> getOrCreateKey(IPFS ipfs, String keyName, boolean create) throws IOException {
        Optional<KeyInfo> keyInfoOptional = ipfs.key.list().stream()
                .filter(k -> Objects.equals(keyName, k.name))
                .findAny();
        if (create && !keyInfoOptional.isPresent()) {
            keyInfoOptional = Optional.of(ipfs.key.gen(keyName, Optional.empty(), Optional.empty()));
        }
        return keyInfoOptional;
    }

    @Override
    protected void doPublish(ArtifactStore artifactStore) throws IOException {
        if (session.config().dryRun()) {
            logger.info("Dry run; not publishing '{}' to IPFS", artifactStore.name());
            return;
        }
        Optional<IPFS> ipfsOptional = connect();
        if (!ipfsOptional.isPresent()) {
            throw new IOException(String.format("Could not connect to IPFS node at address '%s'", config.multiaddr()));
        }
        IPFS ipfs = ipfsOptional.orElseThrow(J8Utils.OET);
        URI root = URI.create("ipfs:///").resolve(config.prefix());
        Optional<String> oldPathCid = getFilesPathCid(ipfs, root.getPath());
        List<ChecksumAlgorithmFactory> checksumAlgorithmFactories = artifactStore.checksumAlgorithmFactories();
        int artifactCount = 0;
        int metadataCount = 0;
        for (Artifact artifact : artifactStore.artifacts()) {
            URI path = root.resolve(Layout.artifactRepositoryPath(artifact));
            ipfs.files.write(
                    path.getPath(),
                    new NamedStreamable.InputStreamWrapper(
                            artifactStore.artifactContent(artifact).orElseThrow(J8Utils.OET)),
                    true,
                    true);
            artifactCount++;
        }
        for (Metadata metadata : artifactStore.metadata()) {
            URI path = root.resolve(Layout.metadataRepositoryPath(metadata));
            ipfs.files.write(
                    path.getPath(),
                    new NamedStreamable.InputStreamWrapper(
                            artifactStore.metadataContent(metadata).orElseThrow(J8Utils.OET)),
                    true,
                    true);
            metadataCount++;
        }
        String newPathCid = getFilesPathCid(ipfs, root.getPath()).orElseThrow(J8Utils.OET);
        logger.debug("Published {} artifacts ({} metadata) to IPFS", artifactCount, metadataCount);
        logger.info(
                "Published {} entries; oldRoot={} newRoot={}",
                artifactCount + metadataCount,
                oldPathCid.orElse("n/a"),
                newPathCid);
        if (config.isPublish()) {
            logger.info("Publishing IPNS entry...");
            Optional<KeyInfo> keyInfo = getOrCreateKey(ipfs, config.publishKeyName(), config.isPublishKeyCreate());
            if (keyInfo.isPresent()) {
                Map publish = ipfs.name.publish(
                        Multihash.decode(newPathCid), Optional.of(keyInfo.orElseThrow(J8Utils.OET).name));
                logger.info("Published {} (pointing to {})", publish.get("Name"), publish.get("Value"));
            } else {
                logger.info("Not published: key '{}' not available nor allowed to create it", config.publishKeyName());
            }
        }
    }
}
