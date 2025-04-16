/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher.basic;

import eu.maveniverse.maven.njord.shared.impl.publisher.ValidatorSupport;
import eu.maveniverse.maven.njord.shared.publisher.spi.ValidationResultCollector;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.artifact.SubArtifact;

/**
 * Verifies presence of source and javadoc for every main artifact.
 */
public class SourceAndJavadocValidatorFactory extends ValidatorSupport {
    public static final String NAME = "source-javadoc";

    private static final String JAR = "jar";
    private static final String SOURCES = "sources";
    private static final String JAVADOC = "javadoc";

    public SourceAndJavadocValidatorFactory() {
        super(NAME, "Source and Javadoc presence");
    }

    @Override
    public void validate(ArtifactStore artifactStore, ValidationResultCollector collector) throws IOException {
        for (Artifact artifact : artifactStore.artifacts()) {
            if (artifact.getClassifier().isEmpty() && "jar".equals(artifact.getExtension())) {
                ValidationResultCollector chkCollector = collector.child(ArtifactIdUtils.toId(artifact));
                HashSet<String> present = new HashSet<>();
                HashSet<String> missing = new HashSet<>();
                Optional<InputStream> c = artifactStore.artifactContent(new SubArtifact(artifact, SOURCES, JAR));
                if (c.isPresent()) {
                    present.add(SOURCES);
                    c.orElseThrow().close();
                } else {
                    missing.add(SOURCES);
                }
                c = artifactStore.artifactContent(new SubArtifact(artifact, JAVADOC, JAR));
                if (c.isPresent()) {
                    present.add(JAVADOC);
                    c.orElseThrow().close();
                } else {
                    missing.add(JAVADOC);
                }
                if (!present.isEmpty()) {
                    chkCollector.addInfo("PRESENT: " + String.join(", ", present));
                }
                if (!missing.isEmpty()) {
                    chkCollector.addError("MISSING: " + String.join(", ", missing));
                }
            }
        }
    }
}
