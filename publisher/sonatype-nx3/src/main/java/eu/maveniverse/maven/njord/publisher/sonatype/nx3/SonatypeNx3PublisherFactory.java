/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype.nx3;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactorySupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirementsFactory;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;

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

    @Inject
    public SonatypeNx3PublisherFactory(
            RepositorySystem repositorySystem,
            Map<String, ArtifactStoreRequirementsFactory> artifactStoreRequirementsFactories) {
        super(repositorySystem, artifactStoreRequirementsFactories);
    }

    @Override
    protected ArtifactStorePublisher doCreate(Session session) {
        // Create NXRM3-specific config
        SonatypeNx3PublisherConfig config = new SonatypeNx3PublisherConfig(session.config());
        return new SonatypeNx3Publisher(
                session,
                repositorySystem,
                config.targetReleaseRepository(),
                config.targetSnapshotRepository(),
                config.serviceReleaseRepository(),
                config.serviceSnapshotRepository(),
                config,
                createArtifactStoreRequirements(session, config));
    }
}
