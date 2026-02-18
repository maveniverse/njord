/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.install;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.NjordUtils;
import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.impl.store.ArtifactStoreInstaller;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherSupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;

public class InstallPublisher extends ArtifactStorePublisherSupport {
    private final InstallPublisherConfig config;

    public InstallPublisher(Session session, RepositorySystem repositorySystem, InstallPublisherConfig config) {
        super(
                session,
                repositorySystem,
                InstallPublisherFactory.NAME,
                "Publishes to local repository just like maven-install-plugin would",
                null,
                null,
                null,
                null,
                ArtifactStoreRequirements.NONE);
        this.config = requireNonNull(config);
    }

    @Override
    public boolean isConfigured() {
        return true; // always available
    }

    @Override
    protected void doPublish(ArtifactStore artifactStore) throws IOException {
        if (session.config().dryRun()) {
            logger.info("Dry run; not publishing '{}' to local repository", artifactStore.name());
            return;
        }
        logger.debug("Publishing '{}' to local repository", artifactStore.name());
        new ArtifactStoreInstaller(
                        repositorySystem,
                        new DefaultRepositorySystemSession(session.config().session())
                                .setConfigProperty(NjordUtils.RESOLVER_SESSION_CONNECTOR_SKIP, true),
                        config.isSilent())
                .install(artifactStore);
    }
}
