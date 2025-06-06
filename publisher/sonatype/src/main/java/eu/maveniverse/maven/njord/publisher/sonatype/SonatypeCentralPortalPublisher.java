/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.NjordUtils;
import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.impl.store.ArtifactStoreDeployer;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherSupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.publisher.MavenCentralPublisherFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.ConfigUtils;
import org.json.JSONObject;

public class SonatypeCentralPortalPublisher extends ArtifactStorePublisherSupport {
    private final SonatypeCentralPortalPublisherConfig publisherConfig;

    public SonatypeCentralPortalPublisher(
            Session session,
            RepositorySystem repositorySystem,
            RemoteRepository releasesRepository,
            RemoteRepository snapshotsRepository,
            ArtifactStoreRequirements artifactStoreRequirements,
            SonatypeCentralPortalPublisherConfig publisherConfig) {
        super(
                session,
                repositorySystem,
                SonatypeCentralPortalPublisherFactory.NAME,
                "Publishes to Sonatype Central Portal",
                MavenCentralPublisherFactory.CENTRAL,
                snapshotsRepository,
                releasesRepository,
                snapshotsRepository,
                artifactStoreRequirements);
        this.publisherConfig = requireNonNull(publisherConfig);
    }

    @Override
    protected void doPublish(ArtifactStore artifactStore) throws IOException {
        RemoteRepository repository = selectRemoteRepositoryFor(artifactStore);
        if (session.config().dryRun()) {
            logger.info("Dry run; not publishing to '{}' service at {}", name, repository.getUrl());
            return;
        }
        if (repository.getPolicy(false).isEnabled()) { // release
            // create ZIP bundle
            Path tmpDir = Files.createTempDirectory(name);
            try {
                Path bundle = session.artifactStoreWriter().writeAsBundle(artifactStore, tmpDir);
                if (bundle == null) {
                    throw new IllegalStateException("Bundle ZIP was not created");
                }
                String bundleName = bundle.getFileName().toString();
                if (publisherConfig.bundleName().isPresent()) {
                    bundleName = publisherConfig.bundleName().orElseThrow(J8Utils.OET);
                } else if (artifactStore.originProjectArtifact().isPresent()) {
                    Artifact originProjectArtifact =
                            artifactStore.originProjectArtifact().orElseThrow(J8Utils.OET);
                    bundleName = originProjectArtifact.getArtifactId() + "-" + originProjectArtifact.getVersion();
                }

                // build auth token
                RemoteRepository authSource =
                        session.artifactPublisherRedirector().getAuthRepositoryId(repository);
                String authValue = null;
                try (AuthenticationContext repoAuthContext =
                        AuthenticationContext.forRepository(session.config().session(), authSource)) {
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
                            "No authorization information found for repository " + authSource.getId());
                }

                // we need to use own HTTP client here
                String deploymentId;
                // use Maven UA
                String userAgent = ConfigUtils.getString(
                        session.config().session(),
                        ConfigurationProperties.DEFAULT_USER_AGENT,
                        "aether.connector.userAgent",
                        "aether.transport.http.userAgent");

                try (CloseableHttpClient httpClient =
                        HttpClientBuilder.create().setUserAgent(userAgent).build()) {
                    URIBuilder uriBuilder = new URIBuilder(repository.getUrl());
                    deploymentId = upload(httpClient, uriBuilder, authValue, bundle, bundleName);
                    logger.info("Deployment ID: {}", deploymentId);

                    if (publisherConfig.waitForStates()) {
                        logger.info(
                                "Waiting for states past {}... (poll {}; timeout {}, failed states {})",
                                publisherConfig.waitForStatesWaitStates(),
                                publisherConfig.waitForStatesSleep(),
                                publisherConfig.waitForStatesTimeout(),
                                publisherConfig.waitForStatesFailureStates());
                        Instant waitingUntil = Instant.now().plus(publisherConfig.waitForStatesTimeout());
                        try {
                            String deploymentState = deploymentState(httpClient, uriBuilder, authValue, deploymentId);
                            logger.debug("deploymentState = {}", deploymentState);
                            while (publisherConfig.waitForStatesWaitStates().contains(deploymentState)) {
                                if (Instant.now().isAfter(waitingUntil)) {
                                    throw new IOException(
                                            "Timeout on waiting for validation for deployment " + deploymentId);
                                }
                                Thread.sleep(
                                        publisherConfig.waitForStatesSleep().toMillis());

                                deploymentState = deploymentState(httpClient, uriBuilder, authValue, deploymentId);
                                logger.debug("deploymentState = {}", deploymentState);
                            }

                            if (publisherConfig.waitForStatesFailureStates().contains(deploymentState)) {
                                throw new PublishFailedException("Publishing of deployment " + deploymentId
                                        + " failed; transitioned to failure state: " + deploymentState);
                            } else {
                                logger.info("Publishing of deployment {} succeeded: {}", deploymentId, deploymentState);
                            }
                        } catch (InterruptedException e) {
                            throw new IOException(e.getMessage(), e);
                        }
                    }
                } catch (URISyntaxException e) {
                    throw new IOException(e.getMessage(), e);
                }
            } finally {
                if (Files.isDirectory(tmpDir)) {
                    FileUtils.deleteRecursively(tmpDir);
                }
            }
        } else { // snapshot
            // handle auth redirection, if needed and
            // just deploy to snapshots as m-deploy-p would
            try (ArtifactStore store = artifactStore) {
                new ArtifactStoreDeployer(
                                repositorySystem,
                                new DefaultRepositorySystemSession(
                                                session.config().session())
                                        .setConfigProperty(NjordUtils.RESOLVER_SESSION_CONNECTOR_SKIP, true),
                                session.artifactPublisherRedirector().getPublishingRepository(repository, true),
                                true)
                        .deploy(store);
            }
        }
    }

    private String upload(
            CloseableHttpClient httpClient,
            URIBuilder uriBuilder,
            String authorizationHeader,
            Path bundle,
            String bundleName)
            throws IOException, URISyntaxException {
        uriBuilder.clearParameters();
        uriBuilder.setPath("/api/v1/publisher/upload");
        uriBuilder.addParameter("name", bundleName);
        if (publisherConfig.publishingType().isPresent()) {
            uriBuilder.addParameter(
                    "publishingType",
                    publisherConfig.publishingType().orElseThrow(J8Utils.OET).toUpperCase(Locale.ENGLISH));
        }

        HttpPost post = new HttpPost(uriBuilder.build());
        post.setHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addBinaryBody("bundle", bundle.toFile(), ContentType.DEFAULT_BINARY, bundleName);
        post.setEntity(builder.build());
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            if (response.getStatusLine().getStatusCode() == 201) {
                return EntityUtils.toString(response.getEntity());
            } else {
                throw new IOException("Unexpected response code: " + response.getStatusLine() + " "
                        + (response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : ""));
            }
        }
    }

    private String deploymentState(
            CloseableHttpClient httpClient, URIBuilder uriBuilder, String authorizationHeader, String deploymentId)
            throws IOException, URISyntaxException {
        uriBuilder.clearParameters();
        uriBuilder.setPath("/api/v1/publisher/status");
        uriBuilder.addParameter("id", deploymentId);
        HttpPost post = new HttpPost(uriBuilder.build());
        post.setHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
        post.setHeader(HttpHeaders.ACCEPT, "application/json");
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                return new JSONObject(EntityUtils.toString(response.getEntity()))
                        .optString("deploymentState")
                        .toLowerCase(Locale.ENGLISH);
            } else {
                throw new IOException("Unexpected response code: " + response.getStatusLine() + " "
                        + (response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : ""));
            }
        }
    }
}
