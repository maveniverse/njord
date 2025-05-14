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
import eu.maveniverse.maven.njord.shared.deploy.ArtifactDeployerRedirector;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
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

    private final Logger logger = LoggerFactory.getLogger(NjordRepositoryConnectorFactory.class);
    private final ArtifactDeployerRedirector artifactDeployerRedirector;
    private final BasicRepositoryConnectorFactory basicRepositoryConnectorFactory;

    @Inject
    public NjordRepositoryConnectorFactory(
            ArtifactDeployerRedirector artifactDeployerRedirector,
            BasicRepositoryConnectorFactory basicRepositoryConnectorFactory) {
        this.artifactDeployerRedirector = requireNonNull(artifactDeployerRedirector);
        this.basicRepositoryConnectorFactory = requireNonNull(basicRepositoryConnectorFactory);
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
            Optional<Session> nso = NjordUtils.mayGetNjordSession(session);
            if (nso.isPresent()
                    && nso.orElseThrow(() -> new IllegalStateException("Value unavailable"))
                            .config()
                            .enabled()) {
                Session ns = nso.orElseThrow(() -> new IllegalStateException("Value unavailable"));
                String url = artifactDeployerRedirector.getRepositoryUrl(ns.config(), repository);
                if (url != null && url.startsWith(NAME + ":")) {
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
        throw new NoRepositoryConnectorException(repository);
    }

    @Override
    public float getPriority() {
        return 10;
    }
}
