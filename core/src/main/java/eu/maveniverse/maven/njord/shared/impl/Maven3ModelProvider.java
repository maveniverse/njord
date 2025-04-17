/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.model.Model;
import org.apache.maven.repository.internal.ArtifactDescriptorReaderDelegate;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

@Singleton
@Named("maven3")
public class Maven3ModelProvider implements ModelProvider {
    private final RepositorySystem repositorySystem;

    @Inject
    public Maven3ModelProvider(RepositorySystem repositorySystem) {
        this.repositorySystem = repositorySystem;
    }

    @Override
    public Optional<Model> readEffectiveModel(
            RepositorySystemSession session, Artifact artifact, List<RemoteRepository> remoteRepositories) {
        AtomicReference<Model> modelRef = new AtomicReference<>(null);
        DefaultRepositorySystemSession ourSession = new DefaultRepositorySystemSession(session);
        ourSession.setConfigProperty(
                ArtifactDescriptorReaderDelegate.class.getName(), new ArtifactDescriptorReaderDelegate() {
                    @Override
                    public void populateResult(
                            RepositorySystemSession session, ArtifactDescriptorResult result, Model model) {
                        modelRef.set(model);
                        super.populateResult(session, result, model);
                    }
                });

        ArtifactDescriptorRequest adr = new ArtifactDescriptorRequest();
        adr.setArtifact(artifact);
        adr.setRepositories(remoteRepositories);
        adr.setRequestContext("njord");
        try {
            repositorySystem.readArtifactDescriptor(ourSession, adr);// ignore result, is in ref
            return Optional.ofNullable(modelRef.get());
        } catch (ArtifactDescriptorException e) {
            return Optional.empty();
        }
    }
}
