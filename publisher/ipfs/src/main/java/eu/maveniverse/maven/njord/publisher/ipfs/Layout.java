/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.ipfs;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;

/**
 * Helper class to {@link Artifact} instances; provides layout.
 *
 * TODO: reuse resolver layout
 */
final class Layout {
    /**
     * Provides "repository path" for metadata.
     */
    public static String metadataRepositoryPath(Metadata metadata) {
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

    /**
     * Provides "repository path" for artifact.
     */
    public static String artifactRepositoryPath(Artifact artifact) {
        return artifact.getGroupId().replaceAll("\\.", "/") + "/" + artifact.getArtifactId() + "/"
                + artifact.getBaseVersion() + "/" + artifactName(artifact);
    }

    /**
     * Provides "name" for artifact.
     */
    public static String artifactName(Artifact artifact) {
        String name = artifact.getArtifactId() + "-" + artifact.getVersion();
        if (artifact.getClassifier() != null && !artifact.getClassifier().trim().isEmpty()) {
            name += "-" + artifact.getClassifier();
        }
        name += "." + artifact.getExtension();
        return name;
    }
}
