/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactPublisherRedirector;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

public class DefaultArtifactPublisherRedirector extends ComponentSupport implements ArtifactPublisherRedirector {
    private final Session session;
    private final RepositorySystem repositorySystem;

    public DefaultArtifactPublisherRedirector(Session session, RepositorySystem repositorySystem) {
        this.session = requireNonNull(session);
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public String getRepositoryUrl(RemoteRepository repository) {
        requireNonNull(repository);

        String url = repository.getUrl();
        if (!url.startsWith(SessionConfig.NAME + ":")
                && session.config().currentProject().isPresent()) {
            return getRepositoryUrl(
                    repository, session.config().currentProject().orElseThrow().repositoryMode());
        }
        return url;
    }

    @Override
    public String getRepositoryUrl(RemoteRepository repository, RepositoryMode repositoryMode) {
        requireNonNull(repository);
        requireNonNull(repositoryMode);

        String url = repository.getUrl();
        Optional<Map<String, String>> sco = session.config().serviceConfiguration(repository.getId());
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
        requireNonNull(repository);

        RemoteRepository authSource = repository;
        LinkedHashSet<String> authSourcesVisited = new LinkedHashSet<>();
        authSourcesVisited.add(authSource.getId());
        Optional<Map<String, String>> config = session.config().serviceConfiguration(authSource.getId());
        while (config.isPresent()) {
            String authRedirect = config.orElseThrow().get(SessionConfig.CONFIG_AUTH_REDIRECT);
            if (authRedirect != null) {
                authSource = new RemoteRepository.Builder(
                                authRedirect, authSource.getContentType(), authSource.getUrl())
                        .build();
                if (!authSourcesVisited.add(authSource.getId())) {
                    throw new IllegalStateException("Auth redirect forms a cycle: " + authSourcesVisited);
                }
                config = session.config().serviceConfiguration(authSource.getId());
            } else {
                break;
            }
        }
        return authSource;
    }

    @Override
    public RemoteRepository getPublishingRepository(RemoteRepository repository) {
        // handle auth redirection, if needed
        RemoteRepository authSource =
                repositorySystem.newDeploymentRepository(session.config().session(), getAuthRepositoryId(repository));
        if (!Objects.equals(repository.getId(), authSource.getId())) {
            repository = new RemoteRepository.Builder(repository)
                    .setAuthentication(authSource.getAuthentication())
                    .setProxy(authSource.getProxy())
                    .build();
        }
        if (repository.getAuthentication() == null) {
            logger.warn("Publishing repository '{}' has no authentication set", authSource.getId());
        }
        return repository;
    }

    @Override
    public Optional<String> getArtifactStorePublisherName() {
        if (session.config().publisher().isPresent()) {
            return session.config().publisher();
        }
        if (session.config().currentProject().isPresent()) {
            RemoteRepository distributionRepository = session.config()
                    .currentProject()
                    .orElseThrow()
                    .distributionManagementRepositories()
                    .get(session.config().currentProject().orElseThrow().repositoryMode());
            if (distributionRepository != null) {
                Optional<Map<String, String>> sco =
                        session.config().serviceConfiguration(distributionRepository.getId());
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
