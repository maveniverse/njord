/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype.nx3;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.impl.ResolverUtils;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

/**
 * The "sonatype-nx3" publisher publishes to Nexus Repository 3 using the Components API.
 * <p>
 * Like the "deploy" publisher, it respects the <code>altDeploymentRepository</code> property
 * with the same <code>id::url</code> syntax. If not provided, it falls back to the POM's
 * <code>&lt;distributionManagement&gt;</code> configuration.
 * <p>
 * Example usage:
 * <pre>
 *   $ mvn njord:publish -Dpublisher=sonatype-nx3 \
 *       -DaltDeploymentRepository=my-nexus::https://nexus.example.com \
 *       -Dnjord.publisher.sonatype-nx3.releaseRepositoryName=maven-releases
 * </pre>
 */
@Singleton
@Named(SonatypeNx3PublisherFactory.NAME)
public class SonatypeNx3PublisherFactory implements ArtifactStorePublisherFactory {
    public static final String NAME = "sonatype-nx3";

    private static final String PROP_ALT_DEPLOYMENT_REPOSITORY = "altDeploymentRepository";

    private final RepositorySystem repositorySystem;

    @Inject
    public SonatypeNx3PublisherFactory(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public ArtifactStorePublisher create(Session session) {
        requireNonNull(session);

        RemoteRepository releasesRepository = null;
        RemoteRepository snapshotsRepository = null;

        // Check for altDeploymentRepository first (same as deploy publisher)
        if (session.config().effectiveProperties().containsKey(PROP_ALT_DEPLOYMENT_REPOSITORY)) {
            String altDeploymentRepository =
                    session.config().effectiveProperties().get(PROP_ALT_DEPLOYMENT_REPOSITORY);
            RemoteRepository bare = ResolverUtils.parseRemoteRepositoryString(altDeploymentRepository);
            releasesRepository = new RemoteRepository.Builder(bare)
                    .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                    .build();
            snapshotsRepository = new RemoteRepository.Builder(bare)
                    .setReleasePolicy(new RepositoryPolicy(false, null, null))
                    .build();
        } else if (session.config().currentProject().isPresent()) {
            // Fallback to POM's distributionManagement
            SessionConfig.CurrentProject project =
                    session.config().currentProject().orElseThrow(J8Utils.OET);
            releasesRepository = project.distributionManagementRepositories().get(RepositoryMode.RELEASE);
            snapshotsRepository = project.distributionManagementRepositories().get(RepositoryMode.SNAPSHOT);
        }

        // Create NXRM3-specific config
        SonatypeNx3PublisherConfig config = new SonatypeNx3PublisherConfig(session.config());

        return new SonatypeNx3Publisher(
                session,
                repositorySystem,
                releasesRepository,
                snapshotsRepository,
                config,
                ArtifactStoreRequirements.NONE);
    }
}
