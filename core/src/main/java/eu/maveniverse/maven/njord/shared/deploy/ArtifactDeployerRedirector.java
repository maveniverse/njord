/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.deploy;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import org.eclipse.aether.repository.RemoteRepository;

public interface ArtifactDeployerRedirector {
    /**
     * Tells, based on Njord config, what is the real URL used for given remote repository. Never returns {@code null},
     * and without any configuration just returns passed in repository URL.
     */
    String getRepositoryUrl(Session ns, RemoteRepository repository);

    /**
     * Tells, based on Njord config, what is the real URL used for given remote repository. Never returns {@code null},
     * and without any configuration just returns passed in repository URL.
     */
    String getRepositoryUrl(Session ns, RemoteRepository repository, RepositoryMode repositoryMode);
}
