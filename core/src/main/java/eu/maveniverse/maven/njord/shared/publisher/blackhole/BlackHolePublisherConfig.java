/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher.blackhole;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.publisher.PublisherConfigSupport;

/**
 * Black Hole publisher config.
 * <p>
 * Properties supported:
 * <ul>
 *     <li><code>njord.publisher.black-hole.artifactStoreRequirements</code> - the artifact store requirements to apply</li>
 *     <li><code>njord.publisher.black-hole.fail</code> - to fail on publishing</li>
 * </ul>
 */
public final class BlackHolePublisherConfig extends PublisherConfigSupport {
    private final boolean fail;

    public BlackHolePublisherConfig(SessionConfig sessionConfig) {
        super(BlackHolePublisherFactory.NAME, sessionConfig);
        this.fail = Boolean.parseBoolean(
                sessionConfig.effectiveProperties().getOrDefault(keyName("fail"), Boolean.FALSE.toString()));
    }

    public boolean fail() {
        return fail;
    }
}
