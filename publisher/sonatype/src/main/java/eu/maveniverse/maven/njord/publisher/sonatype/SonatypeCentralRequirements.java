/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.impl.publisher.DefaultArtifactStoreValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.basic.ArtifactChecksumValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.basic.PomCoordinatesValidatorFactory;
import eu.maveniverse.maven.njord.shared.impl.publisher.basic.PomProjectValidatorFactory;
import eu.maveniverse.maven.njord.shared.impl.publisher.basic.SourceAndJavadocValidatorFactory;
import eu.maveniverse.maven.njord.shared.impl.publisher.signature.ArtifactSignatureValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.signature.GpgSignatureValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.signature.SigstoreSignatureValidator;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreValidator;
import eu.maveniverse.maven.njord.shared.publisher.spi.ValidatorFactory;
import eu.maveniverse.maven.njord.shared.publisher.spi.signature.SignatureType;
import eu.maveniverse.maven.njord.shared.publisher.spi.signature.SignatureValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;

public class SonatypeCentralRequirements implements ArtifactStoreRequirements {
    private final List<ChecksumAlgorithmFactory> mandatoryChecksumAlgorithms;
    private final List<ChecksumAlgorithmFactory> optionalChecksumAlgorithms;
    private final List<SignatureType> mandatorySignatureTypes;
    private final List<SignatureType> optionalSignatureTypes;
    private final ArtifactStoreValidator releaseValidator;
    private final ArtifactStoreValidator snapshotValidator;

    public SonatypeCentralRequirements(
            RepositorySystemSession session,
            Config config,
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        requireNonNull(checksumAlgorithmFactorySelector);

        // central requirements
        this.mandatoryChecksumAlgorithms = checksumAlgorithmFactorySelector.selectList(List.of("SHA-1", "MD5"));
        this.optionalChecksumAlgorithms = checksumAlgorithmFactorySelector.selectList(List.of("SHA-512", "SHA-256"));

        List<SignatureValidator> mandatorySignatureValidators = List.of(new GpgSignatureValidator());
        List<SignatureValidator> optionalSignatureValidators = List.of(new SigstoreSignatureValidator());
        this.mandatorySignatureTypes = mandatorySignatureValidators.stream()
                .map(SignatureValidator::type)
                .toList();
        this.optionalSignatureTypes = optionalSignatureValidators.stream()
                .map(SignatureValidator::type)
                .toList();

        ArrayList<ValidatorFactory> validators = new ArrayList<>();
        validators.add((s, c) -> new PomCoordinatesValidatorFactory());
        validators.add((s, c) -> new PomProjectValidatorFactory());
        validators.add((s, c) -> new SourceAndJavadocValidatorFactory());
        validators.add((s, c) -> new ArtifactSignatureValidator(
                "artifact-signature",
                "Central Signature Validator",
                mandatorySignatureValidators,
                optionalSignatureValidators));
        validators.add((s, c) -> new ArtifactChecksumValidator(
                "artifact-checksum",
                "Central Checksum Validator",
                mandatoryChecksumAlgorithms,
                optionalChecksumAlgorithms));

        this.releaseValidator =
                new DefaultArtifactStoreValidator("central", "Central Requirements", session, config, validators);
        this.snapshotValidator = null;
    }

    @Override
    public String name() {
        return SonatypeCentralRequirementsFactory.NAME;
    }

    @Override
    public String description() {
        return "Central Requirements";
    }

    @Override
    public Optional<List<ChecksumAlgorithmFactory>> mandatoryChecksumAlgorithms() {
        return Optional.ofNullable(mandatoryChecksumAlgorithms);
    }

    @Override
    public Optional<List<ChecksumAlgorithmFactory>> optionalChecksumAlgorithms() {
        return Optional.ofNullable(optionalChecksumAlgorithms);
    }

    @Override
    public Optional<List<SignatureType>> mandatorySignatureTypes() {
        return Optional.ofNullable(mandatorySignatureTypes);
    }

    @Override
    public Optional<List<SignatureType>> optionalSignatureTypes() {
        return Optional.ofNullable(optionalSignatureTypes);
    }

    @Override
    public Optional<ArtifactStoreValidator> releaseValidator() {
        return Optional.ofNullable(releaseValidator);
    }

    @Override
    public Optional<ArtifactStoreValidator> snapshotValidator() {
        return Optional.ofNullable(snapshotValidator);
    }
}
