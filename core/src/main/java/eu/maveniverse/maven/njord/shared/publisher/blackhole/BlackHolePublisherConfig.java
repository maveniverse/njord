/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher.blackhole;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.publisher.PublisherConfig;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;

/**
 * Black Hole publisher config.
 * <p>
 * Properties supported:
 * <ul>
 *     <li><code>njord.publisher.black-hole.releaseRepositoryId</code> - the release service server.id (default "releases")</li>
 *     <li><code>njord.publisher.black-hole.releaseRepositoryUrl</code> - the release service URL (defaults to {@code "https://maveniverse.eu/releases"} non-existent service)</li>
 *     <li><code>njord.publisher.black-hole.snapshotRepositoryId</code> - the snapshot service server.id (default "snapshots")</li>
 *     <li><code>njord.publisher.black-hole.snapshotRepositoryUrl</code> - the snapshot service URL (defaults to {@code "https://maveniverse.eu/snapshots"} non-existent service)</li>
 *     <li><code>njord.publisher.black-hole.artifactStoreRequirements</code> - the artifact store requirements to apply</li>
 *     <li><code>njord.publisher.black-hole.fail</code> - to fail on publishing</li>
 * </ul>
 */
public final class BlackHolePublisherConfig extends PublisherConfig {
    private final boolean fail;
    private final String artifactStoreRequirements;

    public BlackHolePublisherConfig(SessionConfig sessionConfig) {
        super(
                sessionConfig,
                BlackHolePublisherFactory.NAME,
                repositoryId(sessionConfig, RepositoryMode.RELEASE, "releases"),
                sessionConfig
                        .effectiveProperties()
                        .getOrDefault(
                                keyName(BlackHolePublisherFactory.NAME, "releaseRepositoryUrl"),
                                "https://maveniverse.eu/releases"),
                repositoryId(sessionConfig, RepositoryMode.SNAPSHOT, "snapshots"),
                sessionConfig
                        .effectiveProperties()
                        .getOrDefault(
                                keyName(BlackHolePublisherFactory.NAME, "snapshotRepositoryUrl"),
                                "https://maveniverse.eu/snapshots"));
        this.fail = Boolean.parseBoolean(sessionConfig
                .effectiveProperties()
                .getOrDefault(keyName(BlackHolePublisherFactory.NAME, "fail"), Boolean.FALSE.toString()));
        this.artifactStoreRequirements = sessionConfig
                .effectiveProperties()
                .getOrDefault(
                        keyName(BlackHolePublisherFactory.NAME, "artifactStoreRequirements"),
                        ArtifactStoreRequirements.NONE.name());
    }

    public boolean fail() {
        return fail;
    }

    public String artifactStoreRequirements() {
        return artifactStoreRequirements;
    }
}
