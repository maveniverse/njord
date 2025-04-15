/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher.spi;

import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;

public interface Validator {
    /**
     * Validation result collector.
     */
    interface ValidationResultCollector {
        /**
         * Records an info message and returns {@code this} instance.
         */
        ValidationResultCollector addInfo(String msg);

        /**
         * Records an warning message and returns {@code this} instance.
         */
        ValidationResultCollector addWarning(String msg);

        /**
         * Records an error message and returns {@code this} instance.
         */
        ValidationResultCollector addError(String msg);

        /**
         * Creates child collector and returns newly created instance.
         */
        ValidationResultCollector child(String name);
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
    void validate(ArtifactStore artifactStore, ValidationResultCollector collector) throws IOException;
}
