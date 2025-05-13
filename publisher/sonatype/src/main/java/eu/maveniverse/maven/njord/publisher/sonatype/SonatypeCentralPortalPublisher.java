/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype;

import static java.util.Objects.requireNonNull;

import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import com.github.mizosoft.methanol.MutableRequest;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.store.ArtifactStoreDeployer;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherSupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.publisher.MavenCentralPublisherFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreWriter;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreWriterFactory;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.RemoteRepository;

public class SonatypeCentralPortalPublisher extends ArtifactStorePublisherSupport {
    private final ArtifactStoreWriter artifactStoreWriter;

    public SonatypeCentralPortalPublisher(
            SessionConfig sessionConfig,
            RepositorySystem repositorySystem,
            RemoteRepository releasesRepository,
            RemoteRepository snapshotsRepository,
            ArtifactStoreRequirements artifactStoreRequirements,
            ArtifactStoreWriterFactory artifactStoreWriterFactory) {
        super(
                sessionConfig,
                repositorySystem,
                SonatypeCentralPortalPublisherFactory.NAME,
                "Publishes to Sonatype Central Portal",
                MavenCentralPublisherFactory.CENTRAL,
                snapshotsRepository,
                releasesRepository,
                snapshotsRepository,
                artifactStoreRequirements);
        this.artifactStoreWriter = requireNonNull(artifactStoreWriterFactory).create(sessionConfig);
    }

    @Override
    protected void doPublish(ArtifactStore artifactStore) throws IOException {
        RemoteRepository repository = selectRemoteRepositoryFor(artifactStore);
        if (sessionConfig.dryRun()) {
            logger.info("Dry run; not publishing to '{}' service at {}", name, repository.getUrl());
            return;
        }
        if (repository.getPolicy(false).isEnabled()) { // release
            // create ZIP bundle
            Path tmpDir = Files.createTempDirectory(name);
            try {
                Path bundle = artifactStoreWriter.writeAsBundle(artifactStore, tmpDir);
                if (bundle == null) {
                    throw new IllegalStateException("Bundle ZIP was not created");
                }

                // handle auth redirect
                RemoteRepository authSource = repository;
                LinkedHashSet<String> authSourcesVisited = new LinkedHashSet<>();
                authSourcesVisited.add(authSource.getId());
                Optional<Map<String, String>> config = sessionConfig.serviceConfiguration(authSource.getId());
                while (config.isPresent()) {
                    String authRedirect = config.orElseThrow().get(SessionConfig.CONFIG_AUTH_REDIRECT);
                    if (authRedirect != null) {
                        authSource = new RemoteRepository.Builder(
                                        authRedirect, authSource.getContentType(), authSource.getUrl())
                                .build();
                        if (!authSourcesVisited.add(authSource.getId())) {
                            throw new IllegalStateException("Auth redirect forms a cycle: " + authSourcesVisited);
                        }
                        config = sessionConfig.serviceConfiguration(authSource.getId());
                    } else {
                        break;
                    }
                }
                // build auth token
                String authKey = "Authorization";
                String authValue = null;
                try (AuthenticationContext repoAuthContext = AuthenticationContext.forRepository(
                        sessionConfig.session(),
                        repositorySystem.newDeploymentRepository(sessionConfig.session(), authSource))) {
                    if (repoAuthContext != null) {
                        String username = repoAuthContext.get(AuthenticationContext.USERNAME);
                        String password = repoAuthContext.get(AuthenticationContext.PASSWORD);
                        authValue = "Bearer "
                                + Base64.getEncoder()
                                        .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
                    }
                }
                if (authValue == null) {
                    throw new IllegalStateException(
                            "No authorization information found for repository " + repository.getId());
                }

                // we need to use own HTTP client here
                String deploymentId;
                try {
                    Methanol httpClient = Methanol.create();
                    MultipartBodyPublisher multipartBodyPublisher = MultipartBodyPublisher.newBuilder()
                            .filePart("bundle", bundle, MediaType.APPLICATION_OCTET_STREAM)
                            .build();
                    HttpResponse<String> response = httpClient.send(
                            MutableRequest.POST(repository.getUrl(), multipartBodyPublisher)
                                    .header(authKey, authValue),
                            HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 201) {
                        deploymentId = response.body();
                    } else {
                        throw new IOException(
                                "Unexpected response code: " + response.statusCode() + " " + response.body());
                    }
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                    throw new IOException("Publishing interrupted", e);
                }

                logger.info("Deployment ID: {}", deploymentId);
            } finally {
                if (Files.isDirectory(tmpDir)) {
                    FileUtils.deleteRecursively(tmpDir);
                }
            }
        } else { // snapshot
            // just deploy to snapshots as m-deploy-p would
            try (ArtifactStore store = artifactStore) {
                new ArtifactStoreDeployer(repositorySystem, sessionConfig.session(), repository).deploy(store);
            }
        }
    }
}
