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
import eu.maveniverse.maven.njord.shared.impl.ResolverUtils;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
 * <p>
 * This Mojo is useful in complex workflows/scenarios, where something (fx a build) should happen after publishing done.
 * While some services do provide status of (non-atomic) publishing, this mojo checks the "real thing": when it
 * succeeds, the artifacts can be resolved 100% by builds from given remote repository. Hence, this mojo
 * can work with all publishers available out there, and even with in-house MRMs solutions as well.
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
     * If mojo set to {@link #wait}, the delay duration before the first check happens (as {@link Duration} string).
     * The {@link #waitTimeout} <em>does not include this delay</em>, so "worst case" total execution time of this
     * mojo when set to wait is {@code waitDelay + waitTimeout}.
     */
    @Parameter(required = true, property = "waitDelay", defaultValue = "PT10M")
    private String waitDelay;

    /**
     * If mojo set to {@link #wait}, the sleep duration between checks (as {@link Duration} string).
     */
    @Parameter(required = true, property = "waitSleep", defaultValue = "PT1M")
    private String waitSleep;

    /**
     * The comma separated list of artifacts to check availability for. If this parameter is set, the mojo
     * will use this list instead to go for {@link ArtifactStore}. The comma separated list should contain
     * artifacts in form of {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}.
     * <p>
     * Parameter may point to an existing text file as well, which contains on each line an artifact string
     * in format above. The file may contain empty lines and lines starting with {@code #} (comments) that
     * are ignored.
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
            try {
                Path artifactsFile = Paths.get(artifacts);
                if (Files.exists(artifactsFile) && !Files.isDirectory(artifactsFile)) {
                    logger.debug("Mojo parameter artifacts points to an existing file; loading it up");
                    this.artifacts = Files.readAllLines(artifactsFile).stream()
                            .map(String::trim)
                            .filter(l -> !l.isEmpty())
                            .filter(line -> !line.startsWith("#"))
                            .collect(Collectors.joining(","));
                }
            } catch (InvalidPathException e) {
                // ignore
            }
            logger.debug("Artifacts list: {}", this.artifacts);
            HashSet<Boolean> snaps = new HashSet<>();
            List<Artifact> artifacts = Arrays.stream(this.artifacts.split("[,\\s]"))
                    .map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .map(DefaultArtifact::new)
                    .peek(a -> snaps.add(a.isSnapshot()))
                    .collect(Collectors.toList());
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
                    checkAvailability(from.artifacts(), pto.orElseThrow(J8Utils.OET));
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

    /**
     * Creates (potentially auth and proxy) equipped {@link RemoteRepository} if able to. If user set
     * {@link #remoteRepository} parameter, it wins over {@link ArtifactStorePublisher}.
     * <p>
     * The returned repository has not set any policy, this method just sets {@code id} and {@code url}. So to say,
     * this method return "raw" or "bare" repositories.
     */
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
            RemoteRepository bare = ResolverUtils.parseRemoteRepositoryString(remoteRepository);
            if (mode == RepositoryMode.SNAPSHOT) {
                result = Optional.of(new RemoteRepository.Builder(bare)
                        .setReleasePolicy(new RepositoryPolicy(false, null, null))
                        .build());
            } else {
                result = Optional.of(new RemoteRepository.Builder(bare)
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

    /**
     * Uses Resolver to perform "existence checks" for given artifacts in given remote repository target. Based on
     * parameters it may do "one pass" or "wait/poll".
     */
    protected void checkAvailability(Collection<Artifact> artifacts, RemoteRepository target)
            throws IOException, MojoFailureException {
        Duration waitTimeout = Duration.parse(this.waitTimeout);
        Duration waitDelay = Duration.parse(this.waitDelay);
        Duration waitSleep = Duration.parse(this.waitSleep);
        Map<Artifact, Boolean> artifactsMap = artifacts.stream().collect(Collectors.toMap(a -> a, a -> false));

        try (RepositoryConnector repositoryConnector =
                repositoryConnectorProvider.newRepositoryConnector(mavenSession.getRepositorySession(), target)) {
            if (wait) {
                logger.info(
                        "Waiting for {} artifacts to become available from {} (poll {}; delay {}; timeout {})",
                        artifactsMap.size(),
                        target.getUrl(),
                        waitSleep,
                        waitDelay,
                        waitTimeout);
                Thread.sleep(waitDelay.toMillis());
            } else {
                logger.info("Checking for {} artifacts availability from {}", artifactsMap.size(), target.getUrl());
            }
            Instant waitingUntil = Instant.now().plus(waitTimeout);
            AtomicInteger toCheck = new AtomicInteger(artifactsMap.size());
            while (toCheck.get() > 0) {
                logger.info("Checking availability of {} artifacts (out of {}).", toCheck.get(), artifactsMap.size());
                List<ArtifactDownload> artifactDownloads = new ArrayList<>();
                artifactsMap.forEach((key, value) -> {
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
                        artifactsMap.put(d.getArtifact(), true);
                    }
                });

                if (toCheck.get() == 0) {
                    logger.info("All {} artifacts are available.", artifactsMap.size());
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
