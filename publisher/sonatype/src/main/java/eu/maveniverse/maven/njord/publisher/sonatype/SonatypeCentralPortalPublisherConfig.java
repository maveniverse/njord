/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Config;

public final class SonatypeCentralPortalPublisherConfig {
    public static SonatypeCentralPortalPublisherConfig with(Config config) {
        requireNonNull(config, "config");

        String releaseRepositoryId = "sonatype-cp";
        String releaseRepositoryUrl = "https://central.sonatype.com/api/v1/publisher/upload";
        String snapshotRepositoryId = "sonatype-cp";
        String snapshotRepositoryUrl = "https://central.sonatype.com/repository/maven-snapshots";

        if (config.effectiveProperties().containsKey("njord.publisher.sonatype-cp.releaseRepositoryId")) {
            releaseRepositoryId = config.effectiveProperties().get("njord.publisher.sonatype-s01.releaseRepositoryId");
        }
        if (config.effectiveProperties().containsKey("njord.publisher.sonatype-cp.releaseRepositoryUrl")) {
            releaseRepositoryUrl = config.effectiveProperties().get("njord.publisher.sonatype-cp.releaseRepositoryUrl");
        }
        if (config.effectiveProperties().containsKey("njord.publisher.sonatype-cp.snapshotRepositoryId")) {
            snapshotRepositoryId = config.effectiveProperties().get("njord.publisher.sonatype-cp.snapshotRepositoryId");
        }
        if (config.effectiveProperties().containsKey("njord.publisher.sonatype-cp.snapshotRepositoryUrl")) {
            snapshotRepositoryUrl =
                    config.effectiveProperties().get("njord.publisher.sonatype-cp.snapshotRepositoryUrl");
        }

        return new SonatypeCentralPortalPublisherConfig(
                releaseRepositoryId, releaseRepositoryUrl, snapshotRepositoryId, snapshotRepositoryUrl);
    }

    public static SonatypeCentralPortalPublisherConfig of(
            String releaseRepositoryId,
            String releaseRepositoryUrl,
            String snapshotRepositoryId,
            String snapshotRepositoryUrl) {
        return new SonatypeCentralPortalPublisherConfig(
                releaseRepositoryId, releaseRepositoryUrl, snapshotRepositoryId, snapshotRepositoryUrl);
    }

    private final String releaseRepositoryId;
    private final String releaseRepositoryUrl;
    private final String snapshotRepositoryId;
    private final String snapshotRepositoryUrl;

    private SonatypeCentralPortalPublisherConfig(
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
