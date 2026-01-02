/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.util.Map;
import org.eclipse.aether.RepositorySystem;

public abstract class ArtifactStorePublisherFactorySupport extends ComponentSupport
        implements ArtifactStorePublisherFactory {

    protected final RepositorySystem repositorySystem;
    protected final Map<String, ArtifactStoreRequirementsFactory> artifactStoreRequirementsFactories;

    protected ArtifactStorePublisherFactorySupport(
            RepositorySystem repositorySystem,
            Map<String, ArtifactStoreRequirementsFactory> artifactStoreRequirementsFactories) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.artifactStoreRequirementsFactories = requireNonNull(artifactStoreRequirementsFactories);
    }

    @Override
    public final ArtifactStorePublisher create(Session session) {
        requireNonNull(session);
        return doCreate(session);
    }

    protected ArtifactStoreRequirements createArtifactStoreRequirements(
            Session session, PublisherConfigSupport config) {
        ArtifactStoreRequirements artifactStoreRequirements = ArtifactStoreRequirements.NONE;
        if (!ArtifactStoreRequirements.NONE.name().equals(config.artifactStoreRequirements())) {
            artifactStoreRequirements = artifactStoreRequirementsFactories
                    .get(config.artifactStoreRequirements())
                    .create(session);
        }
        return artifactStoreRequirements;
    }

    protected abstract ArtifactStorePublisher doCreate(Session session);
}
