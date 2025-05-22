/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher.signature;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.impl.publisher.ValidatorSupport;
import eu.maveniverse.maven.njord.shared.publisher.spi.ValidationContext;
import eu.maveniverse.maven.njord.shared.publisher.spi.signature.SignatureType;
import eu.maveniverse.maven.njord.shared.publisher.spi.signature.SignatureValidator;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.SubArtifact;

/**
 * Pluggable signature validator.
 */
public class ArtifactSignatureValidator extends ValidatorSupport {
    private final List<SignatureType> mandatorySignatureTypes;
    private final List<SignatureValidator> mandatorySignatureValidators;
    private final List<SignatureType> optionalSignatureTypes;
    private final List<SignatureValidator> optionalSignatureValidators;

    public ArtifactSignatureValidator(
            String name,
            List<SignatureType> mandatorySignatureTypes,
            List<SignatureValidator> mandatorySignatureValidators,
            List<SignatureType> optionalSignatureTypes,
            List<SignatureValidator> optionalSignatureValidators) {
        super(name);
        this.mandatorySignatureTypes = mandatorySignatureTypes;
        this.mandatorySignatureValidators = requireNonNull(mandatorySignatureValidators);
        this.optionalSignatureTypes = optionalSignatureTypes;
        this.optionalSignatureValidators = requireNonNull(optionalSignatureValidators);
    }

    @Override
    public void validate(ArtifactStore artifactStore, Artifact artifact, ValidationContext collector)
            throws IOException {
        if (artifactStore.omitChecksumsForExtensions().stream()
                .noneMatch(e -> artifact.getExtension().endsWith(e))) {
            validateSignature(
                    artifactStore, artifact, mandatorySignatureTypes, mandatorySignatureValidators, true, collector);
            validateSignature(
                    artifactStore, artifact, optionalSignatureTypes, optionalSignatureValidators, false, collector);
        }
    }

    private void validateSignature(
            ArtifactStore artifactStore,
            Artifact artifact,
            Collection<SignatureType> signatureTypes,
            Collection<SignatureValidator> signatureValidators,
            boolean mandatory,
            ValidationContext chkCollector)
            throws IOException {
        for (SignatureType signatureType : signatureTypes) {
            Artifact signature =
                    new SubArtifact(artifact, "*", artifact.getExtension() + "." + signatureType.extension());
            Collection<SignatureValidator> typeSignatureValidator = signatureValidators.stream()
                    .filter(v -> Objects.equals(v.type().name(), signatureType.name()))
                    .collect(Collectors.toList());
            boolean present = artifactStore.artifactPresent(signature);
            // signature validation as well
            if (present) {
                if (typeSignatureValidator.isEmpty()) {
                    chkCollector.addInfo("PRESENT (not validated) " + signatureType.name());
                } else {
                    for (SignatureValidator signatureValidator : typeSignatureValidator) {
                        try (InputStream artifactContent =
                                        artifactStore.artifactContent(artifact).orElseThrow(J8Utils.OET);
                                InputStream signatureContent =
                                        artifactStore.artifactContent(signature).orElseThrow(J8Utils.OET)) {
                            SignatureValidator.Outcome outcome = signatureValidator.verifySignature(
                                    artifactStore,
                                    artifact,
                                    signature,
                                    artifactContent,
                                    signatureContent,
                                    chkCollector);
                            if (outcome == SignatureValidator.Outcome.VALID) {
                                chkCollector.addInfo(
                                        "VALID " + signatureValidator.type().name());
                            } else if (outcome == SignatureValidator.Outcome.INVALID) {
                                chkCollector.addError(
                                        "INVALID " + signatureValidator.type().name());
                            } else {
                                chkCollector.addInfo("PRESENT (not validated by validator) "
                                        + signatureValidator.type().name());
                            }
                        }
                    }
                }
            } else {
                if (mandatory) {
                    chkCollector.addError("MISSING " + signatureType.name());
                } else {
                    chkCollector.addInfo("MISSING (optional) " + signatureType.name());
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
