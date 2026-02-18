/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.store;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.impl.NjordRepositoryListener;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;

/**
 * Helper class, that uses {@link RepositorySystem#install(RepositorySystemSession, InstallRequest)} calls to perform
 * installs "just like maven-install-plugin would".
 */
public class ArtifactStoreInstaller extends ComponentSupport {
    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession repositorySystemSession;
    private final boolean silent;

    public ArtifactStoreInstaller(
            RepositorySystem repositorySystem, RepositorySystemSession repositorySystemSession, boolean silent) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.repositorySystemSession = requireNonNull(repositorySystemSession);
        this.silent = silent;
    }

    /**
     * Installs all artifacts from the store.
     */
    public void install(ArtifactStore artifactStore) throws IOException {
        requireNonNull(artifactStore);
        install(
                artifactStore,
                artifactStore.artifacts().stream()
                        .map(a -> a.setVersion(a.getBaseVersion()))
                        .collect(Collectors.toList()));
    }

    /**
     * Installs given artifacts from the store. This is useful when we need to install only a subset of the store.
     */
    public void install(ArtifactStore artifactStore, Collection<Artifact> artifacts) throws IOException {
        requireNonNull(artifactStore);
        requireNonNull(artifacts);
        InstallRequest installRequest = new InstallRequest();
        installRequest.setArtifacts(artifacts);
        installRequest.setTrace(new RequestTrace(artifactStore));
        try {
            repositorySystem.install(
                    new DefaultRepositorySystemSession(repositorySystemSession)
                            .setTransferListener(null)
                            .setRepositoryListener(new NjordRepositoryListener(silent)),
                    installRequest);
        } catch (InstallationException e) {
            throw new IOException(e);
        }
    }
}
