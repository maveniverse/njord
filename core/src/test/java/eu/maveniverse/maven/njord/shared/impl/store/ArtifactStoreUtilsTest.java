/*
 * Copyright (c) 2023-2025 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ArtifactStoreUtilsTest {

    @Test
    void smoke() {
        assertThrows(NullPointerException.class, () -> ArtifactStoreUtils.validateArtifactStoreName(null));
        assertThrows(IllegalArgumentException.class, () -> ArtifactStoreUtils.validateArtifactStoreName(""));
        assertThrows(IllegalArgumentException.class, () -> ArtifactStoreUtils.validateArtifactStoreName("  "));
        assertThrows(IllegalArgumentException.class, () -> ArtifactStoreUtils.validateArtifactStoreName("a/b"));
        assertThrows(IllegalArgumentException.class, () -> ArtifactStoreUtils.validateArtifactStoreName("a:b"));
        assertThrows(IllegalArgumentException.class, () -> ArtifactStoreUtils.validateArtifactStoreName("A"));

        assertEquals("some", ArtifactStoreUtils.validateArtifactStoreName("some"));
        assertEquals("some-name", ArtifactStoreUtils.validateArtifactStoreName("some-name"));
        assertEquals("some-name.dot", ArtifactStoreUtils.validateArtifactStoreName("some-name.dot"));
        assertEquals("some_name.dot", ArtifactStoreUtils.validateArtifactStoreName("some_name.dot"));
        assertEquals("some123", ArtifactStoreUtils.validateArtifactStoreName("some123"));
        assertEquals("some-name123", ArtifactStoreUtils.validateArtifactStoreName("some-name123"));
        assertEquals("some-name.dot123", ArtifactStoreUtils.validateArtifactStoreName("some-name.dot123"));
        assertEquals("some_name.dot123", ArtifactStoreUtils.validateArtifactStoreName("some_name.dot123"));
    }
}
