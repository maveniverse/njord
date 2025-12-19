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
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactorySupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirementsFactory;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

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
public class SonatypeNx3PublisherFactory extends ArtifactStorePublisherFactorySupport
        implements ArtifactStorePublisherFactory {
    public static final String NAME = "sonatype-nx3";

    private final RepositorySystem repositorySystem;
    private final Map<String, ArtifactStoreRequirementsFactory> artifactStoreRequirementsFactories;

    @Inject
    public SonatypeNx3PublisherFactory(
            RepositorySystem repositorySystem,
            Map<String, ArtifactStoreRequirementsFactory> artifactStoreRequirementsFactories) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.artifactStoreRequirementsFactories = requireNonNull(artifactStoreRequirementsFactories);
    }

    @Override
    protected ArtifactStorePublisher doCreate(
            Session session, RemoteRepository releasesRepository, RemoteRepository snapshotsRepository) {
        // Create NXRM3-specific config
        SonatypeNx3PublisherConfig config = new SonatypeNx3PublisherConfig(session.config());
        ArtifactStoreRequirements artifactStoreRequirements = ArtifactStoreRequirements.NONE;
        if (!ArtifactStoreRequirements.NONE.name().equals(config.artifactStoreRequirements())) {
            artifactStoreRequirements = artifactStoreRequirementsFactories
                    .get(config.artifactStoreRequirements())
                    .create(session);
        }

        return new SonatypeNx3Publisher(
                session, repositorySystem, releasesRepository, snapshotsRepository, config, artifactStoreRequirements);
    }
}
