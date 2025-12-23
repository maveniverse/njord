/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.store;

import static java.util.Objects.requireNonNull;

import java.util.regex.Pattern;

/**
 * Helper class for store related validations and more.
 */
public final class ArtifactStoreUtils {
    private ArtifactStoreUtils() {}

    private static final Pattern FS_FRIENDLY_NAME = Pattern.compile("[a-z0-9-_.]+");

    /**
     * Validates artifact store name.
     */
    public static String validateArtifactStoreName(String name) {
        return validateName(name);
    }

    /**
     * Validates name.
     */
    public static String validateName(String name) {
        requireNonNull(name);
        if (name.trim().isEmpty() || name.contains("..") || !FS_FRIENDLY_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid name");
        }
        return name;
    }
}
