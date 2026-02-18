/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.deploy;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.NjordRepositoryListener;
import eu.maveniverse.maven.njord.shared.publisher.PublisherConfigSupport;
import java.util.Locale;
import org.eclipse.aether.util.ConfigUtils;

/**
 * Deploy publisher config.
 */
public final class DeployPublisherConfig extends PublisherConfigSupport {
    private final NjordRepositoryListener.Mode listenerMode;

    public DeployPublisherConfig(SessionConfig sessionConfig) {
        super(DeployPublisherFactory.NAME, sessionConfig);

        this.listenerMode = NjordRepositoryListener.Mode.valueOf(ConfigUtils.getString(
                        sessionConfig.effectiveProperties(),
                        NjordRepositoryListener.Mode.AGGREGATED.name(),
                        keyNames("listenerMode"))
                .toUpperCase(Locale.ROOT));
    }

    public NjordRepositoryListener.Mode listenerMode() {
        return listenerMode;
    }
}
