/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype.nx2;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.publisher.PublisherConfigSupport;

/**
 * Sonatype NX2 config.
 */
public final class SonatypeNx2PublisherConfig extends PublisherConfigSupport {
    public SonatypeNx2PublisherConfig(SessionConfig sessionConfig) {
        super(SonatypeNx2PublisherFactory.NAME, sessionConfig);
    }
}
