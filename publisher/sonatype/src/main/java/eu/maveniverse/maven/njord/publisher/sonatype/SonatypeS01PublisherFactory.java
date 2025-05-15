/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.MavenCentralPublisherFactory;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

@Singleton
@Named(SonatypeS01PublisherFactory.NAME)
public class SonatypeS01PublisherFactory implements MavenCentralPublisherFactory {
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
    public SonatypeNx2Publisher create(Session session) {
        SonatypeS01PublisherConfig s01Config = new SonatypeS01PublisherConfig(session.config());
        RemoteRepository releasesRepository =
                s01Config.releaseRepositoryId() != null && s01Config.releaseRepositoryUrl() != null
                        ? new RemoteRepository.Builder(
                                        s01Config.releaseRepositoryId(), "default", s01Config.releaseRepositoryUrl())
                                .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                                .build()
                        : null;
        RemoteRepository snapshotsRepository =
                s01Config.snapshotRepositoryId() != null && s01Config.snapshotRepositoryUrl() != null
                        ? new RemoteRepository.Builder(
                                        s01Config.snapshotRepositoryId(), "default", s01Config.snapshotRepositoryUrl())
                                .setReleasePolicy(new RepositoryPolicy(false, null, null))
                                .build()
                        : null;

        return new SonatypeNx2Publisher(
                session,
                repositorySystem,
                NAME,
                "Publishes to Sonatype S01",
                CENTRAL,
                snapshotsRepository,
                releasesRepository,
                snapshotsRepository,
                centralRequirementsFactory.create(session));
    }
}
