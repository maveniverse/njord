/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher.basic;

import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.nio.file.Paths;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SourcesJarValidatorTest extends ValidatorTestSupport {

    @Test
    void jarWithClassesHavingSources() throws IOException {
        Artifact jar = new DefaultArtifact("org.foo:bar:jar:1.0").setFile(withClasses.toFile());
        Artifact sources = new DefaultArtifact("org.foo:bar:jar:sources:1.0").setFile(withSources.toFile());
        ArtifactStore store = artifactStore(njordRemoteRepository(), jar, sources);
        ValidatorTestSupport.TestValidationContext context = new ValidatorTestSupport.TestValidationContext("test");
        try (SourcesJarValidator subject = new SourcesJarValidator("test")) {
            subject.validate(store, jar, context);
        }

        // info "present"
        Assertions.assertEquals(0, context.error().size());
        Assertions.assertEquals(1, context.info().size());
    }

    @Test
    void jarWithClassesNotHavingSources() throws IOException {
        Artifact jar = new DefaultArtifact("org.foo:bar:jar:1.0").setFile(withClasses.toFile());
        ArtifactStore store = artifactStore(njordRemoteRepository(), jar);
        ValidatorTestSupport.TestValidationContext context = new ValidatorTestSupport.TestValidationContext("test");
        try (SourcesJarValidator subject = new SourcesJarValidator("test")) {
            subject.validate(store, jar, context);
        }

        // error "missing"
        Assertions.assertEquals(1, context.error().size());
        Assertions.assertEquals(0, context.info().size());
    }

    @Test
    void jarWithoutClassesHavingSources() throws IOException {
        Artifact jar = new DefaultArtifact("org.foo:bar:jar:1.0").setFile(withoutClasses.toFile());
        Artifact sources = new DefaultArtifact("org.foo:bar:jar:sources:1.0").setFile(withSources.toFile());
        ArtifactStore store = artifactStore(njordRemoteRepository(), jar, sources);
        Artifact artifact = new DefaultArtifact("org.foo:bar:jar:1.0")
                .setFile(Paths.get("src/test/binaries/validators/withoutClasses.jar")
                        .toFile());
        ValidatorTestSupport.TestValidationContext context = new ValidatorTestSupport.TestValidationContext("test");
        try (SourcesJarValidator subject = new SourcesJarValidator("test")) {
            subject.validate(store, artifact, context);
        }

        // info "present"
        Assertions.assertEquals(0, context.error().size());
        Assertions.assertEquals(1, context.info().size());
    }

    @Test
    void jarWithoutClassesNotHavingSources() throws IOException {
        Artifact jar = new DefaultArtifact("org.foo:bar:jar:1.0").setFile(withoutClasses.toFile());
        ArtifactStore store = artifactStore(njordRemoteRepository(), jar);
        ValidatorTestSupport.TestValidationContext context = new ValidatorTestSupport.TestValidationContext("test");
        try (SourcesJarValidator subject = new SourcesJarValidator("test")) {
            subject.validate(store, jar, context);
        }

        // nothing
        Assertions.assertEquals(0, context.error().size());
        Assertions.assertEquals(0, context.info().size());
    }
}
