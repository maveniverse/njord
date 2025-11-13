/*
 * Copyright (c) 2023-2025 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.store;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultInternalArtifactStoreManagerTest {

    @Test
    void testNewArtifactStoreNameWithExtendedPrefix() {
        List<Path> paths = Arrays.asList(Paths.get("myPrefix-somethingElse-00001"));
        assertEquals(
                "myPrefix-00001", DefaultInternalArtifactStoreManager.newArtifactStoreName("myPrefix", paths.stream()));
    }

    @Test
    void testNewArtifactStoreNameWithEmptyBasedir() {
        List<Path> paths = Collections.emptyList();
        assertEquals(
                "myPrefix-00001", DefaultInternalArtifactStoreManager.newArtifactStoreName("myPrefix", paths.stream()));
    }

    @Test
    void testNewArtifactStoreNameWithPrefix() {
        List<Path> paths = Arrays.asList(Paths.get("myPrefix-00003"));
        assertEquals(
                "myPrefix-00004", DefaultInternalArtifactStoreManager.newArtifactStoreName("myPrefix", paths.stream()));
    }
}
