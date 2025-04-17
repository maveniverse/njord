/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.publisher.ArtifactStorePublisherSupport;
import eu.maveniverse.maven.njord.shared.impl.repository.ArtifactStoreDeployer;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

public class SonatypeNx2Publisher extends ArtifactStorePublisherSupport {
    public SonatypeNx2Publisher(
            SessionConfig sessionConfig,
            RepositorySystem repositorySystem,
            String serviceName,
            String serviceDescription,
            RemoteRepository targetReleaseRepository,
            RemoteRepository targetSnapshotRepository,
            RemoteRepository serviceReleaseRepository,
            RemoteRepository serviceSnapshotRepository,
            ArtifactStoreRequirements artifactStoreRequirements) {
        super(
                sessionConfig,
                repositorySystem,
                serviceName,
                serviceDescription,
                targetReleaseRepository,
                targetSnapshotRepository,
                serviceReleaseRepository,
                serviceSnapshotRepository,
                artifactStoreRequirements);
    }

    @Override
    protected void doPublish(ArtifactStore artifactStore) throws IOException {
        new ArtifactStoreDeployer(repositorySystem, config.session(), selectRemoteRepositoryFor(artifactStore))
                .deploy(artifactStore);
    }
}
