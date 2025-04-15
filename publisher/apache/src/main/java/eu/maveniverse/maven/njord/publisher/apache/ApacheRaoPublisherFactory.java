/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.apache;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.publisher.sonatype.SonatypeNx2Publisher;
import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

@Singleton
@Named(ApacheRaoPublisherFactory.NAME)
public class ApacheRaoPublisherFactory implements ArtifactStorePublisherFactory {
    public static final String NAME = "apache-rao";

    private final RepositorySystem repositorySystem;

    @Inject
    public ApacheRaoPublisherFactory(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public SonatypeNx2Publisher create(RepositorySystemSession session, Config config) {
        ApachePublisherConfig raoConfig = ApachePublisherConfig.with(config);
        RemoteRepository releasesRepository = new RemoteRepository.Builder(
                        raoConfig.releaseRepositoryId(), "default", raoConfig.releaseRepositoryUrl())
                .build();
        RemoteRepository snapshotsRepository = new RemoteRepository.Builder(
                        raoConfig.snapshotRepositoryId(), "default", raoConfig.snapshotRepositoryUrl())
                .build();
        return new SonatypeNx2Publisher(
                NAME,
                "Publishes to ASF",
                Config.CENTRAL,
                snapshotsRepository,
                repositorySystem,
                session,
                releasesRepository,
                snapshotsRepository);
    }
}
