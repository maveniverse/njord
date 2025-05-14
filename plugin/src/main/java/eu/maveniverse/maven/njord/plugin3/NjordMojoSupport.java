/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.NjordUtils;
import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.publisher.spi.signature.SignatureType;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import eu.maveniverse.maven.shared.plugin.MojoSupport;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

public abstract class NjordMojoSupport extends MojoSupport {
    @Inject
    protected MavenSession mavenSession;

    @Override
    public void executeMojo() throws MojoExecutionException, MojoFailureException {
        try {
            Optional<Session> njordSession = NjordUtils.mayGetNjordSession(mavenSession.getRepositorySession());
            if (njordSession.isEmpty()) {
                doWithoutSession();
            } else {
                Session ns = njordSession.orElseThrow();
                if (ns.config().enabled()) {
                    doWithSession(ns);
                } else {
                    logger.info("Njord is disabled");
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    protected abstract void doWithSession(Session ns) throws IOException, MojoExecutionException, MojoFailureException;

    protected void doWithoutSession() throws IOException, MojoExecutionException, MojoFailureException {
        throw new MojoExecutionException("Njord extension is not installed");
    }

    protected void printTemplate(ArtifactStoreTemplate template, boolean defaultTemplate) {
        logger.info(
                "- {} {}",
                template.name(),
                defaultTemplate ? " (default " + template.repositoryMode().name() + ")" : " ");
        logger.info("    Default prefix: '{}'", template.prefix());
        logger.info("    Allow redeploy: {}", template.allowRedeploy());
        logger.info(
                "    Checksum Factories: {}",
                template.checksumAlgorithmFactories().isPresent()
                        ? template.checksumAlgorithmFactories().orElseThrow()
                        : "Globally configured");
        logger.info(
                "    Omit checksums for: {}",
                template.omitChecksumsForExtensions().isPresent()
                        ? template.omitChecksumsForExtensions().orElseThrow()
                        : "Globally configured");
    }

    protected void printPublisher(ArtifactStorePublisher publisher) {
        logger.info("- '{}' -> {}", publisher.name(), publisher.description());
        if (publisher.targetReleaseRepository().isPresent()
                || publisher.targetSnapshotRepository().isPresent()) {
            ArtifactStoreRequirements artifactStoreRequirements = publisher.artifactStoreRequirements();
            logger.info("  Checksums:");
            logger.info(
                    "    Mandatory: {}",
                    artifactStoreRequirements.mandatoryChecksumAlgorithms().isEmpty()
                            ? "No checksum requirements set"
                            : artifactStoreRequirements.mandatoryChecksumAlgorithms().orElseThrow().stream()
                                    .map(ChecksumAlgorithmFactory::getName)
                                    .collect(Collectors.joining(", ")));
            logger.info(
                    "    Supported: {}",
                    artifactStoreRequirements.optionalChecksumAlgorithms().isEmpty()
                            ? "No checksum requirements set"
                            : artifactStoreRequirements.optionalChecksumAlgorithms().orElseThrow().stream()
                                    .map(ChecksumAlgorithmFactory::getName)
                                    .collect(Collectors.joining(", ")));
            logger.info("  Signatures:");
            logger.info(
                    "    Mandatory: {}",
                    artifactStoreRequirements.mandatorySignatureTypes().isEmpty()
                            ? "No signature requirements set"
                            : artifactStoreRequirements.mandatorySignatureTypes().orElseThrow().stream()
                                    .map(SignatureType::name)
                                    .collect(Collectors.joining(", ")));
            logger.info(
                    "    Supported: {}",
                    artifactStoreRequirements.optionalSignatureTypes().isEmpty()
                            ? "No signature requirements set"
                            : artifactStoreRequirements.optionalSignatureTypes().orElseThrow().stream()
                                    .map(SignatureType::name)
                                    .collect(Collectors.joining(", ")));
            logger.info("  Published artifacts will be available from:");
            logger.info(
                    "    RELEASES:  {}", fmt(publisher.targetReleaseRepository().orElse(null)));
            logger.info(
                    "    SNAPSHOTS: {}",
                    fmt(publisher.targetSnapshotRepository().orElse(null)));
        }
        logger.info("  Service endpoints:");
        logger.info(
                "    RELEASES:  {}", fmt(publisher.serviceReleaseRepository().orElse(null)));
        logger.info(
                "    SNAPSHOTS: {}", fmt(publisher.serviceSnapshotRepository().orElse(null)));
    }

    private String fmt(RemoteRepository repo) {
        if (repo == null) {
            return "n/a";
        } else {
            return repo.getId() + " @ " + repo.getUrl();
        }
    }
}
