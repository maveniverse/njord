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
import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.factories.ArtifactStoreExporterFactory;
import eu.maveniverse.maven.njord.shared.impl.publisher.ArtifactStorePublisherSupport;
import eu.maveniverse.maven.njord.shared.impl.repository.ArtifactStoreDeployer;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreExporter;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.RemoteRepository;

public class SonatypeCentralPortalPublisher extends ArtifactStorePublisherSupport {
    private final ArtifactStoreExporterFactory artifactStoreExporterFactory;

    public SonatypeCentralPortalPublisher(
            SessionConfig sessionConfig,
            RepositorySystem repositorySystem,
            RemoteRepository releasesRepository,
            RemoteRepository snapshotsRepository,
            ArtifactStoreRequirements artifactStoreRequirements,
            ArtifactStoreExporterFactory artifactStoreExporterFactory) {
        super(
                sessionConfig,
                repositorySystem,
                SonatypeCentralPortalPublisherFactory.NAME,
                "Publishes to Sonatype Central Portal",
                Config.CENTRAL,
                snapshotsRepository,
                releasesRepository,
                snapshotsRepository,
                artifactStoreRequirements);
        this.artifactStoreExporterFactory = requireNonNull(artifactStoreExporterFactory);
    }

    @Override
    public void publish(ArtifactStore artifactStore) throws IOException {
        requireNonNull(artifactStore);
        checkClosed();

        logger.info("Publishing {} to {}", artifactStore, name());

        validateArtifactStore(artifactStore);

        RemoteRepository repository = artifactStore.repositoryMode() == RepositoryMode.RELEASE
                ? serviceReleaseRepository
                : serviceSnapshotRepository;
        if (repository == null) {
            throw new IllegalArgumentException("Repository mode " + artifactStore.repositoryMode()
                    + " not supported; provide RemoteRepository for it");
        }

        if (repository.getPolicy(false).isEnabled()) { // release
            // create ZIP bundle
            Path tmpDir = Files.createTempDirectory(name());
            Path bundle;
            try (ArtifactStoreExporter artifactStoreExporter = artifactStoreExporterFactory.create(config)) {
                bundle = artifactStoreExporter.exportAsBundle(artifactStore, tmpDir);
            }
            if (bundle == null) {
                throw new IllegalStateException("Bundle ZIP was not created");
            }

            // we need to use own HTTP client here
            String authKey = "Authorization";
            String authValue = null;
            try (AuthenticationContext repoAuthContext = AuthenticationContext.forRepository(
                    config.session(), repositorySystem.newDeploymentRepository(config.session(), repository))) {
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
                    throw new IOException("Unexpected response code: " + response.statusCode() + " " + response.body());
                }
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                throw new IOException("Publishing interrupted", e);
            }

            logger.info("Deployment ID: {}", deploymentId);
        } else { // snapshot
            // just deploy to snapshots as m-deploy-p would
            try (ArtifactStore store = artifactStore) {
                new ArtifactStoreDeployer(repositorySystem, config.session(), repository).deploy(store);
            }
        }
    }
}
