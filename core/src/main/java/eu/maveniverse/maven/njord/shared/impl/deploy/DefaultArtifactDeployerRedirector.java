/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.deploy;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.deploy.ArtifactDeployerRedirector;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.repository.RemoteRepository;

@Singleton
@Named
public class DefaultArtifactDeployerRedirector extends ComponentSupport implements ArtifactDeployerRedirector {
    @Override
    public String getRepositoryUrl(SessionConfig sc, RemoteRepository repository) {
        String url = repository.getUrl();
        if (!url.startsWith(SessionConfig.NAME + ":") && sc.currentProject().isPresent()) {
            return getRepositoryUrl(
                    sc, repository, sc.currentProject().orElseThrow().repositoryMode());
        }
        return url;
    }

    @Override
    public String getRepositoryUrl(SessionConfig sc, RemoteRepository repository, RepositoryMode repositoryMode) {
        String url = repository.getUrl();
        Optional<Map<String, String>> sco = sc.serviceConfiguration(repository.getId());
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
    public RemoteRepository getAuthRepositoryId(SessionConfig sc, RemoteRepository repository) {
        RemoteRepository authSource = repository;
        LinkedHashSet<String> authSourcesVisited = new LinkedHashSet<>();
        authSourcesVisited.add(authSource.getId());
        Optional<Map<String, String>> config = sc.serviceConfiguration(authSource.getId());
        while (config.isPresent()) {
            String authRedirect = config.orElseThrow().get(SessionConfig.CONFIG_AUTH_REDIRECT);
            if (authRedirect != null) {
                authSource = new RemoteRepository.Builder(
                                authRedirect, authSource.getContentType(), authSource.getUrl())
                        .build();
                if (!authSourcesVisited.add(authSource.getId())) {
                    throw new IllegalStateException("Auth redirect forms a cycle: " + authSourcesVisited);
                }
                config = sc.serviceConfiguration(authSource.getId());
            } else {
                break;
            }
        }
        return authSource;
    }

    @Override
    public Optional<String> getArtifactStorePublisherName(SessionConfig sc) {
        if (sc.publisher().isPresent()) {
            return sc.publisher();
        }
        if (sc.currentProject().isPresent()) {
            RemoteRepository distributionRepository = sc.currentProject()
                    .orElseThrow()
                    .distributionManagementRepositories()
                    .get(sc.currentProject().orElseThrow().repositoryMode());
            if (distributionRepository != null) {
                Optional<Map<String, String>> sco = sc.serviceConfiguration(distributionRepository.getId());
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
