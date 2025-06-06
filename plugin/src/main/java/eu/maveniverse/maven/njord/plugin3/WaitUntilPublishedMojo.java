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
import java.util.Collections;
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
import org.eclipse.aether.impl.RepositoryConnectorProvider;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.transfer.NoRepositoryConnectorException;

/**
 * A mojo that waits for all the store contents become available on publishing target from publishing source.
 * In other words, waits for all artifacts to become "published".
 */
@Mojo(name = "wait-until-published", threadSafe = true, requiresProject = false, aggregator = true)
public class WaitUntilPublishedMojo extends PublisherSupportMojo {
    /**
     * Whether source store should be dropped after successful operation.
     */
    @Parameter(required = true, property = "drop", defaultValue = "true")
    private boolean drop;

    @Component
    private RepositoryConnectorProvider repositoryConnectorProvider;

    @Override
    protected void doWithSession(Session ns) throws IOException, MojoFailureException {
        try (ArtifactStore from = getArtifactStore(ns)) {
            RepositoryMode mode = from.repositoryMode();
            ArtifactStorePublisher publisher = getArtifactStorePublisher(ns);
            Optional<RemoteRepository> pto;
            switch (mode) {
                case RELEASE:
                    pto = publisher.targetReleaseRepository();
                    break;
                case SNAPSHOT:
                    pto = publisher.targetSnapshotRepository();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown repository mode: " + mode);
            }
            if (pto.isPresent()) {
                // TODO: parameterize this
                Duration waitTimeout = Duration.parse("PT1M");
                Duration waitSleep = Duration.parse("PT5S");

                Instant waitingUntil = Instant.now().plus(waitTimeout);
                Map<Artifact, Boolean> artifacts =
                        from.artifacts().stream().collect(Collectors.toMap(a -> a, a -> false));
                AtomicInteger toCheck = new AtomicInteger(artifacts.size());
                RemoteRepository publishingTarget = pto.orElseThrow(J8Utils.OET);
                logger.info("Waiting for {} artifacts to become available from {}", artifacts.size(), publishingTarget);
                try (RepositoryConnector repositoryConnector = repositoryConnectorProvider.newRepositoryConnector(
                        mavenSession.getRepositorySession(), publishingTarget)) {
                    while (toCheck.get() > 0) {
                        logger.info(
                                "Checking reachability of {} artifacts (out of {}).", toCheck.get(), artifacts.size());
                        List<ArtifactDownload> artifactDownloads = new ArrayList<>();
                        artifacts.forEach((key, value) -> {
                            if (!value) {
                                ArtifactDownload artifactDownload = new ArtifactDownload(key, "njord", null, null);
                                artifactDownload.setRepositories(Collections.singletonList(publishingTarget));
                                artifactDownload.setExistenceCheck(true);
                                artifactDownloads.add(artifactDownload);
                            }
                        });
                        repositoryConnector.get(artifactDownloads, null);
                        artifactDownloads.forEach(d -> {
                            boolean exists = d.getException() == null;
                            if (exists) {
                                toCheck.decrementAndGet();
                                artifacts.put(d.getArtifact(), true);
                            }
                        });

                        if (toCheck.get() == 0) {
                            break;
                        }

                        if (Instant.now().isAfter(waitingUntil)) {
                            artifactDownloads.forEach(d -> {
                                if (d.getException() != null) {
                                    logger.warn(
                                            "Artifact {} failed for {}",
                                            d.getArtifact(),
                                            d.getException().getMessage());
                                }
                            });
                            throw new IOException("Timeout on waiting for waiting for publishing " + from.name()
                                    + " to " + publishingTarget);
                        }
                        Thread.sleep(waitSleep.toMillis());
                    }
                } catch (NoRepositoryConnectorException e) {
                    logger.info("No connector for publishing target exists; bailing out");
                    throw new MojoFailureException("No publishing target exists");
                } catch (InterruptedException e) {
                    throw new IOException(e.getMessage(), e);
                }
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
