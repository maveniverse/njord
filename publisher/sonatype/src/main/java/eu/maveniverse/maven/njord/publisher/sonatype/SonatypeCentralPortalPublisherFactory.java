/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.factories.ArtifactStoreExporterFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

@Singleton
@Named(SonatypeCentralPortalPublisherFactory.NAME)
public class SonatypeCentralPortalPublisherFactory implements ArtifactStorePublisherFactory {
    public static final String NAME = "sonatype-cp";

    private final RepositorySystem repositorySystem;
    private final ArtifactStoreExporterFactory artifactStoreExporterFactory;
    private final SonatypeCentralRequirementsFactory centralRequirementsFactory;

    @Inject
    public SonatypeCentralPortalPublisherFactory(
            RepositorySystem repositorySystem,
            ArtifactStoreExporterFactory artifactStoreExporterFactory,
            SonatypeCentralRequirementsFactory centralRequirementsFactory) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.artifactStoreExporterFactory = requireNonNull(artifactStoreExporterFactory);
        this.centralRequirementsFactory = requireNonNull(centralRequirementsFactory);
    }

    @Override
    public ArtifactStorePublisher create(SessionConfig sessionConfig) {
        SonatypeCentralPortalPublisherConfig cpConfig =
                SonatypeCentralPortalPublisherConfig.with(sessionConfig.config());
        RemoteRepository releasesRepository = new RemoteRepository.Builder(
                        cpConfig.releaseRepositoryId(), "default", cpConfig.releaseRepositoryUrl())
                .build();
        RemoteRepository snapshotsRepository = new RemoteRepository.Builder(
                        cpConfig.snapshotRepositoryId(), "default", cpConfig.snapshotRepositoryUrl())
                .build();

        return new SonatypeCentralPortalPublisher(
                sessionConfig,
                repositorySystem,
                releasesRepository,
                snapshotsRepository,
                centralRequirementsFactory.create(sessionConfig),
                artifactStoreExporterFactory);
    }
}
