/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.comparator;

/**
 * Comparison result collector.
 */
public interface ComparisonContext {
    /**
     * Records an info message and returns {@code this} instance.
     */
    ComparisonContext addEquality(String msg);

    /**
     * Records an warning message and returns {@code this} instance.
     */
    ComparisonContext addDifference(String msg);

    /**
     * Creates child collector and returns newly created instance.
     */
    ComparisonContext child(String name);
}
