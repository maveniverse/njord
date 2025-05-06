/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.SessionConfig;

public final class SonatypeOSSPublisherConfig {
    public static SonatypeOSSPublisherConfig with(SessionConfig config) {
        requireNonNull(config, "config");

        String releaseRepositoryId = "sonatype-oss";
        String releaseRepositoryUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2";
        String snapshotRepositoryId = "sonatype-oss";
        String snapshotRepositoryUrl = "https://oss.sonatype.org/content/repositories/snapshots";

        if (config.effectiveProperties().containsKey("njord.publisher.sonatype-oss.releaseRepositoryId")) {
            releaseRepositoryId = config.effectiveProperties().get("njord.publisher.sonatype-oss.releaseRepositoryId");
        }
        if (config.effectiveProperties().containsKey("njord.publisher.sonatype-oss.releaseRepositoryUrl")) {
            releaseRepositoryUrl =
                    config.effectiveProperties().get("njord.publisher.sonatype-oss.releaseRepositoryUrl");
        }
        if (config.effectiveProperties().containsKey("njord.publisher.sonatype-oss.snapshotRepositoryId")) {
            snapshotRepositoryId =
                    config.effectiveProperties().get("njord.publisher.sonatype-oss.snapshotRepositoryId");
        }
        if (config.effectiveProperties().containsKey("njord.publisher.sonatype-oss.snapshotRepositoryUrl")) {
            snapshotRepositoryUrl =
                    config.effectiveProperties().get("njord.publisher.sonatype-oss.snapshotRepositoryUrl");
        }

        return new SonatypeOSSPublisherConfig(
                releaseRepositoryId, releaseRepositoryUrl, snapshotRepositoryId, snapshotRepositoryUrl);
    }

    public static SonatypeOSSPublisherConfig of(
            String releaseRepositoryId,
            String releaseRepositoryUrl,
            String snapshotRepositoryId,
            String snapshotRepositoryUrl) {
        return new SonatypeOSSPublisherConfig(
                releaseRepositoryId, releaseRepositoryUrl, snapshotRepositoryId, snapshotRepositoryUrl);
    }

    private final String releaseRepositoryId;
    private final String releaseRepositoryUrl;
    private final String snapshotRepositoryId;
    private final String snapshotRepositoryUrl;

    private SonatypeOSSPublisherConfig(
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
