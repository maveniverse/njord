/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.RepositoryConnectorProvider;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.transfer.NoRepositoryConnectorException;

/**
 * A mojo that checks availability for all artifacts on given remote repository. The mojo by default blocks/waits
 * for configured time and polls remote repository at given intervals, but can be used for "one pass" checks as well.
 * <p>
 * By default, the mojo will use {@link ArtifactStore} as "artifacts source" and
 * {@link ArtifactStorePublisher#targetReleaseRepository()} or {@link ArtifactStorePublisher#targetSnapshotRepository()}
 * as source of remote repository, but user can configure list of artifacts and remote repository directly as well.
 */
@Mojo(name = "check-artifacts-availability", threadSafe = true, requiresProject = false, aggregator = true)
public class CheckArtifactsAvailabilityMojo extends PublisherSupportMojo {
    /**
     * If using {@link ArtifactStore} as artifact source, whether source store should be dropped after successful operation.
     */
    @Parameter(required = true, property = "drop", defaultValue = "true")
    private boolean drop;

    /**
     * Should the mojo block/wait for artifacts to become available, or should just perform one-pass of check.
     */
    @Parameter(required = true, property = "wait", defaultValue = "true")
    private boolean wait;

    /**
     * If mojo set to {@link #wait}, the total allowed wait duration (as {@link Duration} string).
     */
    @Parameter(required = true, property = "waitTimeout", defaultValue = "PT1H")
    private String waitTimeout;

    /**
     * If mojo set to {@link #wait}, the sleep duration between checks (as {@link Duration} string).
     */
    @Parameter(required = true, property = "waitSleep", defaultValue = "PT1M")
    private String waitSleep;

    /**
     * The comma separated list of artifacts to check availability for. If this parameter is set, the mojo
     * will use this list instead to go for {@link ArtifactStore}. The comma separated list should contain
     * artifacts in form of {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}.
     */
    @Parameter(property = "artifacts")
    private String artifacts;

    /**
     * The string representing remote repository where to check availability from in form of usual
     * {@code id::url}. If this parameter is set, the mojo will use this remote repository instead to go for
     * {@link ArtifactStorePublisher} and get the URL from there.
     */
    @Parameter(property = "remoteRepository")
    private String remoteRepository;

    @Component
    private RepositoryConnectorProvider repositoryConnectorProvider;

    @Override
    protected void doWithSession(Session ns) throws IOException, MojoFailureException {
        if (artifacts != null) {
            HashSet<Boolean> snaps = new HashSet<>();
            Map<Artifact, Boolean> artifacts = Arrays.stream(this.artifacts.split(","))
                    .map(DefaultArtifact::new)
                    .peek(a -> snaps.add(a.isSnapshot()))
                    .collect(Collectors.toMap(a -> a, a -> false));
            if (snaps.size() != 1) {
                throw new IllegalArgumentException(
                        "Provided artifactList parameter must be uniform re snapshot (must be all release or all snapshot)");
            }
            Optional<RemoteRepository> pto;
            if (snaps.contains(Boolean.TRUE)) {
                pto = getRemoteRepository(ns, RepositoryMode.SNAPSHOT);
            } else {
                pto = getRemoteRepository(ns, RepositoryMode.RELEASE);
            }
            if (pto.isPresent()) {
                checkAvailability(artifacts, pto.orElseThrow(J8Utils.OET));
            } else {
                logger.info("No publishing target exists; bailing out");
                throw new MojoFailureException("No publishing target exists");
            }
        } else {
            try (ArtifactStore from = getArtifactStore(ns)) {
                Optional<RemoteRepository> pto = getRemoteRepository(ns, from.repositoryMode());
                if (pto.isPresent()) {
                    Map<Artifact, Boolean> artifacts =
                            from.artifacts().stream().collect(Collectors.toMap(a -> a, a -> false));
                    checkAvailability(artifacts, pto.orElseThrow(J8Utils.OET));
                } else {
                    logger.info("No publishing target exists; bailing out");
                    throw new MojoFailureException("No publishing target exists");
                }
            }
            if (drop) {
                logger.info("Dropping {}", store);
                ns.artifactStoreManager().dropArtifactStore(store);
            }
        }
    }

    protected Optional<RemoteRepository> getRemoteRepository(Session ns, RepositoryMode mode)
            throws MojoFailureException {
        Optional<RemoteRepository> result;
        if (remoteRepository == null) {
            ArtifactStorePublisher publisher = getArtifactStorePublisher(ns);
            switch (mode) {
                case RELEASE:
                    result = publisher.targetReleaseRepository();
                    break;
                case SNAPSHOT:
                    result = publisher.targetSnapshotRepository();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown repository mode: " + mode);
            }
        } else {
            String[] split = remoteRepository.split("::");
            if (split.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid alt deployment repository syntax (supported is id::url): " + remoteRepository);
            }
            String id = split[0];
            String url = split[1];
            if (mode == RepositoryMode.SNAPSHOT) {
                result = Optional.of(new RemoteRepository.Builder(id, "default", url)
                        .setReleasePolicy(new RepositoryPolicy(false, null, null))
                        .build());
            } else {
                result = Optional.of(new RemoteRepository.Builder(id, "default", url)
                        .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                        .build());
            }
        }
        if (result.isPresent()) {
            result = Optional.of(repositorySystem.newDeploymentRepository(
                    mavenSession.getRepositorySession(), result.orElseThrow(J8Utils.OET)));
        }
        return result;
    }

    protected void checkAvailability(Map<Artifact, Boolean> artifacts, RemoteRepository target)
            throws IOException, MojoFailureException {
        Duration waitTimeout = Duration.parse(this.waitTimeout);
        Duration waitSleep = Duration.parse(this.waitSleep);
        Instant waitingUntil = Instant.now().plus(waitTimeout);
        AtomicInteger toCheck = new AtomicInteger(artifacts.size());
        logger.info("Waiting for {} artifacts to become available from {}", artifacts.size(), target.getUrl());
        try (RepositoryConnector repositoryConnector =
                repositoryConnectorProvider.newRepositoryConnector(mavenSession.getRepositorySession(), target)) {
            while (toCheck.get() > 0) {
                logger.info("Checking availability of {} artifacts (out of {}).", toCheck.get(), artifacts.size());
                List<ArtifactDownload> artifactDownloads = new ArrayList<>();
                artifacts.forEach((key, value) -> {
                    if (!value) {
                        ArtifactDownload artifactDownload = new ArtifactDownload(key, "njord", null, null);
                        artifactDownload.setRepositories(Collections.singletonList(target));
                        artifactDownload.setExistenceCheck(true);
                        artifactDownloads.add(artifactDownload);
                    }
                });
                repositoryConnector.get(artifactDownloads, null);
                artifactDownloads.forEach(d -> {
                    if (d.getException() == null) {
                        toCheck.decrementAndGet();
                        artifacts.put(d.getArtifact(), true);
                    }
                });

                if (toCheck.get() == 0) {
                    logger.info("All {} artifacts are available.", artifacts.size());
                    break;
                }

                if (!wait || Instant.now().isAfter(waitingUntil)) {
                    artifactDownloads.forEach(d -> {
                        if (d.getException() != null) {
                            logger.warn(
                                    "Artifact {} failed for {}",
                                    d.getArtifact(),
                                    d.getException().getMessage());
                        }
                    });
                    throw new MojoFailureException(
                            wait
                                    ? "Timeout on checking availability of artifacts on " + target
                                    : "Checking availability of artifacts on " + target + " failed");
                }
                Thread.sleep(waitSleep.toMillis());
            }
        } catch (NoRepositoryConnectorException e) {
            logger.info("No connector for publishing target exists; bailing out");
            throw new MojoFailureException("No connector for publishing target exists");
        } catch (InterruptedException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
