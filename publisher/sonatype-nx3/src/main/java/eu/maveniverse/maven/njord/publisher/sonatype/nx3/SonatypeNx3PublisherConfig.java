/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype.nx3;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import java.time.Duration;
import java.util.Map;

/**
 * Sonatype Nexus Repository 3 publisher configuration.
 * <p>
 * This config handles NXRM3-specific settings like repository names and tags.
 * Repository URLs are configured via altDeploymentRepository or distributionManagement (handled by factory).
 * <p>
 * Properties supported:
 * <ul>
 *     <li><code>njord.publisher.sonatype-nx3.releaseRepositoryName</code> - the NXRM3 hosted repository name for releases (required)</li>
 *     <li><code>njord.publisher.sonatype-nx3.snapshotRepositoryName</code> - the NXRM3 hosted repository name for snapshots (optional)</li>
 *     <li><code>njord.publisher.sonatype-nx3.tag</code> or <code>njord.tag</code> - tag to apply to components (defaults to ${groupId}-${artifactId}-${version})</li>
 *     <li><code>njord.publisher.sonatype-nx3.connectTimeout</code> - HTTP connect timeout (default: PT30S)</li>
 *     <li><code>njord.publisher.sonatype-nx3.requestTimeout</code> - HTTP request timeout (default: PT5M)</li>
 * </ul>
 */
public final class SonatypeNx3PublisherConfig {
    private static final String DEFAULT_CONNECT_TIMEOUT = "PT30S";
    private static final String DEFAULT_REQUEST_TIMEOUT = "PT5M";

    private final String releaseRepositoryName;
    private final String snapshotRepositoryName;
    private final String tag;
    private final Duration connectTimeout;
    private final Duration requestTimeout;

    public SonatypeNx3PublisherConfig(SessionConfig sessionConfig) {
        requireNonNull(sessionConfig, "sessionConfig");

        Map<String, String> effectiveProperties = sessionConfig.effectiveProperties();

        // Required: release repository name
        this.releaseRepositoryName = effectiveProperties.get(keyName("releaseRepositoryName"));

        // Optional: snapshot repository name
        this.snapshotRepositoryName = effectiveProperties.get(keyName("snapshotRepositoryName"));

        // Tag: check specific property first, then generic njord.tag, then compute default
        String tagValue = effectiveProperties.get(keyName("tag"));
        if (tagValue == null) {
            tagValue = effectiveProperties.get("njord.tag");
        }
        if (tagValue == null && sessionConfig.currentProject().isPresent()) {
            SessionConfig.CurrentProject project =
                    sessionConfig.currentProject().get();
            tagValue =
                    project.artifact().getGroupId() + "-" + project.artifact().getArtifactId() + "-"
                            + project.artifact().getVersion();
        }
        this.tag = tagValue;

        // Timeouts with defaults
        String connectTimeoutStr = effectiveProperties.getOrDefault(keyName("connectTimeout"), DEFAULT_CONNECT_TIMEOUT);
        this.connectTimeout = Duration.parse(connectTimeoutStr);

        String requestTimeoutStr = effectiveProperties.getOrDefault(keyName("requestTimeout"), DEFAULT_REQUEST_TIMEOUT);
        this.requestTimeout = Duration.parse(requestTimeoutStr);
    }

    private static String keyName(String property) {
        requireNonNull(property);
        return "njord.publisher." + SonatypeNx3PublisherFactory.NAME + "." + property;
    }

    public String releaseRepositoryName() {
        return releaseRepositoryName;
    }

    public String snapshotRepositoryName() {
        return snapshotRepositoryName;
    }

    public String tag() {
        return tag;
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }
}
