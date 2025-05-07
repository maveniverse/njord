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
import eu.maveniverse.maven.njord.shared.publisher.MavenCentralPublisherFactory;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

@Singleton
@Named(SonatypeOSSPublisherFactory.NAME)
public class SonatypeOSSPublisherFactory implements MavenCentralPublisherFactory {
    public static final String NAME = "sonatype-oss";

    private final RepositorySystem repositorySystem;
    private final SonatypeCentralRequirementsFactory centralRequirementsFactory;

    @Inject
    public SonatypeOSSPublisherFactory(
            RepositorySystem repositorySystem, SonatypeCentralRequirementsFactory centralRequirementsFactory) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.centralRequirementsFactory = requireNonNull(centralRequirementsFactory);
    }

    @Override
    public SonatypeNx2Publisher create(SessionConfig sessionConfig) {
        SonatypeOSSPublisherConfig ossConfig = SonatypeOSSPublisherConfig.with(sessionConfig);
        RemoteRepository releasesRepository = new RemoteRepository.Builder(
                        ossConfig.releaseRepositoryId(), "default", ossConfig.releaseRepositoryUrl())
                .build();
        RemoteRepository snapshotsRepository = new RemoteRepository.Builder(
                        ossConfig.snapshotRepositoryId(), "default", ossConfig.snapshotRepositoryUrl())
                .build();

        return new SonatypeNx2Publisher(
                sessionConfig,
                repositorySystem,
                NAME,
                "Publishes to Sonatype OSS",
                CENTRAL,
                snapshotsRepository,
                releasesRepository,
                snapshotsRepository,
                centralRequirementsFactory.create(sessionConfig));
    }
}
