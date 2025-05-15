/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.store;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.store.Layout;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;

/**
 * Layout, that may, but does not have to be used by a store. For inter-store operations a layout should be defined.
 */
public final class DefaultLayout implements Layout {
    public static final String NAME = "default";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String artifactPath(Artifact artifact) {
        requireNonNull(artifact);
        StringBuilder path = new StringBuilder(128);
        path.append(artifact.getGroupId().replace('.', '/')).append('/');
        path.append(artifact.getArtifactId()).append('/');
        path.append(artifact.getBaseVersion()).append('/');
        path.append(artifact.getArtifactId()).append('-').append(artifact.getVersion());
        if (!artifact.getClassifier().isEmpty()) {
            path.append('-').append(artifact.getClassifier());
        }
        if (!artifact.getExtension().isEmpty()) {
            path.append('.').append(artifact.getExtension());
        }
        return path.toString();
    }

    @Override
    public String metadataPath(Metadata metadata) {
        requireNonNull(metadata);
        StringBuilder path = new StringBuilder(128);
        if (!metadata.getGroupId().isEmpty()) {
            path.append(metadata.getGroupId().replace('.', '/')).append('/');
            if (!metadata.getArtifactId().isEmpty()) {
                path.append(metadata.getArtifactId()).append('/');
                if (!metadata.getVersion().isEmpty()) {
                    path.append(metadata.getVersion()).append('/');
                }
            }
        }
        path.append(metadata.getType());
        return path.toString();
    }
}
