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
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactorySupport;
import eu.maveniverse.maven.njord.shared.publisher.MavenCentralPublisherFactory;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

@Singleton
@Named(SonatypeCentralPortalPublisherFactory.NAME)
public class SonatypeCentralPortalPublisherFactory extends ArtifactStorePublisherFactorySupport
        implements MavenCentralPublisherFactory {
    public static final String NAME = "sonatype-cp";
    public static final String RELEASE_REPOSITORY_ID = "sonatype-cp";
    public static final String RELEASE_REPOSITORY_URL = "https://central.sonatype.com/api/v1/publisher/upload";
    public static final String SNAPSHOT_REPOSITORY_ID = "sonatype-cp";
    public static final String SNAPSHOT_REPOSITORY_URL = "https://central.sonatype.com/repository/maven-snapshots/";

    private final RepositorySystem repositorySystem;
    private final SonatypeCentralRequirementsFactory centralRequirementsFactory;

    @Inject
    public SonatypeCentralPortalPublisherFactory(
            RepositorySystem repositorySystem, SonatypeCentralRequirementsFactory centralRequirementsFactory) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.centralRequirementsFactory = requireNonNull(centralRequirementsFactory);
    }

    @Override
    protected Map<RepositoryMode, RemoteRepository> createRepositories(Session session) {
        HashMap<RepositoryMode, RemoteRepository> result = new HashMap<>();
        result.put(
                RepositoryMode.RELEASE,
                new RemoteRepository.Builder(RELEASE_REPOSITORY_ID, "default", RELEASE_REPOSITORY_URL)
                        .setSnapshotPolicy(new RepositoryPolicy(false, "", ""))
                        .build());
        result.put(
                RepositoryMode.SNAPSHOT,
                new RemoteRepository.Builder(SNAPSHOT_REPOSITORY_ID, "default", SNAPSHOT_REPOSITORY_URL)
                        .setReleasePolicy(new RepositoryPolicy(false, "", ""))
                        .build());
        return result;
    }

    @Override
    protected ArtifactStorePublisher doCreate(
            Session session, RemoteRepository releasesRepository, RemoteRepository snapshotsRepository) {
        SonatypeCentralPortalPublisherConfig cpConfig = new SonatypeCentralPortalPublisherConfig(session.config());
        return new SonatypeCentralPortalPublisher(
                session,
                repositorySystem,
                releasesRepository,
                snapshotsRepository,
                centralRequirementsFactory.create(session),
                cpConfig);
    }
}
