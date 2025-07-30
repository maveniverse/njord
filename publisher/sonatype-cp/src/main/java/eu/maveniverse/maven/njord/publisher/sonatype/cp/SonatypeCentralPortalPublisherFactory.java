/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype.cp;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.publisher.sonatype.central.SonatypeCentralRequirementsFactory;
import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.MavenCentralPublisherFactory;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

@Singleton
@Named(SonatypeCentralPortalPublisherFactory.NAME)
public class SonatypeCentralPortalPublisherFactory implements MavenCentralPublisherFactory {
    public static final String NAME = "sonatype-cp";

    private final RepositorySystem repositorySystem;
    private final SonatypeCentralRequirementsFactory centralRequirementsFactory;

    @Inject
    public SonatypeCentralPortalPublisherFactory(
            RepositorySystem repositorySystem, SonatypeCentralRequirementsFactory centralRequirementsFactory) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.centralRequirementsFactory = requireNonNull(centralRequirementsFactory);
    }

    @Override
    public ArtifactStorePublisher create(Session session) {
        SonatypeCentralPortalPublisherConfig cpConfig = new SonatypeCentralPortalPublisherConfig(session.config());
        RemoteRepository releasesRepository =
                cpConfig.releaseRepositoryId() != null && cpConfig.releaseRepositoryUrl() != null
                        ? new RemoteRepository.Builder(
                                        cpConfig.releaseRepositoryId(), "default", cpConfig.releaseRepositoryUrl())
                                .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                                .build()
                        : null;
        RemoteRepository snapshotsRepository =
                cpConfig.snapshotRepositoryId() != null && cpConfig.snapshotRepositoryUrl() != null
                        ? new RemoteRepository.Builder(
                                        cpConfig.snapshotRepositoryId(), "default", cpConfig.snapshotRepositoryUrl())
                                .setReleasePolicy(new RepositoryPolicy(false, null, null))
                                .build()
                        : null;

        return new SonatypeCentralPortalPublisher(
                session,
                repositorySystem,
                releasesRepository,
                snapshotsRepository,
                centralRequirementsFactory.create(session),
                cpConfig);
    }
}
