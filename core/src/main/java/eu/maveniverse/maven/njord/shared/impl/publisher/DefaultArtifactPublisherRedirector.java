/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactPublisherRedirector;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import org.eclipse.aether.repository.RemoteRepository;

public class DefaultArtifactPublisherRedirector extends ComponentSupport implements ArtifactPublisherRedirector {
    private final SessionConfig sessionConfig;

    public DefaultArtifactPublisherRedirector(SessionConfig sessionConfig) {
        this.sessionConfig = requireNonNull(sessionConfig);
    }

    @Override
    public String getRepositoryUrl(RemoteRepository repository) {
        String url = repository.getUrl();
        if (!url.startsWith(SessionConfig.NAME + ":")
                && sessionConfig.currentProject().isPresent()) {
            return getRepositoryUrl(
                    repository, sessionConfig.currentProject().orElseThrow().repositoryMode());
        }
        return url;
    }

    @Override
    public String getRepositoryUrl(RemoteRepository repository, RepositoryMode repositoryMode) {
        String url = repository.getUrl();
        Optional<Map<String, String>> sco = sessionConfig.serviceConfiguration(repository.getId());
        if (!url.startsWith(SessionConfig.NAME + ":") && sco.isPresent()) {
            Map<String, String> config = sco.orElseThrow();
            String redirectUrl;
            switch (repositoryMode) {
                case RELEASE:
                    redirectUrl = config.get(SessionConfig.CONFIG_RELEASE_URL);
                    break;
                case SNAPSHOT:
                    redirectUrl = config.get(SessionConfig.CONFIG_SNAPSHOT_URL);
                    break;
                default:
                    throw new IllegalStateException("Unknown repository mode: " + repositoryMode);
            }
            if (redirectUrl != null) {
                return redirectUrl;
            }
        }
        return url;
    }

    @Override
    public RemoteRepository getAuthRepositoryId(RemoteRepository repository) {
        RemoteRepository authSource = repository;
        LinkedHashSet<String> authSourcesVisited = new LinkedHashSet<>();
        authSourcesVisited.add(authSource.getId());
        Optional<Map<String, String>> config = sessionConfig.serviceConfiguration(authSource.getId());
        while (config.isPresent()) {
            String authRedirect = config.orElseThrow().get(SessionConfig.CONFIG_AUTH_REDIRECT);
            if (authRedirect != null) {
                authSource = new RemoteRepository.Builder(
                                authRedirect, authSource.getContentType(), authSource.getUrl())
                        .build();
                if (!authSourcesVisited.add(authSource.getId())) {
                    throw new IllegalStateException("Auth redirect forms a cycle: " + authSourcesVisited);
                }
                config = sessionConfig.serviceConfiguration(authSource.getId());
            } else {
                break;
            }
        }
        return authSource;
    }

    @Override
    public Optional<String> getArtifactStorePublisherName() {
        if (sessionConfig.publisher().isPresent()) {
            return sessionConfig.publisher();
        }
        if (sessionConfig.currentProject().isPresent()) {
            RemoteRepository distributionRepository = sessionConfig
                    .currentProject()
                    .orElseThrow()
                    .distributionManagementRepositories()
                    .get(sessionConfig.currentProject().orElseThrow().repositoryMode());
            if (distributionRepository != null) {
                Optional<Map<String, String>> sco = sessionConfig.serviceConfiguration(distributionRepository.getId());
                if (sco.isPresent()) {
                    String publisher = sco.orElseThrow().get(SessionConfig.CONFIG_PUBLISHER);
                    if (publisher != null) {
                        return Optional.of(publisher);
                    }
                }
                return Optional.of(distributionRepository.getId());
            }
        }
        return Optional.empty();
    }
}
