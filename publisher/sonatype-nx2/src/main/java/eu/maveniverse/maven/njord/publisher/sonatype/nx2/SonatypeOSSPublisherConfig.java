/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype.nx2;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.publisher.PublisherConfig;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;

/**
 * Sonatype OSS config.
 * <p>
 * User usually does not want to fiddle with these (as this is SaaS, so URLs are fixed).
 * Properties supported:
 * <ul>
 *     <li><code>njord.publisher.sonatype-oss.releaseRepositoryId</code> - the release service server.id</li>
 *     <li><code>njord.publisher.sonatype-oss.releaseRepositoryUrl</code> - the release service URL</li>
 *     <li><code>njord.publisher.sonatype-oss.snapshotRepositoryId</code> - the snapshot service server.id</li>
 *     <li><code>njord.publisher.sonatype-oss.snapshotRepositoryUrl</code> - the snapshot service URL</li>
 * </ul>
 */
public final class SonatypeOSSPublisherConfig extends PublisherConfig {
    public static final String RELEASE_REPOSITORY_ID = "sonatype-oss";
    public static final String RELEASE_REPOSITORY_URL = "https://oss.sonatype.org/service/local/staging/deploy/maven2/";
    public static final String SNAPSHOT_REPOSITORY_ID = "sonatype-oss";
    public static final String SNAPSHOT_REPOSITORY_URL = "https://oss.sonatype.org/content/repositories/snapshots/";

    public SonatypeOSSPublisherConfig(SessionConfig sessionConfig) {
        super(
                sessionConfig,
                SonatypeOSSPublisherFactory.NAME,
                repositoryId(sessionConfig, RepositoryMode.RELEASE, RELEASE_REPOSITORY_ID),
                RELEASE_REPOSITORY_URL,
                repositoryId(sessionConfig, RepositoryMode.SNAPSHOT, SNAPSHOT_REPOSITORY_ID),
                SNAPSHOT_REPOSITORY_URL);
    }
}
