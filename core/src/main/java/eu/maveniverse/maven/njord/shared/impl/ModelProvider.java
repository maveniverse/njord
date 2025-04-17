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
import org.apache.maven.model.Model;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

public interface ModelProvider {
    Optional<Model> readEffectiveModel(
            RepositorySystemSession session, Artifact artifact, List<RemoteRepository> remoteRepositories);
}
