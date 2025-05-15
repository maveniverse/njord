/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.publisher.PublisherConfig;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.util.Optional;
import org.eclipse.aether.util.ConfigUtils;

/**
 * Sonatype Central Portal config.
 * <p>
 * User usually does not want to fiddle with these (as this is SaaS, so URLs are fixed).
 * Properties supported:
 * <ul>
 *     <li><code>njord.publisher.sonatype-cp.releaseRepositoryId</code> - the release service server.id</li>
 *     <li><code>njord.publisher.sonatype-cp.releaseRepositoryUrl</code> - the release service URL</li>
 *     <li><code>njord.publisher.sonatype-cp.snapshotRepositoryId</code> - the snapshot service server.id</li>
 *     <li><code>njord.publisher.sonatype-cp.snapshotRepositoryUrl</code> - the snapshot service URL</li>
 *     <li><code>njord.publisher.sonatype-cp.bundleName</code> (alias <code>njord.bundleName</code>) - the name to use for bundle</li>
 * </ul>
 * The property <code>njord.publisher.sonatype-cp.bundleName</code> defines the bundle name that is shown on CP WebUI.
 * Define it somehow, possible to be interpolated (in Maven or Maven Config, both are interpolated) to something
 * like <code>${project.artifactId}-${project.version}</code>.
 */
public final class SonatypeCentralPortalPublisherConfig extends PublisherConfig {
    public static final String RELEASE_REPOSITORY_ID = "sonatype-cp";
    public static final String RELEASE_REPOSITORY_URL = "https://central.sonatype.com/api/v1/publisher/upload";
    public static final String SNAPSHOT_REPOSITORY_ID = "sonatype-cp";
    public static final String SNAPSHOT_REPOSITORY_URL = "https://central.sonatype.com/repository/maven-snapshots";

    private final String bundleName;

    public SonatypeCentralPortalPublisherConfig(SessionConfig sessionConfig) {
        super(
                sessionConfig,
                SonatypeCentralPortalPublisherFactory.NAME,
                repositoryId(sessionConfig, RepositoryMode.RELEASE, RELEASE_REPOSITORY_ID),
                RELEASE_REPOSITORY_URL,
                repositoryId(sessionConfig, RepositoryMode.SNAPSHOT, SNAPSHOT_REPOSITORY_ID),
                SNAPSHOT_REPOSITORY_URL);

        // njord.publisher.sonatype-cp.bundleName
        this.bundleName = ConfigUtils.getString(
                sessionConfig.effectiveProperties(),
                null,
                keyName(SonatypeCentralPortalPublisherFactory.NAME, "bundleName"),
                SessionConfig.CONFIG_PREFIX + "bundleName");
    }

    public Optional<String> bundleName() {
        return Optional.ofNullable(bundleName);
    }
}
