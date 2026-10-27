/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype.nx3;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.publisher.PublisherConfigSupport;
import java.util.Map;
import org.eclipse.aether.util.ConfigUtils;

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
 * </ul>
 */
public class SonatypeNx3PublisherConfig extends PublisherConfigSupport {
    private final String releaseRepositoryName;
    private final String snapshotRepositoryName;
    private final boolean tagConfigured;
    private final String tag;

    public SonatypeNx3PublisherConfig(SessionConfig sessionConfig) {
        super(SonatypeNx3PublisherFactory.NAME, sessionConfig);

        Map<String, String> effectiveProperties = sessionConfig.effectiveProperties();

        // Required: release repository name
        this.releaseRepositoryName =
                ConfigUtils.getString(effectiveProperties, null, keyNames("releaseRepositoryName"));

        // Optional: snapshot repository name
        this.snapshotRepositoryName =
                ConfigUtils.getString(effectiveProperties, null, keyNames("snapshotRepositoryName"));

        // Tag: check specific property first, then generic njord.tag, then compute default
        String tagValue = ConfigUtils.getString(effectiveProperties, null, keyNames("tag"));
        boolean tagConfigured = tagValue != null;
        if (tagValue == null && sessionConfig.currentProject().isPresent()) {
            SessionConfig.CurrentProject project =
                    sessionConfig.currentProject().get();
            tagValue =
                    project.artifact().getGroupId() + "-" + project.artifact().getArtifactId() + "-"
                            + project.artifact().getVersion();
        }
        this.tagConfigured = tagConfigured;
        this.tag = tagValue;
    }

    public String releaseRepositoryName() {
        return releaseRepositoryName;
    }

    public String snapshotRepositoryName() {
        return snapshotRepositoryName;
    }

    public boolean isTagConfigured() {
        return tagConfigured;
    }

    public String tag() {
        return tag;
    }
}
