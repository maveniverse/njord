/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

/**
 * Shows publishing status and configuration for given project.
 */
@Mojo(name = "status", threadSafe = true, aggregator = true)
public class StatusMojo extends PublisherSupportMojo {
    @Override
    protected void doWithSession(Session ns) throws IOException, MojoFailureException {
        MavenProject cp = mavenSession.getTopLevelProject();
        if (cp.getDistributionManagement() == null) {
            logger.warn("No distribution management for project {}", cp.getName());
            throw new MojoFailureException("No distribution management found");
        }
        RemoteRepository deploymentRelease = new RemoteRepository.Builder(
                        cp.getDistributionManagement().getRepository().getId(),
                        "default",
                        cp.getDistributionManagement().getRepository().getUrl())
                .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                .build();
        RemoteRepository deploymentSnapshot = new RemoteRepository.Builder(
                        cp.getDistributionManagement().getSnapshotRepository().getId(),
                        "default",
                        cp.getDistributionManagement().getSnapshotRepository().getUrl())
                .setReleasePolicy(new RepositoryPolicy(false, null, null))
                .build();
        String deploymentReleaseUrl =
                ns.artifactPublisherRedirector().getRepositoryUrl(deploymentRelease, RepositoryMode.RELEASE);
        String deploymentSnapshotUrl =
                ns.artifactPublisherRedirector().getRepositoryUrl(deploymentSnapshot, RepositoryMode.SNAPSHOT);

        logger.info("Project deployment:");
        if (ns.config().prefix().isPresent()) {
            logger.info("  Store prefix: {}", ns.config().prefix().orElseThrow(J8Utils.OET));
        }
        logger.info("* Release");
        logger.info("  Repository Id: {}", deploymentRelease.getId());
        RemoteRepository releaseAuthSource =
                ns.artifactPublisherRedirector().getPublishingRepository(deploymentRelease, false);
        if (!Objects.equals(releaseAuthSource.getId(), deploymentRelease.getId())) {
            logger.info("  Auth source Id: {}", releaseAuthSource.getId());
        }
        if (releaseAuthSource.getAuthentication() != null) {
            logger.info("  Repository Auth: Present");
        } else {
            logger.warn("  Repository Auth: Absent");
        }
        logger.info("  POM URL: {}", deploymentRelease.getUrl());
        if (!Objects.equals(deploymentRelease.getUrl(), deploymentReleaseUrl)) {
            logger.info("  Effective URL: {}", deploymentReleaseUrl);
            if (deploymentReleaseUrl.startsWith("njord:")) {
                ArtifactStoreTemplate template =
                        ns.selectSessionArtifactStoreTemplate(deploymentReleaseUrl.substring("njord:".length()));
                printTemplate(template, false);
            }
        }
        logger.info("* Snapshot");
        logger.info("  Repository Id: {}", deploymentSnapshot.getId());
        RemoteRepository snapshotAuthSource =
                ns.artifactPublisherRedirector().getPublishingRepository(deploymentSnapshot, false);
        if (!Objects.equals(snapshotAuthSource.getId(), deploymentSnapshot.getId())) {
            logger.info("  Auth source Id: {}", snapshotAuthSource.getId());
        }
        if (snapshotAuthSource.getAuthentication() != null) {
            logger.info("  Repository Auth: Present");
        } else {
            logger.warn("  Repository Auth: Absent");
        }
        logger.info("  POM URL: {}", deploymentSnapshot.getUrl());
        if (!Objects.equals(deploymentSnapshot.getUrl(), deploymentSnapshotUrl)) {
            logger.info("  Effective URL: {}", deploymentSnapshotUrl);
            if (deploymentSnapshotUrl.startsWith("njord:")) {
                ArtifactStoreTemplate template =
                        ns.selectSessionArtifactStoreTemplate(deploymentSnapshotUrl.substring("njord:".length()));
                printTemplate(template, false);
            }
        }

        logger.info("");

        List<String> storeNameCandidates = getArtifactStoreNameCandidates(ns);
        if (storeNameCandidates.isEmpty()) {
            logger.info("No candidate artifact stores found");
        } else {
            logger.info("Locally staged stores:");
            for (String storeName : storeNameCandidates) {
                Optional<ArtifactStore> aso = ns.artifactStoreManager().selectArtifactStore(storeName);
                if (aso.isPresent()) {
                    try (ArtifactStore artifactStore = aso.orElseThrow(J8Utils.OET)) {
                        logger.info("  {}", artifactStore);
                    }
                }
            }
        }

        logger.info("");

        Optional<String> pno = getArtifactStorePublisherName(ns);
        if (!pno.isPresent()) {
            logger.info("No configured publishers found");
        } else {
            logger.info("Project publishing:");
            String publisherName = pno.orElseThrow(J8Utils.OET);
            Optional<ArtifactStorePublisher> po = ns.selectArtifactStorePublisher(publisherName);
            if (po.isPresent()) {
                printPublisher(po.orElseThrow(J8Utils.OET));
            } else {
                logger.warn("Unknown publisher set: {}", publisherName);
            }
        }

        logger.info("");
    }
}
