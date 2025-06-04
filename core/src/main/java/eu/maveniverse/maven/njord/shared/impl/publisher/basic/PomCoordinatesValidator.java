/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher.basic;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.impl.ModelProvider;
import eu.maveniverse.maven.njord.shared.impl.publisher.ValidatorSupport;
import eu.maveniverse.maven.njord.shared.publisher.spi.ValidationContext;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.maven.model.Model;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Verifies any found POM that its coordinates matches layout.
 */
public class PomCoordinatesValidator extends ValidatorSupport {
    private final RepositorySystemSession session;
    private final List<RemoteRepository> remoteRepositories;
    private final ModelProvider modelProvider;

    public PomCoordinatesValidator(
            String name,
            RepositorySystemSession session,
            List<RemoteRepository> repositories,
            ModelProvider modelProvider) {
        super(name);
        this.session = requireNonNull(session);
        this.remoteRepositories = requireNonNull(repositories);
        this.modelProvider = requireNonNull(modelProvider);
    }

    @Override
    public void validate(ArtifactStore artifactStore, Artifact artifact, ValidationContext collector)
            throws IOException {
        if (mainPom(artifact)) {
            ArrayList<RemoteRepository> remoteRepositories = new ArrayList<>(this.remoteRepositories);
            remoteRepositories.add(artifactStore.storeRemoteRepository());
            Optional<Model> mo = modelProvider.readEffectiveModel(session, artifact, remoteRepositories);
            if (mo.isPresent()) {
                Model m = mo.orElseThrow(J8Utils.OET);
                if (Objects.equals(artifact.getGroupId(), m.getGroupId())
                        && Objects.equals(artifact.getArtifactId(), m.getArtifactId())
                        && Objects.equals(artifact.getBaseVersion(), m.getVersion())) {
                    collector.addInfo("VALID");
                } else {
                    collector.addError(String.format(
                            "MISMATCH: %s:%s:%s != %s:%s:%s",
                            artifact.getGroupId(),
                            artifact.getArtifactId(),
                            artifact.getBaseVersion(),
                            m.getGroupId(),
                            m.getArtifactId(),
                            m.getVersion()));
                }
            } else {
                collector.addWarning("Could not get effective model");
            }
        }
    }
}
