/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype.cp;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.NjordRepositoryListener;
import eu.maveniverse.maven.njord.shared.publisher.PublisherConfigSupport;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.ConfigUtils;

/**
 * Sonatype Central Portal config.
 * <p>
 * User usually does not want to fiddle with these (as this is SaaS, so URLs are fixed).
 * Properties supported:
 * <ul>
 *     <li><code>njord.publisher.sonatype-cp.bundleName</code> (alias <code>njord.bundleName</code>) - the name to use for bundle</li>
 *     <li><code>njord.publisher.sonatype-cp.publishingType</code> (alias <code>njord.publishingType</code>) - the "publishing type": USER_MANAGED, AUTOMATIC</li>
 *     <li><code>njord.publisher.sonatype-cp.waitForStates</code> (alias <code>njord.waitForStates</code>) - should publisher wait for state transitions? (def: false)</li>
 *     <li><code>njord.publisher.sonatype-cp.waitForStatesTimeout</code> (alias <code>njord.waitForStatesTimeout</code>) - how long should publisher wait for validation in total? (def: PT15M)</li>
 *     <li><code>njord.publisher.sonatype-cp.waitForStatesSleep</code> (alias <code>njord.waitForStatesSleep</code>) - how long should publisher sleep between each state check (def: PT10S)</li>
 *     <li><code>njord.publisher.sonatype-cp.waitForStatesWaitStates</code> (alias <code>njord.waitForStatesWaitStates</code>) - the comma separated states that publisher should wait CP to transition from (def: "pending,validating")</li>
 *     <li><code>njord.publisher.sonatype-cp.waitForStatesFailureStates</code> (alias <code>njord.waitForStatesFailureStates</code>) - the comma separated states that publisher should consider as failure (def: "failed")</li>
 * </ul>
 * The property <code>njord.publisher.sonatype-cp.bundleName</code> defines the bundle name that is shown on CP WebUI.
 * By default, value of <code>${project.artifactId}-${project.version}</code> is used IF current project is present.
 * Also <a href="https://central.sonatype.com/api-doc">see API documentation.</a>
 * Note: publishingType, waitForStatesWaitStates and waitForStatesFailureStates are case-insensitive (are converted to required case).
 */
public final class SonatypeCentralPortalPublisherConfig extends PublisherConfigSupport {
    public static final String RELEASE_REPOSITORY_ID = "sonatype-cp";
    public static final String RELEASE_REPOSITORY_URL = "https://central.sonatype.com/api/v1/publisher/upload";
    public static final String SNAPSHOT_REPOSITORY_ID = "sonatype-cp";
    public static final String SNAPSHOT_REPOSITORY_URL = "https://central.sonatype.com/repository/maven-snapshots/";

    private final String bundleName;
    private final String publishingType;
    private final boolean waitForStates;
    private final Duration waitForStatesTimeout;
    private final Duration waitForStatesSleep;
    private final Set<String> waitForStatesWaitStates;
    private final Set<String> waitForStatesFailureStates;
    private final NjordRepositoryListener.Mode listenerMode;

    public SonatypeCentralPortalPublisherConfig(SessionConfig sessionConfig) {
        super(SonatypeCentralPortalPublisherFactory.NAME, sessionConfig);
        // njord.publisher.sonatype-cp.bundleName
        this.bundleName = ConfigUtils.getString(sessionConfig.effectiveProperties(), null, keyNames("bundleName"));

        // njord.publisher.sonatype-cp.publishingType
        this.publishingType =
                ConfigUtils.getString(sessionConfig.effectiveProperties(), null, keyNames("publishingType"));

        // njord.publisher.sonatype-cp.waitForStates
        this.waitForStates =
                ConfigUtils.getBoolean(sessionConfig.effectiveProperties(), false, keyNames("waitForStates"));

        // njord.publisher.sonatype-cp.waitForStatesTimeout
        this.waitForStatesTimeout = Duration.parse(
                ConfigUtils.getString(sessionConfig.effectiveProperties(), "PT15M", keyNames("waitForStatesTimeout")));
        if (this.waitForStatesTimeout.isNegative()) {
            throw new IllegalArgumentException("waitForStatesTimeout cannot be negative");
        }

        // njord.publisher.sonatype-cp.waitForStatesSleep
        this.waitForStatesSleep = Duration.parse(
                ConfigUtils.getString(sessionConfig.effectiveProperties(), "PT10S", keyNames("waitForStatesSleep")));
        if (this.waitForStatesSleep.isNegative()) {
            throw new IllegalArgumentException("waitForStatesSleep cannot be negative");
        }

        // njord.publisher.sonatype-cp.waitForStatesWaitStates
        this.waitForStatesWaitStates = Collections.unmodifiableSet(
                new HashSet<>(ConfigUtils.parseCommaSeparatedUniqueNames(ConfigUtils.getString(
                                sessionConfig.effectiveProperties(),
                                "pending,validating",
                                keyNames("waitForStatesWaitStates"))
                        .toLowerCase(Locale.ENGLISH))));

        // njord.publisher.sonatype-cp.waitForStatesFailureStates
        this.waitForStatesFailureStates = Collections.unmodifiableSet(
                new HashSet<>(ConfigUtils.parseCommaSeparatedUniqueNames(ConfigUtils.getString(
                                sessionConfig.effectiveProperties(), "failed", keyNames("waitForStatesFailureStates"))
                        .toLowerCase(Locale.ENGLISH))));

        this.listenerMode = NjordRepositoryListener.Mode.valueOf(ConfigUtils.getString(
                        sessionConfig.effectiveProperties(),
                        NjordRepositoryListener.Mode.AGGREGATED.name(),
                        keyNames("listenerMode"))
                .toUpperCase(Locale.ROOT));
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

    public Optional<String> bundleName() {
        return Optional.ofNullable(bundleName);
    }

    public Optional<String> publishingType() {
        if (publishingType == null) {
            return Optional.empty();
        } else {
            return Optional.of(publishingType.toUpperCase(Locale.ENGLISH));
        }
    }

    public boolean waitForStates() {
        return waitForStates;
    }

    public Duration waitForStatesTimeout() {
        return waitForStatesTimeout;
    }

    public Duration waitForStatesSleep() {
        return waitForStatesSleep;
    }

    public Set<String> waitForStatesWaitStates() {
        return waitForStatesWaitStates;
    }

    public Set<String> waitForStatesFailureStates() {
        return waitForStatesFailureStates;
    }

    public NjordRepositoryListener.Mode listenerMode() {
        return listenerMode;
    }
}
