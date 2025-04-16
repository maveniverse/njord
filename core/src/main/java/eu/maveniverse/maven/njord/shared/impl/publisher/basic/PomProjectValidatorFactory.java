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
 *  Verifies that any found POM name, description, project URL, SCM and license is filled in.
 */
public class PomProjectValidatorFactory extends ValidatorSupport {
    public static final String NAME = "pom-project";

    public PomProjectValidatorFactory() {
        super(NAME, "POM Project Validator");
    }

    @Override
    public void validate(ArtifactStore artifactStore, ValidationResultCollector collector) throws IOException {
        collector.addWarning("Not implemented yet");
    }
}
