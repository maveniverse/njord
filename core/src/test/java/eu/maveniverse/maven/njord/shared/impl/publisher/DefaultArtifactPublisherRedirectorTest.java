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
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import org.eclipse.aether.repository.RemoteRepository;
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
}
