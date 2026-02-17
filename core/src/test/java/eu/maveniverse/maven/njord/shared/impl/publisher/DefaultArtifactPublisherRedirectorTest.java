/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DefaultArtifactPublisherRedirectorTest extends PublisherTestSupport {
    @Test
    void smoke() throws IOException {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(ContextOverrides.create()
                .withUserSettings(true)
                .withUserSettingsXmlOverride(
                        Paths.get("src/test/settings/smoke.xml").toAbsolutePath())
                .build())) {
            Session session = createSession(
                    context,
                    SessionConfig.defaults(context.repositorySystemSession(), context.remoteRepositories())
                            .basedir(cwd())
                            .build());
            DefaultArtifactPublisherRedirector subject =
                    new DefaultArtifactPublisherRedirector(session, context.repositorySystem());

            Assertions.assertFalse(subject.getArtifactStorePublisherName().isPresent());
            Assertions.assertTrue(subject.getArtifactStorePublisherName("sonatype-central-portal")
                    .isPresent());
        }
    }

    @Test
    void serviceRedirect() throws IOException {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(ContextOverrides.create()
                .withUserSettings(true)
                .withUserSettingsXmlOverride(
                        Paths.get("src/test/settings/smoke.xml").toAbsolutePath())
                .build())) {
            Session session = createSession(
                    context,
                    SessionConfig.defaults(context.repositorySystemSession(), context.remoteRepositories())
                            .basedir(cwd())
                            .build());
            DefaultArtifactPublisherRedirector subject =
                    new DefaultArtifactPublisherRedirector(session, context.repositorySystem());

            Optional<String> publisher;

            publisher = subject.getArtifactStorePublisherName("sonatype-central-portal");
            Assertions.assertTrue(publisher.isPresent());
            Assertions.assertEquals("sonatype-cp", publisher.get());

            publisher = subject.getArtifactStorePublisherName("some-project-releases");
            Assertions.assertTrue(publisher.isPresent());
            Assertions.assertEquals("sonatype-cp", publisher.get());

            publisher = subject.getArtifactStorePublisherName("just-auth-redirect");
            Assertions.assertTrue(publisher.isPresent());
            Assertions.assertEquals("sonatype-cp", publisher.get());

            Assertions.assertThrows(
                    IllegalArgumentException.class, () -> subject.getArtifactStorePublisherName("unconfigured"));
        }
    }

    @Test
    void unconfiguredPublisherProvidesHelpfulMessage() throws IOException {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(ContextOverrides.create()
                .withUserSettings(true)
                .withUserSettingsXmlOverride(
                        Paths.get("src/test/settings/smoke.xml").toAbsolutePath())
                .build())) {
            Session session = createSession(
                    context,
                    SessionConfig.defaults(context.repositorySystemSession(), context.remoteRepositories())
                            .basedir(cwd())
                            .build());
            DefaultArtifactPublisherRedirector subject =
                    new DefaultArtifactPublisherRedirector(session, context.repositorySystem());

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class, () -> subject.getArtifactStorePublisherName("unconfigured"));

            String message = exception.getMessage();
            // Verify the error message contains helpful information
            Assertions.assertTrue(
                    message.contains("Failed to resolve publisher name"),
                    "Error message should indicate failure to resolve publisher");
            Assertions.assertTrue(message.contains("Check the logs"), "Error message should direct user to check logs");
            Assertions.assertTrue(message.contains("settings.xml"), "Error message should mention settings.xml");
            // Verify the failed name is in the message
            Assertions.assertTrue(
                    message.contains("unconfigured"), "Error message should contain the failed lookup name");
        }
    }

    @Test
    void authRedirect() throws IOException {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(ContextOverrides.create()
                .withUserSettings(true)
                .withUserSettingsXmlOverride(
                        Paths.get("src/test/settings/smoke.xml").toAbsolutePath())
                .build())) {
            Session session = createSession(
                    context,
                    SessionConfig.defaults(context.repositorySystemSession(), context.remoteRepositories())
                            .basedir(cwd())
                            .build());
            DefaultArtifactPublisherRedirector subject =
                    new DefaultArtifactPublisherRedirector(session, context.repositorySystem());

            RemoteRepository authSource;

            // serviceRedirect
            authSource = subject.getAuthRepositoryId(
                    new RemoteRepository.Builder("some-project-releases", "default", "whatever").build());
            Assertions.assertEquals("sonatype-central-portal", authSource.getId());
            Assertions.assertNotNull(authSource.getAuthentication());

            // authRedirect
            authSource = subject.getAuthRepositoryId(
                    new RemoteRepository.Builder("just-auth-redirect", "default", "whatever").build());
            Assertions.assertEquals("sonatype-central-portal", authSource.getId());
            Assertions.assertNotNull(authSource.getAuthentication());

            // unconfigured
            authSource = subject.getAuthRepositoryId(
                    new RemoteRepository.Builder("unconfigured", "default", "whatever").build());
            Assertions.assertEquals("unconfigured", authSource.getId());
            Assertions.assertNull(authSource.getAuthentication());
        }
    }

    @Test
    void redirectUrl() {
        // configured in server configuration
        Map<String, String> effectiveProps = new HashMap<>();
        Map<String, String> serverProps = new HashMap<>();
        serverProps.put("njord.releaseUrl", "serverValue");
        effectiveProps.putAll(serverProps);
        Assertions.assertEquals(
                "serverValue",
                DefaultArtifactPublisherRedirector.getRedirectUrl(
                        effectiveProps, serverProps, RepositoryMode.RELEASE, "my-id", Optional.empty()));
        // configured in project properties
        effectiveProps.put("njord.releaseUrl", "projectValue");
        SessionConfig.CurrentProject project =
                new ProjectWithDistributionManagement("my-id", "releaseDistributionValue", null, null);
        Assertions.assertEquals(
                "projectValue",
                DefaultArtifactPublisherRedirector.getRedirectUrl(
                        effectiveProps, serverProps, RepositoryMode.RELEASE, "my-id", Optional.of(project)));
        // configured also with id suffix
        effectiveProps.put("njord.releaseUrl.my-id", "suffixedValue");
        Assertions.assertEquals(
                "suffixedValue",
                DefaultArtifactPublisherRedirector.getRedirectUrl(
                        effectiveProps, serverProps, RepositoryMode.RELEASE, "my-id", Optional.of(project)));
    }

    @Test
    void redirectUrlReturningNull() {
        // Njord not configured
        Map<String, String> effectiveProps = new HashMap<>();
        Map<String, String> serverProps = new HashMap<>();
        Assertions.assertNull(DefaultArtifactPublisherRedirector.getRedirectUrl(
                effectiveProps, serverProps, RepositoryMode.RELEASE, "my-id", Optional.empty()));
        // different repo id
        effectiveProps.put("njord.releaseUrl", "projectValue");
        Assertions.assertNull(DefaultArtifactPublisherRedirector.getRedirectUrl(
                effectiveProps, serverProps, RepositoryMode.RELEASE, "my-other-id", Optional.empty()));
    }

    static final class ProjectWithDistributionManagement implements SessionConfig.CurrentProject {

        private final Map<RepositoryMode, RemoteRepository> distributionMgmtRepos;

        ProjectWithDistributionManagement(
                String releaseRepoId, String releaseRepoUrl, String snapshotRepoId, String snapshotRepoUrl) {
            distributionMgmtRepos = new EnumMap<>(RepositoryMode.class);
            distributionMgmtRepos.put(
                    RepositoryMode.RELEASE,
                    new RemoteRepository.Builder(releaseRepoId, "default", releaseRepoUrl)
                            .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                            .build());
            distributionMgmtRepos.put(
                    RepositoryMode.SNAPSHOT,
                    new RemoteRepository.Builder(snapshotRepoId, "default", snapshotRepoUrl)
                            .setReleasePolicy(new RepositoryPolicy(false, null, null))
                            .build());
        }

        @Override
        public Artifact artifact() {
            return null;
        }

        @Override
        public Map<String, String> projectProperties() {
            return null;
        }

        @Override
        public List<RemoteRepository> remoteRepositories() {
            return null;
        }

        @Override
        public Map<RepositoryMode, RemoteRepository> distributionManagementRepositories() {
            return distributionMgmtRepos;
        }

        @Override
        public Path buildDirectory() {
            return null;
        }
    }
}
