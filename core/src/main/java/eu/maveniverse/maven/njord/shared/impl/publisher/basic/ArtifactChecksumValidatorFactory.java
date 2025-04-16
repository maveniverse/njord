/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher.basic;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.publisher.spi.Validator;
import eu.maveniverse.maven.njord.shared.publisher.spi.ValidatorFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmHelper;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.artifact.SubArtifact;

/**
 * Verifies checksum for every artifact.
 */
@Singleton
@Named(ArtifactChecksumValidatorFactory.NAME)
public class ArtifactChecksumValidatorFactory extends ValidatorSupport implements ValidatorFactory {
    public static final String NAME = "artifact-checksum";

    public ArtifactChecksumValidatorFactory() {
        super(NAME, "Artifact Checksum Validator");
    }

    @Override
    public Validator create(RepositorySystemSession session, Config config) {
        return this;
    }

    @Override
    public void validate(ArtifactStore artifactStore, ValidationResultCollector collector) throws IOException {
        for (Artifact artifact : artifactStore.artifacts()) {
            if (artifactStore.omitChecksumsForExtensions().stream()
                    .noneMatch(e -> artifact.getExtension().endsWith(e))) {
                Map<String, String> checksums = ChecksumAlgorithmHelper.calculate(
                        artifact.getFile(), artifactStore.checksumAlgorithmFactories());
                ValidationResultCollector chkCollector = collector.child(ArtifactIdUtils.toId(artifact));
                HashSet<String> algOk = new HashSet<>();
                HashSet<String> algMissing = new HashSet<>();
                HashSet<String> algMismatch = new HashSet<>();
                for (ChecksumAlgorithmFactory algorithmFactory : artifactStore.checksumAlgorithmFactories()) {
                    String calculated = checksums.get(algorithmFactory.getName());
                    String deployed = null;
                    Artifact checksumArtifact = new SubArtifact(
                            artifact, "*", artifact.getExtension() + "." + algorithmFactory.getFileExtension());
                    Optional<InputStream> co = artifactStore.artifactContent(checksumArtifact);
                    if (co.isPresent()) {
                        try (InputStream in = co.orElseThrow()) {
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            in.transferTo(bos);
                            deployed = bos.toString(StandardCharsets.UTF_8);
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
                    chkCollector.addInfo("OK: " + String.join(", ", algOk));
                }
                if (!algMissing.isEmpty()) {
                    chkCollector.addError("MISSING: " + String.join(", ", algMissing));
                }
                if (!algMismatch.isEmpty()) {
                    chkCollector.addError("MISMATCH: " + String.join(", ", algMismatch));
                }
            }
        }
    }

    /**
     * This validator is stateless.
     */
    @Override
    public void close() throws IOException {}
}
