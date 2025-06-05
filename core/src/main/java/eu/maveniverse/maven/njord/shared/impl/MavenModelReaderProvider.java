/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl;

import eu.maveniverse.maven.mima.extensions.mmr.internal.MavenModelReaderImpl;
import java.util.Collections;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.interpolation.StringVisitorModelInterpolator;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.RepositoryEventDispatcher;

@Singleton
@Named
public class MavenModelReaderProvider implements Provider<MavenModelReaderImpl> {
    private final MavenModelReaderImpl instance;

    @Inject
    public MavenModelReaderProvider(
            RepositorySystem repositorySystem,
            RemoteRepositoryManager remoteRepositoryManager,
            RepositoryEventDispatcher repositoryEventDispatcher,
            ModelBuilder modelBuilder,
            StringVisitorModelInterpolator stringVisitorModelInterpolator) {
        this.instance = new MavenModelReaderImpl(
                repositorySystem,
                remoteRepositoryManager,
                repositoryEventDispatcher,
                modelBuilder,
                stringVisitorModelInterpolator,
                Collections.emptyList());
    }

    @Override
    public MavenModelReaderImpl get() {
        return instance;
    }
}
