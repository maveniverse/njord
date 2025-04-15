/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher;

import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.util.Collection;

public interface ArtifactStoreValidator {
    /**
     * The validation result. By default, result is valid as long as there are no errors.
     */
    interface ValidationResult {
        /**
         * Result is valid as long as there is no error in itself and in any of its children.
         */
        default boolean isValid() {
            return error().isEmpty() && children().stream().allMatch(ValidationResult::isValid);
        }

        String name();

        Collection<String> info();

        Collection<String> warning();

        Collection<String> error();

        Collection<ValidationResult> children();
    }

    /**
     * Validator name,
     */
    String name();

    /**
     * Validator description.
     */
    String description();

    /**
     * Performs the validation.
     */
    ValidationResult validate(ArtifactStore artifactStore) throws IOException;
}
