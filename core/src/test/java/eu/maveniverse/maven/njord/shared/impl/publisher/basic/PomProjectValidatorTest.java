/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher.basic;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PomProjectValidatorTest extends ValidatorTestSupport {
    @Test
    void pomWithRelocation() throws IOException {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(ContextOverrides.create().build())) {
            Session session = createSession(
                    context,
                    SessionConfig.defaults(context.repositorySystemSession(), context.remoteRepositories())
                            .basedir(cwd())
                            .build());
            ArtifactStore store = artifactStore(ContextOverrides.CENTRAL);
            Artifact artifact = new DefaultArtifact(
                    "io.quarkus:quarkus-hibernate-search-orm-coordination-outbox-polling-deployment:pom:3.23.2");
            TestValidationContext validationContext = new TestValidationContext("test");
            try (PomProjectValidator subject = new PomProjectValidator("test", session)) {
                subject.validate(store, artifact, validationContext);
            }
            // info 5
            // "VALID project/name"
            // "VALID project/description"
            // "VALID project/url"
            // "VALID project/licenses"
            // "VALID project/developers"
            Assertions.assertEquals(0, validationContext.error().size());
            Assertions.assertEquals(5, validationContext.info().size());
        }
    }
}
