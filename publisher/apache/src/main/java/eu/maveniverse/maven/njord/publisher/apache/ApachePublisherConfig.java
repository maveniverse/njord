/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.apache;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Config;

public final class ApachePublisherConfig {
    public static ApachePublisherConfig with(Config config) {
        requireNonNull(config, "config");

        String releaseRepositoryId = "apache.releases.https";
        String releaseRepositoryUrl = "https://repository.apache.org/service/local/staging/deploy/maven2";
        String snapshotRepositoryId = "apache.snapshots.https";
        String snapshotRepositoryUrl = "https://repository.apache.org/content/repositories/snapshots";

        if (config.effectiveProperties().containsKey("njord.publisher.apache-rao.releaseRepositoryId")) {
            releaseRepositoryId = config.effectiveProperties().get("njord.publisher.apache-rao.releaseRepositoryId");
        }
        if (config.effectiveProperties().containsKey("njord.publisher.apache-rao.releaseRepositoryUrl")) {
            releaseRepositoryUrl = config.effectiveProperties().get("njord.publisher.apache-rao.releaseRepositoryUrl");
        }
        if (config.effectiveProperties().containsKey("njord.publisher.apache-rao.snapshotRepositoryId")) {
            snapshotRepositoryId = config.effectiveProperties().get("njord.publisher.apache-rao.snapshotRepositoryId");
        }
        if (config.effectiveProperties().containsKey("njord.publisher.apache-rao.snapshotRepositoryUrl")) {
            snapshotRepositoryUrl =
                    config.effectiveProperties().get("njord.publisher.apache-rao.snapshotRepositoryUrl");
        }

        return new ApachePublisherConfig(
                releaseRepositoryId, releaseRepositoryUrl, snapshotRepositoryId, snapshotRepositoryUrl);
    }

    public static ApachePublisherConfig of(
            String releaseRepositoryId,
            String releaseRepositoryUrl,
            String snapshotRepositoryId,
            String snapshotRepositoryUrl) {
        return new ApachePublisherConfig(
                releaseRepositoryId, releaseRepositoryUrl, snapshotRepositoryId, snapshotRepositoryUrl);
    }

    private final String releaseRepositoryId;
    private final String releaseRepositoryUrl;
    private final String snapshotRepositoryId;
    private final String snapshotRepositoryUrl;

    private ApachePublisherConfig(
            String releaseRepositoryId,
            String releaseRepositoryUrl,
            String snapshotRepositoryId,
            String snapshotRepositoryUrl) {
        this.releaseRepositoryId = requireNonNull(releaseRepositoryId);
        this.releaseRepositoryUrl = requireNonNull(releaseRepositoryUrl);
        this.snapshotRepositoryId = requireNonNull(snapshotRepositoryId);
        this.snapshotRepositoryUrl = requireNonNull(snapshotRepositoryUrl);
    }

    public String releaseRepositoryId() {
        return releaseRepositoryId;
    }

    public String releaseRepositoryUrl() {
        return releaseRepositoryUrl;
    }

    public String snapshotRepositoryId() {
        return snapshotRepositoryId;
    }

    public String snapshotRepositoryUrl() {
        return snapshotRepositoryUrl;
    }
}
