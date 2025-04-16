/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher.signature;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.impl.publisher.ValidatorSupport;
import eu.maveniverse.maven.njord.shared.publisher.spi.ValidationResultCollector;
import eu.maveniverse.maven.njord.shared.publisher.spi.signature.SignatureValidator;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.artifact.SubArtifact;

/**
 * Pluggable signature validator.
 */
public class ArtifactSignatureValidator extends ValidatorSupport {
    private final List<SignatureValidator> mandatorySignatureValidators;
    private final List<SignatureValidator> optionalSignatureValidators;

    public ArtifactSignatureValidator(
            String name,
            String description,
            List<SignatureValidator> mandatorySignatureValidators,
            List<SignatureValidator> optionalSignatureValidators) {
        super(name, description);
        this.mandatorySignatureValidators = requireNonNull(mandatorySignatureValidators);
        this.optionalSignatureValidators = requireNonNull(optionalSignatureValidators);
    }

    @Override
    public void validate(ArtifactStore artifactStore, ValidationResultCollector collector) throws IOException {
        for (Artifact artifact : artifactStore.artifacts()) {
            if (artifactStore.omitChecksumsForExtensions().stream()
                    .noneMatch(e -> artifact.getExtension().endsWith(e))) {
                ValidationResultCollector chkCollector = collector.child(ArtifactIdUtils.toId(artifact));
                validateSignature(artifactStore, artifact, mandatorySignatureValidators, true, chkCollector);
                validateSignature(artifactStore, artifact, optionalSignatureValidators, false, chkCollector);
            }
        }
    }

    private void validateSignature(
            ArtifactStore artifactStore,
            Artifact artifact,
            Collection<SignatureValidator> signatureValidators,
            boolean mandatory,
            ValidationResultCollector chkCollector)
            throws IOException {
        for (SignatureValidator signatureValidator : signatureValidators) {
            Artifact signature =
                    new SubArtifact(artifact, "*", artifact.getExtension() + "." + signatureValidator.extension());
            Optional<InputStream> so = artifactStore.artifactContent(signature);
            if (so.isPresent()) {
                final InputStream signatureContent;
                try (InputStream in = so.orElseThrow()) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    in.transferTo(bos);
                    signatureContent = new ByteArrayInputStream(bos.toByteArray());
                }
                if (signatureValidator.verifySignature(
                        artifactStore.artifactContent(artifact).orElseThrow(), signatureContent)) {
                    chkCollector.addInfo("VALID " + signatureValidator.name());
                } else {
                    chkCollector.addError("MISMATCH " + signatureValidator.name());
                }
            } else {
                if (mandatory) {
                    chkCollector.addError("MISSING " + signatureValidator.name());
                } else {
                    chkCollector.addInfo("MISSING " + signatureValidator.name());
                }
            }
        }
    }

    @Override
    protected void doClose() throws IOException {
        ArrayList<IOException> exceptions = new ArrayList<>();
        try {
            for (SignatureValidator signatureValidator : mandatorySignatureValidators) {
                signatureValidator.close();
            }
            for (SignatureValidator signatureValidator : optionalSignatureValidators) {
                signatureValidator.close();
            }
        } catch (IOException e) {
            exceptions.add(e);
        }
        if (!exceptions.isEmpty()) {
            IOException e = new IOException("Failed to close validators");
            exceptions.forEach(e::addSuppressed);
            throw e;
        }
    }
}
