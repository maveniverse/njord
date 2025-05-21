/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher;

import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.util.Optional;
import org.eclipse.aether.repository.RemoteRepository;

public interface ArtifactPublisherRedirector {
    /**
     * Tells, based on Njord config, what is the real URL used for given remote repository. Never returns {@code null},
     * and without any configuration just returns passed in repository URL.
     */
    String getRepositoryUrl(RemoteRepository repository);

    /**
     * Tells, based on Njord config, what is the real URL used for given remote repository. Never returns {@code null},
     * and without any configuration just returns passed in repository URL.
     */
    String getRepositoryUrl(RemoteRepository repository, RepositoryMode repositoryMode);

    /**
     * Returns the remote repository to source auth from for the passed in remote repository. Never returns {@code null}.
     */
    RemoteRepository getAuthRepositoryId(RemoteRepository repository);

    /**
     * Returns the remote repository to use for publishing. Never returns {@code null}. The repository will have
     * auth and any auth-redirection, if applicable, applied.
     *
     * @see #getAuthRepositoryId(RemoteRepository)
     */
    RemoteRepository getPublishingRepository(RemoteRepository repository, boolean expectAuth);

    /**
     * Returns the name of wanted/configured {@link eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher}.
     */
    Optional<String> getArtifactStorePublisherName();
}
