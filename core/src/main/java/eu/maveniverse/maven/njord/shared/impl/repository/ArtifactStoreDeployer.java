/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.repository;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Helper class.
 */
public class ArtifactStoreDeployer {
    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession repositorySystemSession;
    private final RemoteRepository repository;

    public ArtifactStoreDeployer(
            RepositorySystem repositorySystem,
            RepositorySystemSession repositorySystemSession,
            RemoteRepository repository) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.repositorySystemSession = requireNonNull(repositorySystemSession);
        this.repository = requireNonNull(repository);
    }

    public void deploy(ArtifactStore artifactStore) throws IOException {
        deploy(
                artifactStore,
                artifactStore.artifacts().stream()
                        .map(a -> a.setVersion(a.getBaseVersion()))
                        .collect(Collectors.toList()));
    }

    public void deploy(ArtifactStore artifactStore, Collection<Artifact> artifacts) throws IOException {
        DeployRequest deployRequest = new DeployRequest();
        deployRequest.setArtifacts(artifacts);
        deployRequest.setRepository(repositorySystem.newDeploymentRepository(repositorySystemSession, repository));
        deployRequest.setTrace(new RequestTrace(artifactStore));
        try {
            repositorySystem.deploy(repositorySystemSession, deployRequest);
        } catch (DeploymentException e) {
            throw new IOException(e);
        }
    }
}
