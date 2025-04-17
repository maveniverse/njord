/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.repository;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.factories.ArtifactStoreMergerFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMerger;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;

@Singleton
@Named
public class DefaultArtifactStoreMergerFactory implements ArtifactStoreMergerFactory {
    private final RepositorySystem repositorySystem;

    @Inject
    public DefaultArtifactStoreMergerFactory(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public ArtifactStoreMerger create(SessionConfig sessionConfig) {
        return new DefaultArtifactStoreMerger(sessionConfig, repositorySystem);
    }
}
