/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.extension3;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.NjordUtils;
import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.transfer.NoRepositoryConnectorException;
import org.eclipse.aether.util.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for now "hard wraps" basic, but it should be made smarter.
 */
@Named(NjordRepositoryConnectorFactory.NAME)
public class NjordRepositoryConnectorFactory implements RepositoryConnectorFactory {
    public static final String NAME = "njord";

    private static final Logger logger = LoggerFactory.getLogger(NjordRepositoryConnectorFactory.class);
    private final Map<String, Provider<RepositoryConnectorFactory>> repositoryConnectorFactories;

    @Inject
    public NjordRepositoryConnectorFactory(
            Map<String, Provider<RepositoryConnectorFactory>> repositoryConnectorFactories) {
        this.repositoryConnectorFactories = requireNonNull(repositoryConnectorFactories);
    }

    /**
     * {@code njord:}
     * {@code njord:templateName}
     * {@code njord:template:templateName}
     * {@code njord:store:storeName}
     */
    @Override
    public RepositoryConnector newInstance(RepositorySystemSession session, RemoteRepository repository)
            throws NoRepositoryConnectorException {
        boolean connectorSkip = ConfigUtils.getBoolean(session, false, NjordUtils.RESOLVER_SESSION_CONNECTOR_SKIP);
        if (!connectorSkip) {
            // is repository used for deployment or for downloading?
            Optional<Session> nso = NjordUtils.mayGetNjordSession(session);
            if (nso.isPresent()) {
                Session ns = nso.orElseThrow(J8Utils.OET);
                if (isDistributionRepo(repository, ns.config())) {
                    String url = ns.artifactPublisherRedirector().getRepositoryUrl(repository);
                    if (url != null && url.startsWith(NAME + ":")) {
                        RepositoryConnectorFactory basicRepositoryConnectorFactory = requireNonNull(
                                repositoryConnectorFactories.get("basic").get(),
                                "No basic repository connector factory found");
                        ArtifactStore artifactStore = ns.getOrCreateSessionArtifactStore(url.substring(6));
                        return new NjordRepositoryConnector(
                                artifactStore,
                                repository,
                                basicRepositoryConnectorFactory.newInstance(
                                        artifactStore.storeRepositorySession(session),
                                        artifactStore.storeRemoteRepository()));
                    }
                }
            }
        }
        throw new NoRepositoryConnectorException(repository);
    }

    static boolean isDistributionRepo(RemoteRepository repository, SessionConfig config) {
        if (config.currentProject().isPresent()) {
            RemoteRepository distributionRepository = config.currentProject()
                    .orElseThrow(J8Utils.OET)
                    .distributionManagementRepositories()
                    .get(config.currentProject().orElseThrow(J8Utils.OET).repositoryMode());
            if (distributionRepository != null
                    && !distributionRepository.getId().equals(repository.getId())) {
                logger.debug(
                        "Repository {} is not having the id of the project's distribution repository {}",
                        repository,
                        distributionRepository);
                return false;
            } else {
                logger.debug(
                        "Haven't found project's distribution repository for repository mode {}",
                        config.currentProject().orElseThrow(J8Utils.OET).repositoryMode());
            }
        } else {
            logger.debug("No project available every repository may be a distribution one");
        }
        return true;
    }

    @Override
    public float getPriority() {
        return 10;
    }
}
