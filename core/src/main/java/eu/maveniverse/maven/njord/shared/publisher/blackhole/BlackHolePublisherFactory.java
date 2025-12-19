/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher.blackhole;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactorySupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirementsFactory;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

/**
 * Black hole publisher (or like {@code /dev/null} device) factory.
 */
@Singleton
@Named(BlackHolePublisherFactory.NAME)
public class BlackHolePublisherFactory extends ArtifactStorePublisherFactorySupport
        implements ArtifactStorePublisherFactory {
    public static final String NAME = "black-hole";

    @Inject
    public BlackHolePublisherFactory(
            RepositorySystem repositorySystem,
            Map<String, ArtifactStoreRequirementsFactory> artifactStoreRequirementsFactories) {
        super(repositorySystem, artifactStoreRequirementsFactories);
    }

    @Override
    protected Map<RepositoryMode, RemoteRepository> createRepositories(Session session) {
        HashMap<RepositoryMode, RemoteRepository> result = new HashMap<>();
        result.put(
                RepositoryMode.RELEASE,
                new RemoteRepository.Builder(NAME + "-release", "default", "irrelevant")
                        .setSnapshotPolicy(new RepositoryPolicy(false, "", ""))
                        .build());
        result.put(
                RepositoryMode.SNAPSHOT,
                new RemoteRepository.Builder(NAME + "-snapshot", "default", "irrelevant")
                        .setReleasePolicy(new RepositoryPolicy(false, "", ""))
                        .build());
        return result;
    }

    @Override
    protected ArtifactStorePublisher doCreate(
            Session session, RemoteRepository releasesRepository, RemoteRepository snapshotsRepository) {
        BlackHolePublisherConfig config = new BlackHolePublisherConfig(session.config());

        return new BlackHolePublisher(
                session,
                repositorySystem,
                releasesRepository,
                snapshotsRepository,
                createArtifactStoreRequirements(session, config),
                config.fail());
    }
}
