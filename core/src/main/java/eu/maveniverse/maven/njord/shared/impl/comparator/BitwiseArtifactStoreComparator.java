/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.comparator;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmHelper;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

/**
 * Bitwise comparator.
 * <p>
 * For now a simple one: recalculates SHA-1 of both artifact and compares them, does not rely on repository
 * checksums (see validator for that).
 */
public class BitwiseArtifactStoreComparator extends ArtifactStoreComparatorSupport {
    private static final String SHA1 = "SHA-1";

    private final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;

    public BitwiseArtifactStoreComparator(
            SessionConfig sessionConfig,
            String name,
            String description,
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        super(sessionConfig, name, description);
        this.checksumAlgorithmFactorySelector = requireNonNull(checksumAlgorithmFactorySelector);
    }

    @Override
    protected void doCompare(ComparisonContext comparisonContext, ArtifactStore a1, ArtifactStore a2)
            throws IOException {
        Collection<Artifact> a1artifacts = a1.artifacts();
        Collection<Artifact> a2artifacts = a2.artifacts();

        List<ChecksumAlgorithmFactory> checksumAlgorithmFactories =
                checksumAlgorithmFactorySelector.selectList(List.of(SHA1));

        ComparisonContext context = comparisonContext.child("Bitwise comparison");
        for (Artifact artifact : extractIndex(a1)) {
            if (a1.omitChecksumsForExtensions().stream()
                    .noneMatch(e -> artifact.getExtension().endsWith(e))) {
                String id = ArtifactIdUtils.toId(artifact);
                Artifact artifact1 = a1artifacts.stream()
                        .filter(a -> ArtifactIdUtils.toId(a).equals(id))
                        .findFirst()
                        .orElse(null);
                Artifact artifact2 = a2artifacts.stream()
                        .filter(a -> ArtifactIdUtils.toId(a).equals(id))
                        .findFirst()
                        .orElse(null);
                if (artifact1 != null && artifact2 != null) {
                    ComparisonContext artifactContext = context.child(id);
                    Map<String, String> a1hashes =
                            ChecksumAlgorithmHelper.calculate(artifact1.getFile(), checksumAlgorithmFactories);
                    Map<String, String> a2hashes =
                            ChecksumAlgorithmHelper.calculate(artifact2.getFile(), checksumAlgorithmFactories);
                    if (a1hashes.equals(a2hashes)) {
                        artifactContext.addEquality("Equal: " + a1hashes.get(SHA1));
                    } else {
                        artifactContext.addDifference("Different: " + a1hashes.get(SHA1) + " vs " + a2hashes.get(SHA1));
                    }
                }
            }
        }
    }
}
