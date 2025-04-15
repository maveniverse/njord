/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.store;

import java.io.Closeable;
import java.io.IOException;

public interface ArtifactStoreMerger extends Closeable {
    /**
     * Merges two stores by redeploying source store onto target.
     */
    void redeploy(ArtifactStore source, ArtifactStore target) throws IOException;

    /**
     * Merges two stores by inlining source store onto target.
     */
    void merge(ArtifactStore source, ArtifactStore target) throws IOException;
}
