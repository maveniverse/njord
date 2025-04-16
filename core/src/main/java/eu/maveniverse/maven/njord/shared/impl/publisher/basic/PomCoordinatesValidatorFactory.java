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

/**
 * Verifies any found POM that its coordinates matches layout.
 */
public class PomCoordinatesValidatorFactory extends ValidatorSupport {
    public static final String NAME = "pom-coordinates";

    public PomCoordinatesValidatorFactory() {
        super(NAME, "POM Coordinates Validator");
    }

    @Override
    public void validate(ArtifactStore artifactStore, ValidationResultCollector collector) throws IOException {
        collector.addWarning("Not implemented yet");
    }

    /**
     * This validator is stateless.
     */
    @Override
    public void close() throws IOException {}
}
