/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.publisher.spi.Validator;
import eu.maveniverse.maven.shared.core.component.CloseableSupport;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.SubArtifact;

/**
 * Verifies checksum for every artifact.
 */
public abstract class ValidatorSupport extends CloseableSupport implements Validator {
    protected static final String POM = "pom";
    protected static final String JAR = "jar";
    protected static final String SOURCES = "sources";
    protected static final String JAVADOC = "javadoc";

    private final String name;

    public ValidatorSupport(String name) {
        this.name = requireNonNull(name);
    }

    @Override
    public String name() {
        return name;
    }

    protected boolean mainPom(Artifact artifact) {
        return artifact.getClassifier().isEmpty() && POM.equals(artifact.getExtension());
    }

    protected boolean mainJar(Artifact artifact) {
        return artifact.getClassifier().isEmpty() && JAR.equals(artifact.getExtension());
    }

    protected Artifact sourcesJar(Artifact artifact) {
        return new SubArtifact(artifact, SOURCES, JAR);
    }

    protected Artifact javadocJar(Artifact artifact) {
        return new SubArtifact(artifact, JAVADOC, JAR);
    }

    protected boolean jarContainsJavaClasses(InputStream jarContent) throws IOException {
        try (JarInputStream jarInputStream = new JarInputStream(jarContent)) {
            ZipEntry zipEntry = jarInputStream.getNextEntry();
            while (zipEntry != null) {
                if (zipEntry.getName().endsWith(".class")) {
                    return true;
                }
                zipEntry = jarInputStream.getNextEntry();
            }
        }
        return false;
    }

    protected boolean jarContainsJavaSources(InputStream jarContent) throws IOException {
        try (JarInputStream jarInputStream = new JarInputStream(jarContent)) {
            ZipEntry zipEntry = jarInputStream.getNextEntry();
            while (zipEntry != null) {
                if (zipEntry.getName().endsWith(".java")) {
                    return true;
                }
                zipEntry = jarInputStream.getNextEntry();
            }
        }
        return false;
    }
}
