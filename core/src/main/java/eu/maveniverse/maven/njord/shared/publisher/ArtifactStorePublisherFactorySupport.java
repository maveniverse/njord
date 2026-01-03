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
import java.util.stream.Collectors;
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
        String asr = config.artifactStoreRequirements();
        if (asr != null && !ArtifactStoreRequirements.NONE.name().equals(asr)) {
            ArtifactStoreRequirementsFactory factory = artifactStoreRequirementsFactories.get(asr);
            if (factory != null) {
                return factory.create(session);
            } else {
                throw new IllegalArgumentException(String.format(
                        "Unknown artifact store requirement: '%s', supported ones are: %s and '%s'",
                        asr,
                        artifactStoreRequirementsFactories.keySet().stream()
                                .map(n -> "'" + n + "'")
                                .collect(Collectors.joining(",")),
                        ArtifactStoreRequirements.NONE.name()));
            }
        }
        return artifactStoreRequirements;
    }

    protected abstract ArtifactStorePublisher doCreate(Session session);
}
