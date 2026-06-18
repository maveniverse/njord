/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.apache;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.NjordRepositoryListener;
import eu.maveniverse.maven.njord.shared.publisher.PublisherConfigSupport;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.util.Locale;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.ConfigUtils;

/**
 * Apache RAO publisher config.
 */
public final class ApacheRaoPublisherConfig extends PublisherConfigSupport {
    public static final String RELEASE_REPOSITORY_ID = "apache.releases.https";
    public static final String RELEASE_REPOSITORY_URL =
            "https://repository.apache.org/service/local/staging/deploy/maven2/";
    public static final String SNAPSHOT_REPOSITORY_ID = "apache.snapshots.https";
    public static final String SNAPSHOT_REPOSITORY_URL =
            "https://repository.apache.org/content/repositories/snapshots/";

    private final NjordRepositoryListener.Mode listenerMode;

    public ApacheRaoPublisherConfig(SessionConfig sessionConfig) {
        super(ApacheRaoPublisherFactory.NAME, sessionConfig);

        this.listenerMode = NjordRepositoryListener.Mode.valueOf(ConfigUtils.getString(
                        sessionConfig.effectiveProperties(),
                        NjordRepositoryListener.Mode.AGGREGATED.name(),
                        keyNames("listenerMode"))
                .toUpperCase(Locale.ROOT));
    }

    public NjordRepositoryListener.Mode listenerMode() {
        return listenerMode;
    }

    @Override
    protected RemoteRepository createServiceReleaseRepository() {
        RemoteRepository result;
        if (targetReleaseRepository == null) {
            result = new RemoteRepository.Builder(
                            repositoryId(RepositoryMode.RELEASE, RELEASE_REPOSITORY_ID),
                            "default",
                            RELEASE_REPOSITORY_URL)
                    .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                    .build();
        } else {
            result = new RemoteRepository.Builder(targetReleaseRepository.getId(), "default", RELEASE_REPOSITORY_URL)
                    .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                    .build();
        }
        return result;
    }

    @Override
    protected RemoteRepository createServiceSnapshotRepository() {
        RemoteRepository result;
        if (targetSnapshotRepository == null) {
            result = new RemoteRepository.Builder(
                            repositoryId(RepositoryMode.SNAPSHOT, SNAPSHOT_REPOSITORY_ID),
                            "default",
                            SNAPSHOT_REPOSITORY_URL)
                    .setReleasePolicy(new RepositoryPolicy(false, null, null))
                    .build();
        } else {
            result = new RemoteRepository.Builder(targetSnapshotRepository.getId(), "default", SNAPSHOT_REPOSITORY_URL)
                    .setReleasePolicy(new RepositoryPolicy(false, null, null))
                    .build();
        }
        return result;
    }
}
