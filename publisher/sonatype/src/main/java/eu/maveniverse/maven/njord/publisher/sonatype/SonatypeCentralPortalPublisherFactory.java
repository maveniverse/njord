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
import eu.maveniverse.maven.njord.shared.impl.factories.ArtifactStoreExporterFactory;
import eu.maveniverse.maven.njord.shared.impl.publisher.DefaultArtifactStoreValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.basic.ArtifactChecksumValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.basic.PomCoordinatesValidatorFactory;
import eu.maveniverse.maven.njord.shared.impl.publisher.basic.PomProjectValidatorFactory;
import eu.maveniverse.maven.njord.shared.impl.publisher.signature.ArtifactSignatureValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.signature.GpgSignatureValidator;
import eu.maveniverse.maven.njord.shared.impl.publisher.signature.SigstoreSignatureValidator;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.publisher.spi.ValidatorFactory;
import eu.maveniverse.maven.njord.shared.publisher.spi.signature.SignatureType;
import eu.maveniverse.maven.njord.shared.publisher.spi.signature.SignatureValidator;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;

@Singleton
@Named(SonatypeCentralPortalPublisherFactory.NAME)
public class SonatypeCentralPortalPublisherFactory implements ArtifactStorePublisherFactory {
    public static final String NAME = "sonatype-cp";

    private final RepositorySystem repositorySystem;
    private final ArtifactStoreExporterFactory artifactStoreExporterFactory;
    private final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;

    @Inject
    public SonatypeCentralPortalPublisherFactory(
            RepositorySystem repositorySystem,
            ArtifactStoreExporterFactory artifactStoreExporterFactory,
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.artifactStoreExporterFactory = requireNonNull(artifactStoreExporterFactory);
        this.checksumAlgorithmFactorySelector = requireNonNull(checksumAlgorithmFactorySelector);
    }

    @Override
    public ArtifactStorePublisher create(RepositorySystemSession session, Config config) {
        SonatypeCentralPortalPublisherConfig cpConfig = SonatypeCentralPortalPublisherConfig.with(config);
        RemoteRepository releasesRepository = new RemoteRepository.Builder(
                        cpConfig.releaseRepositoryId(), "default", cpConfig.releaseRepositoryUrl())
                .build();
        RemoteRepository snapshotsRepository = new RemoteRepository.Builder(
                        cpConfig.snapshotRepositoryId(), "default", cpConfig.snapshotRepositoryUrl())
                .build();

        List<ChecksumAlgorithmFactory> mandatoryChecksumAlgorithms =
                checksumAlgorithmFactorySelector.selectList(List.of("SHA-1", "MD5"));
        List<ChecksumAlgorithmFactory> optionalChecksumAlgorithms =
                checksumAlgorithmFactorySelector.selectList(List.of("SHA-512", "SHA-256"));
        // TODO: finish this
        List<SignatureValidator> mandatorySignatureValidators = List.of(new GpgSignatureValidator());
        List<SignatureValidator> optionalSignatureValidators = List.of(new SigstoreSignatureValidator());
        List<SignatureType> mandatorySignatureTypes = mandatorySignatureValidators.stream()
                .map(v -> (SignatureType) v)
                .toList();
        List<SignatureType> optionalSignatureTypes =
                optionalSignatureValidators.stream().map(v -> (SignatureType) v).toList();

        ArrayList<ValidatorFactory> validators = new ArrayList<>();
        validators.add((s, c) -> new PomCoordinatesValidatorFactory());
        validators.add((s, c) -> new PomProjectValidatorFactory());
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

        return new SonatypeCentralPortalPublisher(
                config,
                repositorySystem,
                session,
                releasesRepository,
                snapshotsRepository,
                mandatoryChecksumAlgorithms,
                optionalChecksumAlgorithms,
                mandatorySignatureTypes,
                optionalSignatureTypes,
                new DefaultArtifactStoreValidator("central", "Central Requirements", session, config, validators),
                artifactStoreExporterFactory);
    }
}
