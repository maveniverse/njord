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
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JavadocJarValidatorTest extends ValidatorTestSupport {
    @Test
    void jarWithClassesHavingJavadoc() throws IOException {
        Artifact jar = new DefaultArtifact("org.foo:bar:jar:1.0").setFile(withClasses.toFile());
        Artifact javadoc = new DefaultArtifact("org.foo:bar:jar:javadoc:1.0");
        ArtifactStore store = artifactStore(njordRemoteRepository(), jar, javadoc);
        TestValidationContext context = new TestValidationContext("test");
        try (JavadocJarValidator subject = new JavadocJarValidator("test")) {
            subject.validate(store, jar, context);
        }

        // info "present"
        Assertions.assertEquals(0, context.error().size());
        Assertions.assertEquals(1, context.info().size());
    }

    @Test
    void jarWithClassesNotHavingJavadoc() throws IOException {
        Artifact jar = new DefaultArtifact("org.foo:bar:jar:1.0").setFile(withClasses.toFile());
        ArtifactStore store = artifactStore(njordRemoteRepository(), jar);
        TestValidationContext context = new TestValidationContext("test");
        try (JavadocJarValidator subject = new JavadocJarValidator("test")) {
            subject.validate(store, jar, context);
        }

        // error "missing"
        Assertions.assertEquals(1, context.error().size());
        Assertions.assertEquals(0, context.info().size());
    }

    @Test
    void jarWithoutClassesHavingJavadoc() throws IOException {
        Artifact jar = new DefaultArtifact("org.foo:bar:jar:1.0").setFile(withoutClasses.toFile());
        Artifact javadoc = new DefaultArtifact("org.foo:bar:jar:javadoc:1.0");
        ArtifactStore store = artifactStore(njordRemoteRepository(), jar, javadoc);
        TestValidationContext context = new TestValidationContext("test");
        try (JavadocJarValidator subject = new JavadocJarValidator("test")) {
            subject.validate(store, jar, context);
        }

        // info "present"
        Assertions.assertEquals(0, context.error().size());
        Assertions.assertEquals(1, context.info().size());
    }

    @Test
    void jarWithoutClassesNotHavingJavadoc() throws IOException {
        Artifact jar = new DefaultArtifact("org.foo:bar:jar:1.0").setFile(withoutClasses.toFile());
        ArtifactStore store = artifactStore(njordRemoteRepository(), jar);
        TestValidationContext context = new TestValidationContext("test");
        try (JavadocJarValidator subject = new JavadocJarValidator("test")) {
            subject.validate(store, jar, context);
        }

        // nothing
        Assertions.assertEquals(0, context.error().size());
        Assertions.assertEquals(0, context.info().size());
    }

    @Test
    void jarWithClassesHavingJavadocWithJavaSource() throws IOException {
        Artifact jar = new DefaultArtifact("org.foo:bar:jar:1.0").setFile(withClasses.toFile());
        Artifact source = new DefaultArtifact("org.foo:bar:jar:sources:1.0").setFile(withSources.toFile());
        Artifact javadoc = new DefaultArtifact("org.foo:bar:jar:javadoc:1.0");
        ArtifactStore store = artifactStore(njordRemoteRepository(), jar, source, javadoc);
        TestValidationContext context = new TestValidationContext("test");
        try (JavadocJarValidator subject = new JavadocJarValidator("test")) {
            subject.validate(store, jar, context);
        }

        // info "present"
        Assertions.assertEquals(0, context.error().size());
        Assertions.assertEquals(1, context.info().size());
    }

    @Test
    void jarWithClassesNotHavingJavadocWithoutJavaSource() throws IOException {
        Artifact jar = new DefaultArtifact("org.foo:bar:jar:1.0").setFile(withClasses.toFile());
        Artifact source = new DefaultArtifact("org.foo:bar:jar:sources:1.0").setFile(withoutSources.toFile());
        ArtifactStore store = artifactStore(njordRemoteRepository(), jar, source);
        TestValidationContext context = new TestValidationContext("test");
        try (JavadocJarValidator subject = new JavadocJarValidator("test")) {
            subject.validate(store, jar, context);
        }

        // nothing
        Assertions.assertEquals(0, context.error().size());
        Assertions.assertEquals(0, context.info().size());
    }

    @Test
    void jarWithoutClassesHavingJavadocWithJavaSource() throws IOException {
        Artifact jar = new DefaultArtifact("org.foo:bar:jar:1.0").setFile(withoutClasses.toFile());
        Artifact source = new DefaultArtifact("org.foo:bar:jar:sources:1.0").setFile(withSources.toFile());
        Artifact javadoc = new DefaultArtifact("org.foo:bar:jar:javadoc:1.0");
        ArtifactStore store = artifactStore(njordRemoteRepository(), jar, source, javadoc);
        TestValidationContext context = new TestValidationContext("test");
        try (JavadocJarValidator subject = new JavadocJarValidator("test")) {
            subject.validate(store, jar, context);
        }

        // info "present"
        Assertions.assertEquals(0, context.error().size());
        Assertions.assertEquals(1, context.info().size());
    }

    @Test
    void jarWithoutClassesNotHavingJavadocWithoutJavaSource() throws IOException {
        Artifact jar = new DefaultArtifact("org.foo:bar:jar:1.0").setFile(withoutClasses.toFile());
        Artifact source = new DefaultArtifact("org.foo:bar:jar:sources:1.0").setFile(withoutSources.toFile());
        ArtifactStore store = artifactStore(njordRemoteRepository(), jar, source);
        TestValidationContext context = new TestValidationContext("test");
        try (JavadocJarValidator subject = new JavadocJarValidator("test")) {
            subject.validate(store, jar, context);
        }

        // nothing
        Assertions.assertEquals(0, context.error().size());
        Assertions.assertEquals(0, context.info().size());
    }
}
