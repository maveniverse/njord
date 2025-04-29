/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.store;

import java.io.IOException;
import java.util.Collection;

public interface ArtifactStoreComparator {
    /**
     * The comparison result.
     */
    interface ComparisonResult {
        /**
         * Result is equal as long as there is no difference in itself and in any of its children.
         */
        default boolean isEqual() {
            return differences().isEmpty() && children().stream().allMatch(ComparisonResult::isEqual);
        }

        /**
         * Total "comparison actions" count (comparison performed and those left some message).
         */
        default int checksCount() {
            return equalities().size()
                    + differences().size()
                    + children().stream().map(ComparisonResult::checksCount).reduce(0, Integer::sum);
        }

        /**
         * Total difference count (this instance and all children).
         */
        default int differenceCount() {
            return differences().size()
                    + children().stream().map(ComparisonResult::differenceCount).reduce(0, Integer::sum);
        }

        String name();

        Collection<String> equalities();

        Collection<String> differences();

        Collection<ComparisonResult> children();
    }

    /**
     * Comparator name,
     */
    String name();

    /**
     * Comparator description.
     */
    String description();

    /**
     * Performs the comparison.
     */
    ComparisonResult compare(ArtifactStore artifactStore1, ArtifactStore artifactStore2) throws IOException;
}
