/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactPublisherRedirector;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactPublisherRedirectorFactory;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class DefaultArtifactPublisherRedirectorFactory implements ArtifactPublisherRedirectorFactory {
    @Override
    public ArtifactPublisherRedirector create(Session session) {
        return new DefaultArtifactPublisherRedirector(session);
    }
}
