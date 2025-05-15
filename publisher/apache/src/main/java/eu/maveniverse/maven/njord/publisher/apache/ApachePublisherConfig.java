/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.apache;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.publisher.PublisherConfig;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;

/**
 * ASF repository.apache.org config.
 * <p>
 * User usually does not want to fiddle with these (as this is SaaS, so URLs are fixed).
 * Properties supported:
 * <ul>
 *     <li><code>njord.publisher.apache-rao.releaseRepositoryId</code> - the release service server.id</li>
 *     <li><code>njord.publisher.apache-rao.releaseRepositoryUrl</code> - the release service URL</li>
 *     <li><code>njord.publisher.apache-rao.snapshotRepositoryId</code> - the snapshot service server.id</li>
 *     <li><code>njord.publisher.apache-rao.snapshotRepositoryUrl</code> - the snapshot service URL</li>
 * </ul>
 * If you are ASF publisher using Maven, you are most probably already set up, as <code>server.id</code> used
 * by default matches with those on ASF Maven Parent POM.
 */
public final class ApachePublisherConfig extends PublisherConfig {
    public static final String RELEASE_REPOSITORY_ID = "apache.releases.https";
    public static final String RELEASE_REPOSITORY_URL =
            "https://repository.apache.org/service/local/staging/deploy/maven2";
    public static final String SNAPSHOT_REPOSITORY_ID = "apache.snapshots.https";
    public static final String SNAPSHOT_REPOSITORY_URL = "https://repository.apache.org/content/repositories/snapshots";

    public ApachePublisherConfig(SessionConfig sessionConfig) {
        super(
                sessionConfig,
                ApacheRaoPublisherFactory.NAME,
                repositoryId(sessionConfig, RepositoryMode.RELEASE, RELEASE_REPOSITORY_ID),
                RELEASE_REPOSITORY_URL,
                repositoryId(sessionConfig, RepositoryMode.SNAPSHOT, SNAPSHOT_REPOSITORY_ID),
                SNAPSHOT_REPOSITORY_URL);
    }
}
