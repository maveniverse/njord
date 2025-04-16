/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher.spi;

import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.Closeable;
import java.io.IOException;
import org.eclipse.aether.artifact.Artifact;

public interface Validator extends Closeable {
    /**
     * Validator name,
     */
    String name();

    /**
     * Performs the validation, if applicable. All the validation actions should be recorded against passed in
     * collector.
     */
    void validate(ArtifactStore artifactStore, Artifact artifact, ValidationContext collector) throws IOException;
}
