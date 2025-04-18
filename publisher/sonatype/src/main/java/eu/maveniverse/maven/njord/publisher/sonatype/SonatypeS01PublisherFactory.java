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
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

@Singleton
@Named(SonatypeS01PublisherFactory.NAME)
public class SonatypeS01PublisherFactory implements ArtifactStorePublisherFactory {
    public static final String NAME = "sonatype-s01";

    private final RepositorySystem repositorySystem;
    private final SonatypeCentralRequirementsFactory centralRequirementsFactory;

    @Inject
    public SonatypeS01PublisherFactory(
            RepositorySystem repositorySystem, SonatypeCentralRequirementsFactory centralRequirementsFactory) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.centralRequirementsFactory = requireNonNull(centralRequirementsFactory);
    }

    @Override
    public SonatypeNx2Publisher create(SessionConfig sessionConfig) {
        SonatypeS01PublisherConfig s01Config = SonatypeS01PublisherConfig.with(sessionConfig.config());
        RemoteRepository releasesRepository = new RemoteRepository.Builder(
                        s01Config.releaseRepositoryId(), "default", s01Config.releaseRepositoryUrl())
                .build();
        RemoteRepository snapshotsRepository = new RemoteRepository.Builder(
                        s01Config.snapshotRepositoryId(), "default", s01Config.snapshotRepositoryUrl())
                .build();

        return new SonatypeNx2Publisher(
                sessionConfig,
                repositorySystem,
                NAME,
                "Publishes to Sonatype s01",
                Config.CENTRAL,
                snapshotsRepository,
                releasesRepository,
                snapshotsRepository,
                centralRequirementsFactory.create(sessionConfig));
    }
}
