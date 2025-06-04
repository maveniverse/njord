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

public class JavadocJarValidatorTest extends ValidatorTestSupport {
    @Test
    void jarWithClassesHavingJavadoc() throws IOException {
        ArtifactStore store = artifactStoreContaining(
                new DefaultArtifact("org.foo:bar:jar:1.0"), new DefaultArtifact("org.foo:bar:jar:javadoc:1.0"));
        Artifact artifact = new DefaultArtifact("org.foo:bar:jar:1.0")
                .setFile(Paths.get("src/test/binaries/validators/withClasses.jar")
                        .toFile());
        TestValidationContext context = new TestValidationContext("test");
        try (JavadocJarValidator subject = new JavadocJarValidator("test")) {
            subject.validate(store, artifact, context);
        }

        // into "present"
        Assertions.assertEquals(0, context.error().size());
        Assertions.assertEquals(1, context.info().size());
    }

    @Test
    void jarWithClassesNotHavingJavadoc() throws IOException {
        ArtifactStore store = artifactStoreContaining(new DefaultArtifact("org.foo:bar:jar:1.0"));
        Artifact artifact = new DefaultArtifact("org.foo:bar:jar:1.0")
                .setFile(Paths.get("src/test/binaries/validators/withClasses.jar")
                        .toFile());
        TestValidationContext context = new TestValidationContext("test");
        try (JavadocJarValidator subject = new JavadocJarValidator("test")) {
            subject.validate(store, artifact, context);
        }

        // error "missing"
        Assertions.assertEquals(1, context.error().size());
        Assertions.assertEquals(0, context.info().size());
    }

    @Test
    void jarWithoutClassesHavingJavadoc() throws IOException {
        ArtifactStore store = artifactStoreContaining(
                new DefaultArtifact("org.foo:bar:jar:1.0"), new DefaultArtifact("org.foo:bar:jar:javadoc:1.0"));
        Artifact artifact = new DefaultArtifact("org.foo:bar:jar:1.0")
                .setFile(Paths.get("src/test/binaries/validators/withoutClasses.jar")
                        .toFile());
        TestValidationContext context = new TestValidationContext("test");
        try (JavadocJarValidator subject = new JavadocJarValidator("test")) {
            subject.validate(store, artifact, context);
        }

        // info "present"
        Assertions.assertEquals(0, context.error().size());
        Assertions.assertEquals(1, context.info().size());
    }

    @Test
    void jarWithoutClassesNotHavingJavadoc() throws IOException {
        ArtifactStore store = artifactStoreContaining(new DefaultArtifact("org.foo:bar:jar:1.0"));
        Artifact artifact = new DefaultArtifact("org.foo:bar:jar:1.0")
                .setFile(Paths.get("src/test/binaries/validators/withoutClasses.jar")
                        .toFile());
        TestValidationContext context = new TestValidationContext("test");
        try (JavadocJarValidator subject = new JavadocJarValidator("test")) {
            subject.validate(store, artifact, context);
        }

        // nothing
        Assertions.assertEquals(0, context.error().size());
        Assertions.assertEquals(0, context.info().size());
    }
}
