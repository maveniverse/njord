/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.impl.ResolverUtils;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

public abstract class ArtifactStorePublisherFactorySupport extends ComponentSupport
        implements ArtifactStorePublisherFactory {
    /**
     * The Maven deploy option to override deployment repository. To be used by those publishers that can
     * support it. It is NOT supported by all publishers.
     */
    protected static final String PROP_ALT_DEPLOYMENT_REPOSITORY = "altDeploymentRepository";

    protected final RepositorySystem repositorySystem;
    protected final Map<String, ArtifactStoreRequirementsFactory> artifactStoreRequirementsFactories;

    protected ArtifactStorePublisherFactorySupport(
            RepositorySystem repositorySystem,
            Map<String, ArtifactStoreRequirementsFactory> artifactStoreRequirementsFactories) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.artifactStoreRequirementsFactories = requireNonNull(artifactStoreRequirementsFactories);
    }

    @Override
    public final ArtifactStorePublisher create(Session session) {
        requireNonNull(session);

        Map<RepositoryMode, RemoteRepository> repositories = createRepositories(session);
        return doCreate(session, repositories.get(RepositoryMode.RELEASE), repositories.get(RepositoryMode.SNAPSHOT));
    }

    /**
     * Publishers working against SaaS would like to override this. By default, this method implements a simple logic
     * to use {@link #PROP_ALT_DEPLOYMENT_REPOSITORY} if defined, or try to grab project distribution management,
     * if project is present.
     */
    protected Map<RepositoryMode, RemoteRepository> createRepositories(Session session) {
        RemoteRepository releaseRepository = null;
        RemoteRepository snapshotRepository = null;
        if (session.config().effectiveProperties().containsKey(PROP_ALT_DEPLOYMENT_REPOSITORY)) {
            String altDeploymentRepository =
                    session.config().effectiveProperties().get(PROP_ALT_DEPLOYMENT_REPOSITORY);
            RemoteRepository bare = ResolverUtils.parseRemoteRepositoryString(altDeploymentRepository);
            releaseRepository = new RemoteRepository.Builder(bare)
                    .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                    .build();
            snapshotRepository = new RemoteRepository.Builder(bare)
                    .setReleasePolicy(new RepositoryPolicy(false, null, null))
                    .build();
        } else if (session.config().currentProject().isPresent()) {
            SessionConfig.CurrentProject project =
                    session.config().currentProject().orElseThrow(J8Utils.OET);
            releaseRepository = project.distributionManagementRepositories().get(RepositoryMode.RELEASE);
            snapshotRepository = project.distributionManagementRepositories().get(RepositoryMode.SNAPSHOT);
        }
        HashMap<RepositoryMode, RemoteRepository> result = new HashMap<>();
        if (releaseRepository != null) {
            result.put(RepositoryMode.RELEASE, releaseRepository);
        }
        if (snapshotRepository != null) {
            result.put(RepositoryMode.SNAPSHOT, snapshotRepository);
        }
        return result;
    }

    protected ArtifactStoreRequirements createArtifactStoreRequirements(
            Session session, PublisherConfigSupport config) {
        ArtifactStoreRequirements artifactStoreRequirements = ArtifactStoreRequirements.NONE;
        if (!ArtifactStoreRequirements.NONE.name().equals(config.artifactStoreRequirements())) {
            artifactStoreRequirements = artifactStoreRequirementsFactories
                    .get(config.artifactStoreRequirements())
                    .create(session);
        }
        return artifactStoreRequirements;
    }

    protected abstract ArtifactStorePublisher doCreate(
            Session session, RemoteRepository releasesRepository, RemoteRepository snapshotsRepository);
}
