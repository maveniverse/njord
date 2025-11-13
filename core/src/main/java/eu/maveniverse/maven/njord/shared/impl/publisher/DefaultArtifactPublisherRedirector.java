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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

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

        Map<String, String> config = effectiveConfiguration(repository.getId(), false);
        if (!repository.getUrl().startsWith(SessionConfig.NAME + ":")) {
            String redirectUrl = getRedirectUrl(
                    config,
                    configuration(repository.getId(), false).orElse(Collections.emptyMap()),
                    repositoryMode,
                    repository);
            if (redirectUrl != null) {
                logger.debug("Found server {} configured URL: {}", repository.getId(), redirectUrl);
                return redirectUrl;
            }
        }
        return repository.getUrl();
    }

    private String getRedirectUrl(
            Map<String, String> effectiveConfig,
            Map<String, String> serverConfig,
            RepositoryMode mode,
            RemoteRepository repository) {
        String key;
        if (mode == RepositoryMode.RELEASE) {
            key = SessionConfig.CONFIG_RELEASE_URL;
        } else if (mode == RepositoryMode.SNAPSHOT) {
            key = SessionConfig.CONFIG_SNAPSHOT_URL;
        } else {
            throw new IllegalStateException("Unknown repository mode: " + mode);
        }
        // if conf comes from server/config, and contains it unprefixed, use it
        if (serverConfig.containsKey(SessionConfig.SERVER_ID_KEY) && serverConfig.containsKey(key)) {
            return serverConfig.get(key);
        }
        // try repoId suffixed property (most specific)
        String suffixedUrl = effectiveConfig.get(key + "." + repository.getId());
        if (suffixedUrl != null) {
            return suffixedUrl;
        }
        // if project present, try unprefixed IF repoID == project.release.distRepoID
        Optional<SessionConfig.CurrentProject> project = session.config().currentProject();
        if (effectiveConfig.containsKey(key) && project.isPresent()) {
            RemoteRepository dist = project.orElseThrow(J8Utils.OET)
                    .distributionManagementRepositories()
                    .get(mode);
            if (dist != null && Objects.equals(repository.getId(), dist.getId())) {
                return effectiveConfig.get(key);
            }
        }
        return null;
    }

    @Override
    public RemoteRepository getAuthRepositoryId(RemoteRepository repository) {
        requireNonNull(repository);

        RemoteRepository authSource = repository;
        Map<String, String> config = effectiveConfiguration(repository.getId(), true);
        if (config.containsKey(SessionConfig.SERVER_ID_KEY)) {
            authSource = new RemoteRepository.Builder(
                            requireNonNull(config.get(SessionConfig.SERVER_ID_KEY)),
                            authSource.getContentType(),
                            authSource.getUrl())
                    .build();
            logger.debug("Found server {} configured auth in server {}", repository.getId(), authSource.getId());
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
                // see is name a server id (w/ config) and return configured publisher
                Map<String, String> config = effectiveConfiguration(name, false);
                String originServerId = config.getOrDefault(SessionConfig.SERVER_ID_KEY, "<properties>");
                String publisher = config.get(SessionConfig.CONFIG_PUBLISHER);
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
                throw new IllegalArgumentException("Name '" + name
                        + "' is not a name of known publisher nor is server ID with configured publisher");
            }
        }
        return getArtifactStorePublisherName();
    }

    /**
     * Creates "effective" server configuration by properly factoring in possible server configuration. Returns a mutable
     * map of properties. Is configuration for asked {@code serverId} existing, can be queried by the presence of the
     * {@link SessionConfig#SERVER_ID_KEY} key in the returned map.
     */
    protected Map<String, String> effectiveConfiguration(String serverId, boolean followAuthRedirection) {
        HashMap<String, String> config = new HashMap<>(session.config().effectiveProperties());
        configuration(serverId, followAuthRedirection).ifPresent(server -> {
            config.putAll(server);
            session.config().currentProject().ifPresent(project -> config.putAll(project.projectProperties()));
            config.putAll(session.config().userProperties());
        });
        return config;
    }

    /**
     * Returns the Njord configuration for given server ID (under servers/server/serverId/config) and is able to
     * follow redirections. Hence, if a map is returned, the {@link SessionConfig#SERVER_ID_KEY} may be different that the
     * server ID called used (due redirections).
     */
    protected Optional<Map<String, String>> configuration(String serverId, boolean followAuthRedirection) {
        requireNonNull(serverId);

        String source = serverId;
        Optional<Map<String, String>> config = session.config().serverConfiguration(source);
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
                logger.debug("Following redirect {} -> {}", source, redirect);
                if (!sourcesVisited.add(redirect)) {
                    throw new IllegalStateException("Auth redirect forms a cycle: " + redirect);
                }
                source = redirect;
                config = session.config().serverConfiguration(source);
            } else {
                break;
            }
        }
        if (!Objects.equals(serverId, source)) {
            logger.debug("Trail of redirects for {}: {}", serverId, String.join(" -> ", sourcesVisited));
        }
        return config;
    }
}
