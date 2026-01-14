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
import eu.maveniverse.maven.njord.shared.impl.store.DefaultLayout;
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
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.util.artifact.SubArtifact;

@SuppressWarnings("rawtypes")
public class IpfsPublisher extends ArtifactStorePublisherSupport {
    private final IpfsPublisherConfig config;
    private final DefaultLayout layout;

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
        this.layout = new DefaultLayout();
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

    private Optional<Multihash> getFilesPathCid(IPFS ipfs, String path) {
        try {
            Map stat = ipfs.files.stat(path);
            return Optional.of(Multihash.decode((String) stat.get("Hash")));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<KeyInfo> getOrCreateKey(IPFS ipfs, String keyName, boolean create) throws IOException {
        Optional<KeyInfo> keyInfoOptional = ipfs.key.list().stream()
                .filter(k -> Objects.equals(keyName, k.name))
                .findAny();
        if (create && keyInfoOptional.isEmpty()) {
            keyInfoOptional = Optional.of(ipfs.key.gen(keyName, Optional.empty(), Optional.empty()));
        }
        return keyInfoOptional;
    }

    @Override
    protected void doPublish(ArtifactStore artifactStore) throws IOException {
        Optional<IPFS> ipfsOptional = connect();
        if (ipfsOptional.isEmpty()) {
            throw new IOException(String.format("Could not connect to IPFS node at address '%s'", config.multiaddr()));
        }

        if (session.config().dryRun()) {
            logger.info("Dry run; not publishing '{}' to IPFS", artifactStore.name());
            return;
        }

        final IPFS ipfs = ipfsOptional.orElseThrow(J8Utils.OET);
        final String nsRoot = URI.create("ipfs:///")
                .resolve(config.filesPrefix() + "/")
                .normalize()
                .getPath();
        final String root = URI.create("ipfs:///")
                .resolve(config.filesPrefix() + "/")
                .resolve(config.namespacePrefix() + "/")
                .normalize()
                .getPath();

        if (config.isPublishIPNS()) {
            logger.info("Refreshing IPNS at {}...", nsRoot);
            Optional<KeyInfo> keyInfo =
                    getOrCreateKey(ipfs, config.publishIPNSKeyName(), config.isPublishIPNSKeyCreate());
            if (keyInfo.isPresent()) {
                try {
                    Multihash namespaceCid = Multihash.decode(ipfs.name.resolve(keyInfo.orElseThrow().id));
                    ipfs.files.cp("/ipfs/" + namespaceCid.toBase58(), nsRoot, true);
                    ipfs.pin.add(namespaceCid);
                } catch (Exception e) {
                    // not yet published?; ignore
                    logger.debug("Could not refresh IPNS {}", keyInfo.orElseThrow().id);
                }
            } else {
                logger.info(
                        "Not refreshed: key '{}' not available and not allowed to create it",
                        config.publishIPNSKeyName());
            }
        }

        Optional<Multihash> oldNsCid = getFilesPathCid(ipfs, nsRoot);
        List<ChecksumAlgorithmFactory> checksumAlgorithmFactories = artifactStore.checksumAlgorithmFactories();
        int artifactCount = 0;
        int metadataCount = 0;
        for (Artifact artifact : artifactStore.artifacts()) {
            String path = root + layout.artifactPath(artifact);
            ipfs.files.write(
                    path,
                    new NamedStreamable.InputStreamWrapper(
                            artifactStore.artifactContent(artifact).orElseThrow(J8Utils.OET)),
                    true,
                    true);
            artifactCount++;
            for (ChecksumAlgorithmFactory checksumAlgorithmFactory : checksumAlgorithmFactories) {
                SubArtifact checksum = new SubArtifact(
                        artifact,
                        artifact.getClassifier(),
                        artifact.getExtension() + "." + checksumAlgorithmFactory.getFileExtension());
                String checksumPath = root + layout.artifactPath(checksum);
                ipfs.files.write(
                        checksumPath,
                        new NamedStreamable.InputStreamWrapper(
                                artifactStore.artifactContent(checksum).orElseThrow(J8Utils.OET)),
                        true,
                        true);
            }
        }
        for (Metadata metadata : artifactStore.metadata()) {
            String path = root + layout.metadataPath(metadata);
            ipfs.files.write(
                    path,
                    new NamedStreamable.InputStreamWrapper(
                            artifactStore.metadataContent(metadata).orElseThrow(J8Utils.OET)),
                    true,
                    true);
            metadataCount++;
            for (ChecksumAlgorithmFactory checksumAlgorithmFactory : checksumAlgorithmFactories) {
                Metadata checksum = new DefaultMetadata(
                        metadata.getGroupId(),
                        metadata.getArtifactId(),
                        metadata.getVersion(),
                        metadata.getType() + "." + checksumAlgorithmFactory.getFileExtension(),
                        metadata.getNature());
                String checksumPath = root + layout.metadataPath(checksum);
                ipfs.files.write(
                        checksumPath,
                        new NamedStreamable.InputStreamWrapper(
                                artifactStore.metadataContent(checksum).orElseThrow(J8Utils.OET)),
                        true,
                        true);
            }
        }
        Multihash newNsCid = getFilesPathCid(ipfs, nsRoot).orElseThrow(J8Utils.OET);
        logger.debug("Published {} artifacts ({} metadata) to IPFS", artifactCount, metadataCount);
        logger.info(
                "Published {} entries; oldRoot={} newRoot={}",
                artifactCount + metadataCount,
                oldNsCid.map(Multihash::toBase58).orElse("n/a"),
                newNsCid);
        if (config.isPublishIPNS()) {
            logger.info("Publishing {} to IPNS...", config.namespace());
            Optional<KeyInfo> keyInfo =
                    getOrCreateKey(ipfs, config.publishIPNSKeyName(), config.isPublishIPNSKeyCreate());
            if (keyInfo.isPresent()) {
                ipfs.pin.add(newNsCid);
                Map publish = ipfs.name.publish(newNsCid, Optional.of(keyInfo.orElseThrow(J8Utils.OET).name));
                logger.info("Published {} (pointing to {})", publish.get("Name"), publish.get("Value"));
            } else {
                logger.info(
                        "Not published: key '{}' not available nor allowed to create it", config.publishIPNSKeyName());
            }
        }
    }
}
