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
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
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
    private final BasicRepositoryConnectorFactory basicRepositoryConnectorFactory;

    @Inject
    public NjordRepositoryConnectorFactory(BasicRepositoryConnectorFactory basicRepositoryConnectorFactory) {
        this.basicRepositoryConnectorFactory = requireNonNull(basicRepositoryConnectorFactory);
    }

    /**
     * {@code repoId::njord:default}
     * {@code repoId::njord:template:templateName}
     * {@code repoId::njord:store:storeName}
     */
    @Override
    public RepositoryConnector newInstance(RepositorySystemSession session, RemoteRepository repository)
            throws NoRepositoryConnectorException {
        Optional<Session> nso = NjordUtils.mayGetNjordSessionIfEnabled(session);
        if (nso.isPresent()) {
            Session ns = nso.orElseThrow();
            if (NAME.equals(repository.getProtocol())) {
                ArtifactStore artifactStore =
                        ns.getOrCreateSessionArtifactStore(repository.getUrl().substring(6));
                return new NjordRepositoryConnector(
                        artifactStore,
                        repository,
                        basicRepositoryConnectorFactory.newInstance(
                                artifactStore.storeRepositorySession(session), artifactStore.storeRemoteRepository()));
            }
        }

        throw new NoRepositoryConnectorException(repository);
    }

    @Override
    public float getPriority() {
        return 10;
    }

    private static final String CONFIG_KEY_PREFIX = NAME + ".";
    private static final String CONFIG_PROP_CONFIG = "aether.transport.wagon.config";

    private Map<String, String> getServerConfiguration(RepositorySystemSession session, RemoteRepository repository) {
        HashMap<String, String> serverConfiguration = new HashMap<>();
        Object configuration = ConfigUtils.getObject(session, null, CONFIG_PROP_CONFIG + "." + repository.getId());
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
            for (PlexusConfiguration child : config.getChildren()) {
                if (child.getName().startsWith(CONFIG_KEY_PREFIX) && child.getValue() != null) {
                    serverConfiguration.put(child.getName(), child.getValue());
                }
            }
        }
        return serverConfiguration;
    }
}
