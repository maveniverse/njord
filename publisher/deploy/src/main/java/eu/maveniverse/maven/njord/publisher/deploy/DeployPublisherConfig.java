/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.deploy;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.publisher.PublisherConfig;

public final class DeployPublisherConfig extends PublisherConfig {
    public static final String REPOSITORY_ID = "deploy";

    public DeployPublisherConfig(SessionConfig sessionConfig) {
        super(sessionConfig, DeployPublisherFactory.NAME, REPOSITORY_ID, null, REPOSITORY_ID, null);
    }
}
