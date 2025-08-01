/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype.central;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.impl.publisher.DefaultArtifactStoreValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.basic.ArchiveValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.basic.ArtifactChecksumValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.basic.JavadocJarValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.basic.PomCoordinatesValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.basic.PomProjectValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.basic.SourcesJarValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.signature.ArtifactSignatureValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.signature.GpgSignatureType;
import eu.maveniverse.maven.njord.shared.impl.publisher.signature.GpgSignatureValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.signature.SigstoreSignatureType;
import eu.maveniverse.maven.njord.shared.impl.publisher.signature.SigstoreSignatureValidator;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreValidator;
import eu.maveniverse.maven.njord.shared.publisher.spi.ValidatorFactory;
import eu.maveniverse.maven.njord.shared.publisher.spi.signature.SignatureType;
import eu.maveniverse.maven.njord.shared.publisher.spi.signature.SignatureValidator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
            Session session, ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        requireNonNull(session);
        requireNonNull(checksumAlgorithmFactorySelector);

        // checksums
        this.mandatoryChecksumAlgorithms = checksumAlgorithmFactorySelector.selectList(Arrays.asList("SHA-1", "MD5"));
        this.optionalChecksumAlgorithms =
                checksumAlgorithmFactorySelector.selectList(Arrays.asList("SHA-512", "SHA-256"));

        // signatures
        this.mandatorySignatureTypes = Collections.singletonList(new GpgSignatureType());
        this.optionalSignatureTypes = Collections.singletonList(new SigstoreSignatureType());
        List<SignatureValidator> mandatorySignatureValidators = Collections.singletonList(new GpgSignatureValidator());
        List<SignatureValidator> optionalSignatureValidators =
                Collections.singletonList(new SigstoreSignatureValidator());

        // rest
        ArrayList<ValidatorFactory> validators = new ArrayList<>();
        validators.add((sc) -> new PomCoordinatesValidator("POM Coordinates", session));
        validators.add((sc) -> new PomProjectValidator("POM Completeness", session));
        validators.add((sc) -> new JavadocJarValidator("Javadoc Jar"));
        validators.add((sc) -> new SourcesJarValidator("Source Jar"));
        validators.add((sc) -> new ArchiveValidator("Archive"));
        validators.add((sc) -> new ArtifactChecksumValidator(
                "Checksum Validation", mandatoryChecksumAlgorithms, optionalChecksumAlgorithms));
        validators.add((sc) -> new ArtifactSignatureValidator(
                "Signature Validation",
                mandatorySignatureTypes,
                mandatorySignatureValidators,
                optionalSignatureTypes,
                optionalSignatureValidators));

        this.releaseValidator = new DefaultArtifactStoreValidator(
                session, "central", "Central Requirements", Collections.emptyList(), validators);
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
