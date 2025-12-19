/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.SessionConfig;

/**
 * Publisher config support class.
 */
public abstract class PublisherConfigSupport {
    protected final String name;
    protected final SessionConfig sessionConfig;
    protected final String artifactStoreRequirements;

    public PublisherConfigSupport(String name, SessionConfig sessionConfig) {
        this.name = requireNonNull(name);
        this.sessionConfig = requireNonNull(sessionConfig);
        this.artifactStoreRequirements = sessionConfig
                .effectiveProperties()
                .getOrDefault(keyName("artifactStoreRequirements"), ArtifactStoreRequirements.NONE.name());
    }

    protected String keyName(String property) {
        requireNonNull(property);
        return "njord.publisher." + name + "." + property;
    }

    public String artifactStoreRequirements() {
        return artifactStoreRequirements;
    }
}
