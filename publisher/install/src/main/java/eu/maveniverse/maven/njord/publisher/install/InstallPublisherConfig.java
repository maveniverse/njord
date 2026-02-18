/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.install;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.NjordRepositoryListener;
import eu.maveniverse.maven.njord.shared.publisher.PublisherConfigSupport;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import org.eclipse.aether.util.ConfigUtils;

/**
 * Install publisher config.
 */
public final class InstallPublisherConfig extends PublisherConfigSupport {
    private final Path target;
    private final NjordRepositoryListener.Mode listenerMode;

    public InstallPublisherConfig(SessionConfig sessionConfig) {
        super(InstallPublisherFactory.NAME, sessionConfig);

        String targetString = ConfigUtils.getString(sessionConfig.effectiveProperties(), null, keyNames("target"));
        if (targetString != null) {
            this.target = Paths.get(targetString).toAbsolutePath();
        } else {
            this.target = null;
        }

        this.listenerMode = NjordRepositoryListener.Mode.valueOf(ConfigUtils.getString(
                        sessionConfig.effectiveProperties(),
                        NjordRepositoryListener.Mode.AGGREGATED.name(),
                        keyNames("listenerMode"))
                .toUpperCase(Locale.ROOT));
    }

    public Optional<Path> target() {
        return Optional.ofNullable(target);
    }

    public NjordRepositoryListener.Mode listenerMode() {
        return listenerMode;
    }
}
