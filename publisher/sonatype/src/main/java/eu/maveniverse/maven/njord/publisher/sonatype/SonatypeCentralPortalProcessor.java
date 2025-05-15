/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype;

import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreProcessor;
import eu.maveniverse.maven.njord.shared.store.FilteredArtifactStore;
import java.util.Objects;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.artifact.Artifact;

/**
 * This class is quick-n-dirty circumvention for Maven Central Portal, that craps out on
 * Maven4 build POMs.
 */
@Singleton
@Named(SonatypeCentralPortalProcessor.NAME)
public class SonatypeCentralPortalProcessor implements ArtifactStoreProcessor {
    public static final String NAME = "no-build-pom";

    @Override
    public ArtifactStore process(ArtifactStore artifactStore) {
        return new FilteredArtifactStore(artifactStore, this::isNotMaven4BuildPom, m -> true);
    }

    private boolean isNotMaven4BuildPom(Artifact artifact) {
        return !(Objects.equals("build", artifact.getClassifier())
                && artifact.getExtension().startsWith("pom"));
    }
}
