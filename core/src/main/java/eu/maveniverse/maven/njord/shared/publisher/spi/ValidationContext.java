/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher.spi;

/**
 * Validation result collector.
 */
public interface ValidationContext {
    /**
     * Records an info message and returns {@code this} instance.
     */
    ValidationContext addInfo(String msg);

    /**
     * Records an warning message and returns {@code this} instance.
     */
    ValidationContext addWarning(String msg);

    /**
     * Records an error message and returns {@code this} instance.
     */
    ValidationContext addError(String msg);

    /**
     * Creates child collector and returns newly created instance.
     */
    ValidationContext child(String name);
}
