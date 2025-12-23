/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype.nx3;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.extensions.mhc4.impl.MavenHttpClient4FactoryImpl;
import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherSupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.ConfigUtils;

/**
 * Publisher for Nexus Repository 3 using the Components API.
 * <p>
 * This publisher groups artifacts by GAV coordinates and uploads them as complete Maven components
 * using multipart form-data. It supports optional tagging of components.
 */
public class SonatypeNx3Publisher extends ArtifactStorePublisherSupport {
    private final SonatypeNx3PublisherConfig publisherConfig;
    private final MavenHttpClient4FactoryImpl mhc4;

    public SonatypeNx3Publisher(
            Session session,
            RepositorySystem repositorySystem,
            RemoteRepository releasesRepository,
            RemoteRepository snapshotsRepository,
            SonatypeNx3PublisherConfig publisherConfig,
            ArtifactStoreRequirements artifactStoreRequirements) {
        super(
                session,
                repositorySystem,
                SonatypeNx3PublisherFactory.NAME,
                "Publishes to Nexus Repository 3 using Components API",
                releasesRepository,
                snapshotsRepository,
                releasesRepository,
                snapshotsRepository,
                artifactStoreRequirements);
        this.publisherConfig = requireNonNull(publisherConfig);
        this.mhc4 = new MavenHttpClient4FactoryImpl(repositorySystem);
    }

    /**
     * Checks if the NXRM3 server is Pro edition by checking the Server header from the status endpoint.
     * @param httpClient The HTTP client to use
     * @param repository The repository to check
     * @return true if the server is Pro edition, false if OSS/Community
     */
    private boolean isNexusProEdition(CloseableHttpClient httpClient, RemoteRepository repository) {
        try {
            URIBuilder uriBuilder = new URIBuilder(repository.getUrl());
            uriBuilder.setPath("/service/rest/v1/status");
            HttpGet request = new HttpGet(uriBuilder.build());

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                // Check the Server header for edition information
                // OSS/Community: "Nexus/3.x.x (COMMUNITY)" or "Nexus/3.x.x (OSS)"
                // Pro: "Nexus/3.x.x (PRO)"
                if (response.containsHeader("Server")) {
                    String serverHeader = response.getFirstHeader("Server").getValue();
                    logger.debug("Detected Nexus server: {}", serverHeader);
                    // Check if it's Community/OSS edition
                    return !serverHeader.contains("COMMUNITY") && !serverHeader.contains("OSS");
                }
                // If no Server header, assume OSS to be safe
                logger.debug("No Server header found, assuming OSS");
                return false;
            }
        } catch (Exception e) {
            // If we can't check, assume OSS to be safe
            logger.debug("Could not determine Nexus edition, assuming OSS: {}", e.getMessage());
            return false;
        }
    }

    @Override
    protected void doPublish(ArtifactStore artifactStore) throws IOException {
        RemoteRepository repository = selectRemoteRepositoryFor(artifactStore);
        if (repository == null) {
            throw new IllegalStateException("No repository configured for " + artifactStore.repositoryMode() + " mode");
        }

        // Get the NXRM3 repository name
        String repositoryName = artifactStore.repositoryMode() == RepositoryMode.RELEASE
                ? publisherConfig.releaseRepositoryName()
                : publisherConfig.snapshotRepositoryName();

        if (repositoryName == null) {
            throw new IllegalStateException(
                    "No NXRM3 repository name configured for " + artifactStore.repositoryMode() + " mode. "
                            + "Set njord.publisher.sonatype-nx3."
                            + (artifactStore.repositoryMode() == RepositoryMode.RELEASE
                                    ? "releaseRepositoryName"
                                    : "snapshotRepositoryName"));
        }

        if (session.config().dryRun()) {
            logger.info(
                    "Dry run; not publishing to '{}' service at {} (repository: {})",
                    name,
                    repository.getUrl(),
                    repositoryName);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> extraHeaders = (Map<String, String>) ConfigUtils.getMap(
                session.config().session(),
                Collections.emptyMap(),
                ConfigurationProperties.HTTP_HEADERS + "." + repository.getId(),
                ConfigurationProperties.HTTP_HEADERS);

        // Group artifacts by GAV
        Map<String, List<Artifact>> componentGroups = groupArtifactsByGav(artifactStore);

        logger.info(
                "Publishing {} component(s) to NXRM3 repository '{}' at {}",
                componentGroups.size(),
                repositoryName,
                repository.getUrl());

        // Get auth repository for authentication
        RemoteRepository authSource = session.artifactPublisherRedirector().getAuthRepositoryId(repository);

        try (CloseableHttpClient httpClient = mhc4.createDeploymentClient(
                        session.config().session(), repository)
                .build()) {
            // Check if server is Pro edition (for tagging support)
            boolean isProEdition = isNexusProEdition(httpClient, repository);

            // Warn only if tag is explicitly configured but server is OSS
            if (publisherConfig.isTagConfigured() && !isProEdition) {
                logger.warn(
                        "Tag '{}' is configured but Nexus Repository OSS does not support tagging. "
                                + "Tagging is only available in Nexus Repository Pro. The tag will be omitted.",
                        publisherConfig.tag());
            }

            for (Map.Entry<String, List<Artifact>> entry : componentGroups.entrySet()) {
                String gav = entry.getKey();
                List<Artifact> artifacts = entry.getValue();
                uploadComponent(
                        httpClient,
                        repository,
                        authSource,
                        repositoryName,
                        extraHeaders,
                        gav,
                        artifacts,
                        artifactStore,
                        isProEdition);
            }
        }

        logger.info("Successfully published {} component(s)", componentGroups.size());
    }

    /**
     * Groups artifacts by their GAV coordinates (groupId:artifactId:version).
     */
    private Map<String, List<Artifact>> groupArtifactsByGav(ArtifactStore artifactStore) {
        Map<String, List<Artifact>> groups = new LinkedHashMap<>();

        for (Artifact artifact : artifactStore.artifacts()) {
            String gav = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
            groups.computeIfAbsent(gav, k -> new ArrayList<>()).add(artifact);
        }

        return groups;
    }

    /**
     * Uploads a single component (GAV group) to NXRM3 using the Components API.
     */
    private void uploadComponent(
            CloseableHttpClient httpClient,
            RemoteRepository repository,
            RemoteRepository authSource,
            String repositoryName,
            Map<String, String> extraHeaders,
            String gav,
            List<Artifact> artifacts,
            ArtifactStore artifactStore,
            boolean isProEdition)
            throws IOException {
        try {
            URIBuilder uriBuilder = new URIBuilder(repository.getUrl());
            uriBuilder.setPath("/service/rest/v1/components");
            uriBuilder.addParameter("repository", repositoryName);

            HttpPost post = new HttpPost(uriBuilder.build());
            extraHeaders.forEach(post::setHeader);

            // Add authentication if available
            try (AuthenticationContext repoAuthContext =
                    AuthenticationContext.forRepository(session.config().session(), authSource)) {
                if (repoAuthContext != null) {
                    String username = repoAuthContext.get(AuthenticationContext.USERNAME);
                    String password = repoAuthContext.get(AuthenticationContext.PASSWORD);
                    if (username != null && password != null) {
                        String auth = username + ":" + password;
                        String encodedAuth = java.util.Base64.getEncoder()
                                .encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        post.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
                    }
                }
            }

            // Build multipart entity
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

            // Extract GAV components
            String[] gavParts = gav.split(":");
            String groupId = gavParts[0];
            String artifactId = gavParts[1];
            String version = gavParts[2];

            // Add GAV fields
            builder.addTextBody("maven2.groupId", groupId);
            builder.addTextBody("maven2.artifactId", artifactId);
            builder.addTextBody("maven2.version", version);
            builder.addTextBody("maven2.generate-pom", "false");

            // Add tag if configured and server is Pro edition
            if (publisherConfig.tag() != null && isProEdition) {
                builder.addTextBody("tag", publisherConfig.tag());
            }

            // Sort artifacts: POM first, then main artifact, then classified artifacts
            List<Artifact> sortedArtifacts = new ArrayList<>(artifacts);
            sortedArtifacts.sort((a1, a2) -> {
                boolean a1IsPom = a1.getExtension().equals("pom");
                boolean a2IsPom = a2.getExtension().equals("pom");
                if (a1IsPom && !a2IsPom) return -1;
                if (!a1IsPom && a2IsPom) return 1;
                boolean a1HasClassifier =
                        a1.getClassifier() != null && !a1.getClassifier().isEmpty();
                boolean a2HasClassifier =
                        a2.getClassifier() != null && !a2.getClassifier().isEmpty();
                if (!a1HasClassifier && a2HasClassifier) return -1;
                if (a1HasClassifier && !a2HasClassifier) return 1;
                return 0;
            });

            // Add artifacts as assets
            int assetIndex = 1;
            for (Artifact artifact : sortedArtifacts) {
                InputStream content = artifactStore
                        .artifactContent(artifact)
                        .orElseThrow(() -> new IOException("Artifact content not found for " + artifact));

                String assetPrefix = "maven2.asset" + assetIndex;
                String fileName = artifact.getArtifactId() + "-" + artifact.getVersion()
                        + (artifact.getClassifier() != null
                                        && !artifact.getClassifier().isEmpty()
                                ? "-" + artifact.getClassifier()
                                : "")
                        + "." + artifact.getExtension();

                builder.addPart(assetPrefix, new InputStreamBody(content, ContentType.DEFAULT_BINARY, fileName));
                builder.addTextBody(assetPrefix + ".extension", artifact.getExtension());

                if (artifact.getClassifier() != null
                        && !artifact.getClassifier().isEmpty()) {
                    builder.addTextBody(assetPrefix + ".classifier", artifact.getClassifier());
                }

                assetIndex++;
            }

            post.setEntity(builder.build());

            logger.info("Uploading component {} ({} assets)", gav, sortedArtifacts.size());

            try (CloseableHttpResponse response = httpClient.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";

                if (statusCode == 204 || statusCode == 200 || statusCode == 201) {
                    logger.info("Successfully uploaded component {}", gav);
                } else if (statusCode == 403) {
                    throw new IOException("Insufficient permissions to upload to NXRM3 repository '" + repositoryName
                            + "': " + response.getStatusLine() + " " + responseBody);
                } else if (statusCode == 422) {
                    throw new IOException("Invalid component upload (missing parameters?): " + response.getStatusLine()
                            + " " + responseBody);
                } else {
                    throw new IOException(
                            "Failed to upload component " + gav + ": " + response.getStatusLine() + " " + responseBody);
                }
            }
        } catch (URISyntaxException e) {
            throw new IOException("Invalid repository URL: " + repository.getUrl(), e);
        }
    }
}
