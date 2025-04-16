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
import org.eclipse.aether.artifact.Artifact;

/**
 * Verifies any found POM that its coordinates matches layout.
 */
public class PomCoordinatesValidatorFactory extends ValidatorSupport {
    public PomCoordinatesValidatorFactory(String name) {
        super(name);
    }

    @Override
    public void validate(ArtifactStore artifactStore, Artifact artifact, ValidationResultCollector collector)
            throws IOException {
        if (artifact.getClassifier().isEmpty() && "pom".equals(artifact.getExtension())) {
            collector.addInfo("TODO");
        }
    }
}
