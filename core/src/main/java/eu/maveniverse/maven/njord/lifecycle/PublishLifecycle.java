/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.lifecycle;

import eu.maveniverse.maven.njord.shared.impl.Utils;
import java.util.List;
import java.util.Map;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;

/**
 * The publishing lifecycle.
 */
@Singleton
@Named(PublishLifecycle.NAME)
public class PublishLifecycle implements Provider<Lifecycle> {
    public static final String NAME = "publish";

    private static final String VERSION = Utils.discoverArtifactVersion(
            PublishLifecycle.class.getClassLoader(), "eu.maveniverse.maven.njord", "extension", "BOOM");

    private final Lifecycle lifecycle;

    public PublishLifecycle() {
        List<String> PHASES = List.of(NAME + "-prepare", NAME + "-perform", NAME);
        Map<String, LifecyclePhase> DEFAULT_PHASES = Map.of(
                "publish-prepare",
                new LifecyclePhase("eu.maveniverse.maven.plugins:njord:" + VERSION + ":validate"),
                "publish-perform",
                new LifecyclePhase("eu.maveniverse.maven.plugins:njord:" + VERSION + ":publish"));
        this.lifecycle = new Lifecycle(NAME, PHASES, DEFAULT_PHASES);
    }

    @Override
    public Lifecycle get() {
        return lifecycle;
    }
}
