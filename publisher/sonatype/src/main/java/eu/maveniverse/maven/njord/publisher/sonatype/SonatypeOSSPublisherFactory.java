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
    public SonatypeNx2Publisher create(Session session) {
        SonatypeOSSPublisherConfig ossConfig = new SonatypeOSSPublisherConfig(session.config());
        RemoteRepository releasesRepository =
                ossConfig.releaseRepositoryId() != null && ossConfig.releaseRepositoryUrl() != null
                        ? new RemoteRepository.Builder(
                                        ossConfig.releaseRepositoryId(), "default", ossConfig.releaseRepositoryUrl())
                                .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                                .build()
                        : null;
        RemoteRepository snapshotsRepository =
                ossConfig.snapshotRepositoryId() != null && ossConfig.snapshotRepositoryUrl() != null
                        ? new RemoteRepository.Builder(
                                        ossConfig.snapshotRepositoryId(), "default", ossConfig.snapshotRepositoryUrl())
                                .setReleasePolicy(new RepositoryPolicy(false, null, null))
                                .build()
                        : null;

        return new SonatypeNx2Publisher(
                session,
                repositorySystem,
                NAME,
                "Publishes to Sonatype OSS",
                CENTRAL,
                snapshotsRepository,
                releasesRepository,
                snapshotsRepository,
                centralRequirementsFactory.create(session));
    }
}
