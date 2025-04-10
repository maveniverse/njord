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

public final class SonatypeS01PublisherConfig {
    public static SonatypeS01PublisherConfig with(Config config) {
        requireNonNull(config, "config");

        String releaseRepositoryId = "sonatype-s01";
        String releaseRepositoryUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2";
        String snapshotRepositoryId = "sonatype-s01";
        String snapshotRepositoryUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots";

        if (config.effectiveProperties().containsKey("njord.publisher.sonatype-s01.releaseRepositoryId")) {
            releaseRepositoryId = config.effectiveProperties().get("njord.publisher.sonatype-s01.releaseRepositoryId");
        }
        if (config.effectiveProperties().containsKey("njord.publisher.sonatype-s01.releaseRepositoryUrl")) {
            releaseRepositoryUrl =
                    config.effectiveProperties().get("njord.publisher.sonatype-s01.releaseRepositoryUrl");
        }
        if (config.effectiveProperties().containsKey("njord.publisher.sonatype-s01.snapshotRepositoryId")) {
            snapshotRepositoryId =
                    config.effectiveProperties().get("njord.publisher.sonatype-s01.snapshotRepositoryId");
        }
        if (config.effectiveProperties().containsKey("njord.publisher.sonatype-s01.snapshotRepositoryUrl")) {
            snapshotRepositoryUrl =
                    config.effectiveProperties().get("njord.publisher.sonatype-s01.snapshotRepositoryUrl");
        }

        return new SonatypeS01PublisherConfig(
                releaseRepositoryId, releaseRepositoryUrl, snapshotRepositoryId, snapshotRepositoryUrl);
    }

    public static SonatypeS01PublisherConfig of(
            String releaseRepositoryId,
            String releaseRepositoryUrl,
            String snapshotRepositoryId,
            String snapshotRepositoryUrl) {
        return new SonatypeS01PublisherConfig(
                releaseRepositoryId, releaseRepositoryUrl, snapshotRepositoryId, snapshotRepositoryUrl);
    }

    public static final String NAME = "apache";

    private final String releaseRepositoryId;
    private final String releaseRepositoryUrl;
    private final String snapshotRepositoryId;
    private final String snapshotRepositoryUrl;

    private SonatypeS01PublisherConfig(
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
