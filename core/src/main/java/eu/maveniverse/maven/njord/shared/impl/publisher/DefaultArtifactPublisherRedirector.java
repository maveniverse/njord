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
    protected final Session session;
    protected final RepositorySystem repositorySystem;

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
        Optional<Map<String, String>> sco = configuration(repository.getId(), false);
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
                logger.debug("Found server {} configured URL: {}", repository.getId(), redirectUrl);
                return redirectUrl;
            }
        }
        return url;
    }

    @Override
    public RemoteRepository getAuthRepositoryId(RemoteRepository repository) {
        requireNonNull(repository);

        RemoteRepository authSource = repository;
        Optional<Map<String, String>> config = configuration(authSource.getId(), true);
        if (config.isPresent()) {
            authSource = new RemoteRepository.Builder(
                            requireNonNull(config.orElseThrow(J8Utils.OET).get(SERVER_ID_KEY)),
                            authSource.getContentType(),
                            authSource.getUrl())
                    .build();
            logger.debug("Found server {} configured auth redirect to {}", repository.getId(), authSource.getId());
        }
        return repositorySystem.newDeploymentRepository(session.config().session(), authSource);
    }

    @Override
    public RemoteRepository getPublishingRepository(RemoteRepository repository, boolean expectAuth) {
        requireNonNull(repository);

        // handle auth redirection, if needed
        RemoteRepository authSource = getAuthRepositoryId(repository);
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
            String publisher = session.config().effectiveProperties().get(SessionConfig.CONFIG_PUBLISHER);
            if (session.selectArtifactStorePublisher(publisher).isPresent()) {
                logger.debug("Found publisher {} in effective properties", publisher);
                return Optional.of(publisher);
            } else {
                throw new IllegalStateException(
                        String.format("Session contains unknown publisher name '%s' set as property", publisher));
            }
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
            if (distributionRepository != null && distributionRepository.getId() != null) {
                logger.debug(
                        "Trying current project distribution management repository ID {}",
                        distributionRepository.getId());
                return getArtifactStorePublisherName(distributionRepository.getId());
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getArtifactStorePublisherName(String name) {
        if (name != null) {
            if (session.selectArtifactStorePublisher(name).isPresent()) {
                // name corresponds to existing publisher: return it
                logger.debug("Passed in name {} is a valid publisher name", name);
                return Optional.of(name);
            } else {
                // see is name a server id (w/ config)
                Optional<Map<String, String>> sco = configuration(name, false);
                if (sco.isPresent()) {
                    String originServerId = sco.orElseThrow(J8Utils.OET).get(SERVER_ID_KEY);
                    String publisher = sco.orElseThrow(J8Utils.OET).get(SessionConfig.CONFIG_PUBLISHER);
                    if (publisher != null
                            && session.selectArtifactStorePublisher(publisher).isPresent()) {
                        if (session.selectArtifactStorePublisher(publisher).isPresent()) {
                            logger.debug(
                                    "Passed in name {} led us to server {} with configured publisher {}",
                                    name,
                                    originServerId,
                                    publisher);
                            return Optional.of(publisher);
                        } else {
                            throw new IllegalStateException(String.format(
                                    "Server '%s' contains unknown publisher '%s'", originServerId, publisher));
                        }
                    }
                }
                throw new IllegalArgumentException("Name '" + name
                        + "' is not a name of known publisher nor is server ID with configured publisher");
            }
        }
        return getArtifactStorePublisherName();
    }

    /**
     * This key is always inserted into map returned by {@link #configuration(String)} and {@link #configuration(String, boolean)}
     * carrying the "origin server ID".
     */
    protected static final String SERVER_ID_KEY = "_serverId";

    /**
     * Returns the Njord configuration for given server ID (under servers/server/serverId/config) and is able to
     * follow redirections. Hence, if a map is returned, the {@link #SERVER_ID_KEY} may be different that the
     * server ID called used (due redirections).
     */
    protected Optional<Map<String, String>> configuration(String serverId, boolean followAuthRedirection) {
        requireNonNull(serverId);

        String source = serverId;
        Optional<Map<String, String>> config = configuration(source);
        LinkedHashSet<String> sourcesVisited = new LinkedHashSet<>();
        sourcesVisited.add(source);
        while (config.isPresent()) {
            String redirect = config.orElseThrow(J8Utils.OET).get(SessionConfig.CONFIG_SERVICE_REDIRECT);
            if (redirect == null) {
                redirect = followAuthRedirection
                        ? config.orElseThrow(J8Utils.OET).get(SessionConfig.CONFIG_AUTH_REDIRECT)
                        : null;
            }
            if (redirect != null) {
                logger.debug("Following service redirect {} -> {}", source, redirect);
                if (!sourcesVisited.add(redirect)) {
                    throw new IllegalStateException("Auth redirect forms a cycle: " + redirect);
                }
                source = redirect;
                config = configuration(source);
            } else {
                break;
            }
        }
        if (!Objects.equals(serverId, source)) {
            logger.debug("Trail of service redirects for {}: {}", serverId, String.join(" -> ", sourcesVisited));
        }
        return config;
    }

    /**
     * Returns the Njord configuration for given server ID (under servers/server/serverId/config).
     * The map (if present) always contains mapping with key {@link #SERVER_ID_KEY} that contains server ID that
     * configuration originates from.
     */
    protected Optional<Map<String, String>> configuration(String serverId) {
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
            HashMap<String, String> serviceConfiguration = new HashMap<>(config.getChildCount() + 1);
            serviceConfiguration.put(SERVER_ID_KEY, serverId);
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
