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
import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactPublisherRedirector;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.ConfigUtils;

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
                    repository,
                    session.config().currentProject().orElseThrow(J8Utils.OET).repositoryMode());
        }
        return url;
    }

    @Override
    public String getRepositoryUrl(RemoteRepository repository, RepositoryMode repositoryMode) {
        requireNonNull(repository);
        requireNonNull(repositoryMode);

        String url = repository.getUrl();
        Optional<Map<String, String>> sco = serviceConfiguration(repository.getId());
        if (!url.startsWith(SessionConfig.NAME + ":") && sco.isPresent()) {
            Map<String, String> config = sco.orElseThrow(J8Utils.OET);
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
    public RemoteRepository getAuthRepositoryId(RemoteRepository repository, boolean followAuthRedirection) {
        requireNonNull(repository);

        RemoteRepository authSource = repository;
        if (followAuthRedirection) {
            LinkedHashSet<String> authSourcesVisited = new LinkedHashSet<>();
            authSourcesVisited.add(authSource.getId());
            Optional<Map<String, String>> config = serviceConfiguration(authSource.getId());
            while (config.isPresent()) {
                String authRedirect = config.orElseThrow(J8Utils.OET).get(SessionConfig.CONFIG_AUTH_REDIRECT);
                if (authRedirect != null) {
                    logger.debug("Following auth redirect {} -> {}", authSource.getId(), authRedirect);
                    authSource = new RemoteRepository.Builder(
                                    authRedirect, authSource.getContentType(), authSource.getUrl())
                            .build();
                    if (!authSourcesVisited.add(authSource.getId())) {
                        throw new IllegalStateException("Auth redirect forms a cycle: " + authSourcesVisited);
                    }
                    config = serviceConfiguration(authSource.getId());
                } else {
                    break;
                }
            }
            if (!Objects.equals(repository.getId(), authSource.getId())) {
                logger.debug("Trail of AUTH for {}: {}", repository.getId(), String.join(" -> ", authSourcesVisited));
            }
        } else {
            logger.debug("Auth redirection following inhibited");
        }
        return authSource;
    }

    @Override
    public RemoteRepository getPublishingRepository(
            RemoteRepository repository, boolean expectAuth, boolean followAuthRedirection) {
        requireNonNull(repository);

        // handle auth redirection, if needed
        RemoteRepository authSource = repositorySystem.newDeploymentRepository(
                session.config().session(), getAuthRepositoryId(repository, followAuthRedirection));
        if (!Objects.equals(repository.getId(), authSource.getId())) {
            repository = new RemoteRepository.Builder(repository)
                    .setAuthentication(authSource.getAuthentication())
                    .setProxy(authSource.getProxy())
                    .build();
        } else {
            repository = authSource;
        }

        if (expectAuth && repository.getAuthentication() == null) {
            logger.warn("Publishing repository '{}' has no authentication set", authSource.getId());
        }
        return repository;
    }

    @Override
    public Optional<String> getArtifactStorePublisherName() {
        if (session.config().effectiveProperties().containsKey(SessionConfig.CONFIG_PUBLISHER)) {
            return Optional.of(session.config().effectiveProperties().get(SessionConfig.CONFIG_PUBLISHER));
        }
        if (session.config().currentProject().isPresent()) {
            RemoteRepository distributionRepository = session.config()
                    .currentProject()
                    .orElseThrow(J8Utils.OET)
                    .distributionManagementRepositories()
                    .get(session.config()
                            .currentProject()
                            .orElseThrow(J8Utils.OET)
                            .repositoryMode());
            if (distributionRepository != null) {
                Optional<Map<String, String>> sco = serviceConfiguration(distributionRepository.getId());
                if (sco.isPresent()) {
                    String publisher = sco.orElseThrow(J8Utils.OET).get(SessionConfig.CONFIG_PUBLISHER);
                    if (publisher != null) {
                        return Optional.of(publisher);
                    }
                }
                return Optional.of(distributionRepository.getId());
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the "service configuration" for given server ID.
     */
    private Optional<Map<String, String>> serviceConfiguration(String serverId) {
        requireNonNull(serverId);
        Object configuration = ConfigUtils.getObject(
                session.config().session(),
                null,
                "aether.connector.wagon.config." + serverId,
                "aether.transport.wagon.config." + serverId);
        if (configuration != null) {
            PlexusConfiguration config;
            if (configuration instanceof PlexusConfiguration) {
                config = (PlexusConfiguration) configuration;
            } else if (configuration instanceof Xpp3Dom) {
                config = new XmlPlexusConfiguration((Xpp3Dom) configuration);
            } else {
                throw new IllegalArgumentException("unexpected configuration type: "
                        + configuration.getClass().getName());
            }
            HashMap<String, String> serviceConfiguration = new HashMap<>();
            for (PlexusConfiguration child : config.getChildren()) {
                if (child.getName().startsWith(SessionConfig.KEY_PREFIX) && child.getValue() != null) {
                    serviceConfiguration.put(child.getName(), child.getValue());
                }
            }
            return Optional.of(serviceConfiguration);
        }
        return Optional.empty();
    }
}
