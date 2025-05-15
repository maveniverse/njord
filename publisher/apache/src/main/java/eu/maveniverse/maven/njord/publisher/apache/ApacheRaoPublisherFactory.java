/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.apache;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.publisher.sonatype.SonatypeCentralRequirementsFactory;
import eu.maveniverse.maven.njord.publisher.sonatype.SonatypeNx2Publisher;
import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.MavenCentralPublisherFactory;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

@Singleton
@Named(ApacheRaoPublisherFactory.NAME)
public class ApacheRaoPublisherFactory implements MavenCentralPublisherFactory {
    public static final String NAME = "apache-rao";

    private final RepositorySystem repositorySystem;
    private final SonatypeCentralRequirementsFactory centralRequirementsFactory;

    @Inject
    public ApacheRaoPublisherFactory(
            RepositorySystem repositorySystem, SonatypeCentralRequirementsFactory centralRequirementsFactory) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.centralRequirementsFactory = requireNonNull(centralRequirementsFactory);
    }

    @Override
    public SonatypeNx2Publisher create(Session session) {
        ApachePublisherConfig raoConfig = new ApachePublisherConfig(session.config());
        RemoteRepository releasesRepository =
                raoConfig.releaseRepositoryId() != null && raoConfig.releaseRepositoryUrl() != null
                        ? new RemoteRepository.Builder(
                                        raoConfig.releaseRepositoryId(), "default", raoConfig.releaseRepositoryUrl())
                                .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                                .build()
                        : null;
        RemoteRepository snapshotsRepository =
                raoConfig.snapshotRepositoryId() != null && raoConfig.snapshotRepositoryUrl() != null
                        ? new RemoteRepository.Builder(
                                        raoConfig.snapshotRepositoryId(), "default", raoConfig.snapshotRepositoryUrl())
                                .setReleasePolicy(new RepositoryPolicy(false, null, null))
                                .build()
                        : null;

        return new SonatypeNx2Publisher(
                session,
                repositorySystem,
                NAME,
                "Publishes to ASF RAO",
                CENTRAL,
                snapshotsRepository,
                releasesRepository,
                snapshotsRepository,
                centralRequirementsFactory.create(session));
    }
}
