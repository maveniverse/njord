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
     * Merges two stores by redeploying source store onto target. This operation behaves exactly as one would have
     * one remote hosted repository with contents of {@code target} and use Maven to deploy contents of {@code source}
     * on it. Target artifact store settings like mode and allow to redeploy (overwrite existing artifact; applies only
     * to release repositories as snapshots never overwrite each other) are obeyed.
     * <p>
     * On successful return from this method, both stores are closed.
     */
    void redeploy(ArtifactStore source, ArtifactStore target) throws IOException;

    /**
     * Merges two <em>release</em> stores by inlining source store onto target. This operation handles two cases:
     * First, an artifact from source, if present in target, must be exactly (checksum-wise) same as from source.
     * In this case the source artifact is silently discarded (as already exist in target). The second case is
     * that artifact from source does not exist in target at all, in which case it is written to target. Any other
     * case results in error.
     * <p>
     * On successful return from this method, both stores are closed.
     */
    void merge(ArtifactStore source, ArtifactStore target) throws IOException;
}
