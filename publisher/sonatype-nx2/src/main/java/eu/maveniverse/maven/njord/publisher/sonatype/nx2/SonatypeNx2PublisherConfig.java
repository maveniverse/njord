/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype.nx2;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.publisher.PublisherConfig;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;

/**
 * Sonatype NX2 config.
 * <p>
 * Properties supported (note: at least the URLs must be provided as defaults are most probably wrong):
 * <ul>
 *     <li><code>njord.publisher.sonatype-nx2.releaseRepositoryId</code> - the release service server.id (default "releases")</li>
 *     <li><code>njord.publisher.sonatype-nx2.releaseRepositoryUrl</code> - the release service URL <em>(default to https://localhost:8081/content/repositories/releases/)</em></li>
 *     <li><code>njord.publisher.sonatype-nx2.snapshotRepositoryId</code> - the snapshot service server.id (default "snapshots")</li>
 *     <li><code>njord.publisher.sonatype-nx2.snapshotRepositoryUrl</code> - the snapshot service URL <em>(default to https://localhost:8081/content/repositories/snapshots/)</em></li>
 *     <li><code>njord.publisher.sonatype-nx2.artifactStoreRequirements</code> - the requirements deployment must fulfil (defaults to NONE)</li>
 * </ul>
 */
public final class SonatypeNx2PublisherConfig extends PublisherConfig {
    private final String artifactStoreRequirements;

    public SonatypeNx2PublisherConfig(SessionConfig sessionConfig) {
        super(
                sessionConfig,
                SonatypeNx2PublisherFactory.NAME,
                repositoryId(sessionConfig, RepositoryMode.RELEASE, "releases"),
                sessionConfig
                        .effectiveProperties()
                        .getOrDefault(
                                keyName(SonatypeNx2PublisherFactory.NAME, "releaseRepositoryUrl"),
                                "https://localhost:8081/content/repositories/releases/"),
                repositoryId(sessionConfig, RepositoryMode.SNAPSHOT, "snapshots"),
                sessionConfig
                        .effectiveProperties()
                        .getOrDefault(
                                keyName(SonatypeNx2PublisherFactory.NAME, "snapshotRepositoryUrl"),
                                "https://localhost:8081/content/repositories/snapshots/"));
        this.artifactStoreRequirements = sessionConfig
                .effectiveProperties()
                .getOrDefault(
                        keyName(SonatypeNx2PublisherFactory.NAME, "artifactStoreRequirements"),
                        ArtifactStoreRequirements.NONE.name());
    }

    public String getArtifactStoreRequirements() {
        return artifactStoreRequirements;
    }
}
