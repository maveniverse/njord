/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.util.Map;
import org.eclipse.aether.repository.RemoteRepository;

public class PublisherConfig {
    private final String releaseRepositoryId;
    private final String releaseRepositoryUrl;
    private final String snapshotRepositoryId;
    private final String snapshotRepositoryUrl;

    protected PublisherConfig(
            SessionConfig sessionConfig,
            String name,
            String defaultReleaseRepositoryId,
            String defaultReleaseRepositoryUrl,
            String defaultSnapshotRepositoryId,
            String defaultSnapshotRepositoryUrl) {
        requireNonNull(sessionConfig, "sessionConfig");
        requireNonNull(name);

        Map<String, String> effectiveProperties = sessionConfig.effectiveProperties();
        this.releaseRepositoryId = effectiveProperties.getOrDefault(
                "njord.publisher." + name + ".releaseRepositoryId", defaultReleaseRepositoryId);
        this.releaseRepositoryUrl = effectiveProperties.getOrDefault(
                "njord.publisher." + name + ".releaseRepositoryUrl", defaultReleaseRepositoryUrl);
        this.snapshotRepositoryId = effectiveProperties.getOrDefault(
                "njord.publisher." + name + ".snapshotRepositoryId", defaultSnapshotRepositoryId);
        this.snapshotRepositoryUrl = effectiveProperties.getOrDefault(
                "njord.publisher." + name + ".snapshotRepositoryUrl", defaultSnapshotRepositoryUrl);
    }

    protected static String repositoryId(SessionConfig sessionConfig, RepositoryMode mode, String defaultRepositoryId) {
        requireNonNull(sessionConfig);
        requireNonNull(mode);
        if (sessionConfig.currentProject().isPresent()) {
            RemoteRepository repository = sessionConfig
                    .currentProject()
                    .orElseThrow()
                    .distributionManagementRepositories()
                    .get(mode);
            if (repository != null) {
                return repository.getId();
            }
        }
        return defaultRepositoryId;
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
