/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.extension3;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.ArtifactTransfer;
import org.eclipse.aether.spi.connector.ArtifactUpload;
import org.eclipse.aether.spi.connector.MetadataDownload;
import org.eclipse.aether.spi.connector.MetadataTransfer;
import org.eclipse.aether.spi.connector.MetadataUpload;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.transfer.ArtifactTransferException;
import org.eclipse.aether.transfer.MetadataTransferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Njord connector.
 */
public class NjordRepositoryConnector implements RepositoryConnector {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ArtifactStore artifactStore;
    private final RemoteRepository remoteRepository;
    private final RepositoryConnector original;
    private final RepositoryConnector staging;

    public NjordRepositoryConnector(
            ArtifactStore artifactStore,
            RemoteRepository remoteRepository,
            RepositoryConnector original,
            RepositoryConnector staging) {
        this.artifactStore = requireNonNull(artifactStore);
        this.remoteRepository = requireNonNull(remoteRepository);
        this.original = original; // nullable
        this.staging = requireNonNull(staging);
    }

    private boolean deployIntent(Collection<? extends ArtifactTransfer> at, Collection<? extends MetadataTransfer> mt) {
        if (original == null) {
            return true;
        }
        int nnt = 0; // these are nullable, so non-null-transfers
        int dt = 0; // deploy-transfers; these cannot be mixed, either all of them are or none
        if (at != null) {
            for (ArtifactTransfer t : at) {
                if (t != null) {
                    nnt++;
                    if (deployIntent(t.getTrace())) {
                        dt++;
                    }
                }
            }
        }
        if (mt != null) {
            for (MetadataTransfer t : mt) {
                if (t != null) {
                    nnt++;
                    if (deployIntent(t.getTrace())) {
                        dt++;
                    }
                }
            }
        }
        // TODO: currently nnt > 0 AND dt != null is impossible
        return nnt > 0 && dt == nnt;
    }

    private boolean deployIntent(RequestTrace trace) {
        while (trace != null && !(trace.getData() instanceof DeployRequest)) {
            trace = trace.getParent();
        }
        return trace != null;
    }

    @Override
    public void get(
            Collection<? extends ArtifactDownload> artifactDownloads,
            Collection<? extends MetadataDownload> metadataDownloads) {
        if (deployIntent(artifactDownloads, metadataDownloads)) {
            staging.get(artifactDownloads, metadataDownloads);
        } else {
            original.get(artifactDownloads, metadataDownloads);
        }
    }

    @Override
    public void put(
            Collection<? extends ArtifactUpload> artifactUploads,
            Collection<? extends MetadataUpload> metadataUploads) {
        if (deployIntent(artifactUploads, metadataUploads)) {
            try (ArtifactStore.Operation putOperation = artifactStore.put(
                    artifactUploads != null
                            ? artifactUploads.stream()
                                    .map(u -> u.getArtifact().setFile(u.getFile()))
                                    .collect(Collectors.toList())
                            : Collections.emptyList(),
                    metadataUploads != null
                            ? metadataUploads.stream()
                                    .map(u -> u.getMetadata().setFile(u.getFile()))
                                    .collect(Collectors.toList())
                            : Collections.emptyList())) {
                staging.put(artifactUploads, metadataUploads);
                if (artifactUploads != null && artifactUploads.stream().anyMatch(u -> u.getException() != null)) {
                    putOperation.cancel();
                    throw new IOException("PUT failed");
                }
                if (metadataUploads != null && metadataUploads.stream().anyMatch(u -> u.getException() != null)) {
                    putOperation.cancel();
                    throw new IOException("PUT failed");
                }
            } catch (IOException e) {
                if (artifactUploads != null) {
                    artifactUploads.stream()
                            .filter(u -> u.getException() == null)
                            .forEach(u -> u.setException(
                                    new ArtifactTransferException(u.getArtifact(), remoteRepository, e)));
                }
                if (metadataUploads != null) {
                    metadataUploads.stream()
                            .filter(u -> u.getException() == null)
                            .forEach(u -> u.setException(
                                    new MetadataTransferException(u.getMetadata(), remoteRepository, e)));
                }
            }
        } else {
            original.put(artifactUploads, metadataUploads);
        }
    }

    @Override
    public void close() {
        try {
            artifactStore.close();
        } catch (Exception e) {
            logger.warn("Failed to close artifactStore", e);
        }
        if (original != null) {
            try {
                original.close();
            } catch (Exception e) {
                logger.warn("Failed to close original connector", e);
            }
        }
        try {
            staging.close();
        } catch (Exception e) {
            logger.warn("Failed to close staging connector", e);
        }
    }
}
