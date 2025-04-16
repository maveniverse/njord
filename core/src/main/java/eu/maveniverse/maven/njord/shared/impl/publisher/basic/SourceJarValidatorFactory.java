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
import org.eclipse.aether.util.artifact.SubArtifact;

/**
 * Verifies presence of source JAR for every main JAR artifact.
 */
public class SourceJarValidatorFactory extends ValidatorSupport {
    private static final String JAR = "jar";
    private static final String SOURCES = "sources";

    public SourceJarValidatorFactory(String name) {
        super(name);
    }

    @Override
    public void validate(ArtifactStore artifactStore, Artifact artifact, ValidationResultCollector collector)
            throws IOException {
        if (artifact.getClassifier().isEmpty() && "jar".equals(artifact.getExtension())) {
            HashSet<String> present = new HashSet<>();
            HashSet<String> missing = new HashSet<>();
            Optional<InputStream> c = artifactStore.artifactContent(new SubArtifact(artifact, SOURCES, JAR));
            if (c.isPresent()) {
                present.add(SOURCES);
                c.orElseThrow().close();
            } else {
                missing.add(SOURCES);
            }
            if (!present.isEmpty()) {
                collector.addInfo("PRESENT");
            }
            if (!missing.isEmpty()) {
                collector.addError("MISSING");
            }
        }
    }
}
