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
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import org.apache.maven.search.api.request.Query;
import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchBackend;
import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchBackendFactory;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

import static org.apache.maven.search.api.request.BooleanQuery.and;
import static org.apache.maven.search.api.request.FieldQuery.fieldQuery;

/**
 * A mojo that waits for all the store contents become available from publishing source. In other words, waits
 * for artifacts become "published".
 * <p>
 * <em>Warning: To use this Mojo, runtime Java 11+ is required.</em>
 */
@Mojo(name = "wait-until-published", threadSafe = true, requiresProject = false, aggregator = true)
public class WaitUntilPublishedMojo extends PublisherSupportMojo {
    /**
     * Whether source store should be dropped after successful operation.
     */
    @Parameter(required = true, property = "drop", defaultValue = "true")
    private boolean drop;

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
                // TODO: ensure it is Central or Nx2 (two supported ones)
                // TODO: total wait time
                // TODO: poll time
                Map<Artifact, Boolean> artifacts =
                        from.artifacts().stream().collect(Collectors.toMap(a -> a, a -> Boolean.FALSE));
                AtomicInteger toCheck = new AtomicInteger(artifacts.size());
                RemoteRepository publishingTarget = pto.orElseThrow(J8Utils.OET);
                logger.info(
                        "Waiting for {} artifacts to become available from {}",
                        artifacts.size(),
                        publishingTarget);
                try (RemoteRepositorySearchBackend rrb =
                        RemoteRepositorySearchBackendFactory.createDefaultMavenCentral()) {
                    while (toCheck.get() > 0) {
                        for (Map.Entry<Artifact, Boolean> e : artifacts.entrySet()) {
                            if (!e.getValue()) {
                                if (available(rrb, e.getKey())) {
                                    toCheck.decrementAndGet();
                                    e.setValue(Boolean.TRUE);
                                }
                            }
                        }
                    }
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

    /**
     * Returns {@code true} if artifact is available on remote repository that backend points at.
     */
    private boolean available(SearchBackend backend, Artifact artifact) throws IOException {
        Query query = toRrQuery(artifact);
        SearchRequest searchRequest = new SearchRequest(query);
        SearchResponse searchResponse = backend.search(searchRequest);
        return searchResponse.getTotalHits() == 1;
    }

    /**
     * Creates query for "existence check".
     */
    private Query toRrQuery(Artifact artifact) {
        Query result = fieldQuery(MAVEN.GROUP_ID, artifact.getGroupId());
        result = and(result, fieldQuery(MAVEN.ARTIFACT_ID, artifact.getArtifactId()));
        result = and(result, fieldQuery(MAVEN.VERSION, artifact.getVersion()));
        if (artifact.getClassifier() != null && !artifact.getClassifier().isEmpty()) {
            result = and(result, fieldQuery(MAVEN.CLASSIFIER, artifact.getClassifier()));
        }
        return and(result, fieldQuery(MAVEN.FILE_EXTENSION, artifact.getExtension()));
    }
}
