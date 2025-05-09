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
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.SessionFactory;
import eu.maveniverse.maven.njord.shared.deploy.ArtifactDeployerRedirector;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.maven.RepositoryUtils;
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
    @Inject
    private ArtifactDeployerRedirector artifactDeployerRedirector;

    @Inject
    private SessionFactory sessionFactory;

    @Override
    protected void doWithoutSession() throws IOException, MojoFailureException {
        logger.warn("Njord extension is not installed; continuing with creating temporary session");
        try (Session ns = NjordUtils.lazyInit(
                SessionConfig.defaults(
                                mavenSession.getRepositorySession(),
                                RepositoryUtils.toRepos(
                                        mavenSession.getRequest().getRemoteRepositories()))
                        .currentProject(SessionConfig.fromMavenProject(mavenSession.getCurrentProject()))
                        .build(),
                sessionFactory::create)) {
            doWithSession(ns);
        }
    }

    @Override
    protected void doWithSession(Session ns) throws IOException, MojoFailureException {
        MavenProject cp = mavenSession.getCurrentProject();
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
                artifactDeployerRedirector.getRepositoryUrl(ns, deploymentRelease, RepositoryMode.RELEASE);
        String deploymentSnapshotUrl =
                artifactDeployerRedirector.getRepositoryUrl(ns, deploymentSnapshot, RepositoryMode.SNAPSHOT);

        logger.info("Project deployment:");
        logger.info("* RELEASE");
        logger.info("  ID: {}", deploymentRelease.getId());
        logger.info("  POM URL: {}", deploymentRelease.getUrl());
        if (!Objects.equals(deploymentRelease.getUrl(), deploymentReleaseUrl)) {
            logger.info("  Effective URL: {}", deploymentReleaseUrl);
            if (deploymentReleaseUrl.startsWith("njord:")) {
                ArtifactStoreTemplate template =
                        ns.selectSessionArtifactStoreTemplate(deploymentReleaseUrl.substring("njord:".length()));
                logger.info("  Template:");
                printTemplate(template, false);
            }
        }
        logger.info("* SNAPSHOT");
        logger.info("  ID: {}", deploymentSnapshot.getId());
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
                    try (ArtifactStore artifactStore = aso.orElseThrow()) {
                        logger.info("  {}", artifactStore);
                    }
                }
            }
        }

        logger.info("");

        Optional<String> pno = getArtifactStorePublisherName(ns);
        if (pno.isEmpty()) {
            logger.info("No configured publishers found");
        } else {
            logger.info("Project publishing:");
            Optional<ArtifactStorePublisher> po = ns.selectArtifactStorePublisher(pno.orElseThrow());
            if (po.isPresent()) {
                printPublisher(po.orElseThrow());
            }
        }

        logger.info("");
    }
}
