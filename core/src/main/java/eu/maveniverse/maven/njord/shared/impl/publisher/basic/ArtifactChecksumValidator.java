/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher.basic;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.impl.publisher.ValidatorSupport;
import eu.maveniverse.maven.njord.shared.publisher.spi.ValidationContext;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmHelper;
import org.eclipse.aether.util.artifact.SubArtifact;

/**
 * Pluggable checksum verifier for artifacts.
 */
public class ArtifactChecksumValidator extends ValidatorSupport {
    private final List<ChecksumAlgorithmFactory> mandatoryChecksums;
    private final List<ChecksumAlgorithmFactory> optionalChecksums;

    public ArtifactChecksumValidator(
            String name,
            List<ChecksumAlgorithmFactory> mandatoryChecksums,
            List<ChecksumAlgorithmFactory> optionalChecksums) {
        super(name);
        this.mandatoryChecksums = requireNonNull(mandatoryChecksums);
        this.optionalChecksums = requireNonNull(optionalChecksums);
    }

    @Override
    public void validate(ArtifactStore artifactStore, Artifact artifact, ValidationContext collector)
            throws IOException {
        List<ChecksumAlgorithmFactory> extraChecksums = artifactStore.checksumAlgorithmFactories().stream()
                .filter(a -> mandatoryChecksums.stream().anyMatch(c -> Objects.equals(a.getName(), c.getName())))
                .filter(a -> optionalChecksums.stream().anyMatch(c -> Objects.equals(a.getName(), c.getName())))
                .collect(Collectors.toList());
        if (artifactStore.omitChecksumsForExtensions().stream()
                .noneMatch(e -> artifact.getExtension().endsWith(e))) {
            validateArtifact(artifactStore, artifact, mandatoryChecksums, true, collector);
            validateArtifact(artifactStore, artifact, optionalChecksums, false, collector);
            validateArtifact(artifactStore, artifact, extraChecksums, false, collector);
        }
    }

    protected void validateArtifact(
            ArtifactStore artifactStore,
            Artifact artifact,
            List<ChecksumAlgorithmFactory> algorithms,
            boolean mandatory,
            ValidationContext chkCollector)
            throws IOException {
        Map<String, String> checksums = ChecksumAlgorithmHelper.calculate(artifact.getFile(), algorithms);
        HashSet<String> algOk = new HashSet<>();
        HashSet<String> algMissing = new HashSet<>();
        HashSet<String> algMismatch = new HashSet<>();
        for (ChecksumAlgorithmFactory algorithmFactory : algorithms) {
            String calculated = checksums.get(algorithmFactory.getName());
            Artifact checksumArtifact =
                    new SubArtifact(artifact, "*", artifact.getExtension() + "." + algorithmFactory.getFileExtension());
            Optional<InputStream> co = artifactStore.artifactContent(checksumArtifact);
            if (co.isPresent()) {
                String deployed;
                try (InputStream in = co.orElseThrow(J8Utils.OET)) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    J8Utils.transferTo(in, bos);
                    deployed = new String(bos.toByteArray(), StandardCharsets.UTF_8);
                }
                if (Objects.equals(calculated, deployed)) {
                    algOk.add(algorithmFactory.getName());
                } else {
                    algMismatch.add(algorithmFactory.getName());
                }
            } else {
                algMissing.add(algorithmFactory.getName());
            }
        }
        if (!algOk.isEmpty()) {
            if (mandatory) {
                chkCollector.addInfo("VALID: " + String.join(", ", algOk));
            } else {
                chkCollector.addInfo("VALID (optional): " + String.join(", ", algOk));
            }
        }
        if (mandatory && !algMissing.isEmpty()) {
            chkCollector.addError("MISSING: " + String.join(", ", algMissing));
        }
        if (!algMismatch.isEmpty()) {
            chkCollector.addError("MISMATCH: " + String.join(", ", algMismatch));
        }
    }

    /**
     * This validator is stateless.
     */
    @Override
    public void close() throws IOException {}
}
