/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.store;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;

/**
 * Layout, that may, but does not have to be used by a store. For inter-store operations a layout should be defined.
 */
public interface Layout {
    String name();

    String artifactPath(Artifact artifact);

    String metadataPath(Metadata metadata);
}
