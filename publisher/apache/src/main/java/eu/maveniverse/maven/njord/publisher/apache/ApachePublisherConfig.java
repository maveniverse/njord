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
                RELEASE_REPOSITORY_ID,
                RELEASE_REPOSITORY_URL,
                SNAPSHOT_REPOSITORY_ID,
                SNAPSHOT_REPOSITORY_URL);
    }
}
