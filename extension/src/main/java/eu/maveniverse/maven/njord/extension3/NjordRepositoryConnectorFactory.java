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
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.transfer.NoRepositoryConnectorException;
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
        if (NAME.equals(repository.getProtocol())) {
            Optional<ArtifactStoreManager> asm = NjordUtils.mayGetArtifactStoreManager(session);
            if (asm.isPresent()) {
                ArtifactStoreManager artifactStoreManager = asm.orElseThrow();
                ArtifactStore artifactStore = mayCreateArtifactStore(
                        session, artifactStoreManager, repository.getUrl().substring(6));
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

    private ArtifactStore mayCreateArtifactStore(
            RepositorySystemSession session, ArtifactStoreManager artifactStoreManager, String uri) {
        String nameKey = NjordRepositoryConnectorFactory.class.getName() + "." + uri;
        String storeName = (String) session.getData().computeIfAbsent(nameKey, () -> {
            try {
                String artifactStoreName;
                if (!uri.contains(":")) {
                    if (uri.isEmpty()) {
                        // empty -> default
                        try (ArtifactStore artifactStore = artifactStoreManager.createArtifactStore(
                                session, artifactStoreManager.defaultTemplate())) {
                            artifactStoreName = artifactStore.name();
                        }
                    } else {
                        // non-empty -> template name
                        List<ArtifactStoreTemplate> templates = artifactStoreManager.listTemplates().stream()
                                .filter(t -> t.name().equals(uri))
                                .toList();
                        if (templates.size() != 1) {
                            throw new IllegalArgumentException("Unknown template: " + uri);
                        } else {
                            try (ArtifactStore artifactStore =
                                    artifactStoreManager.createArtifactStore(session, templates.get(0))) {
                                artifactStoreName = artifactStore.name();
                            }
                        }
                    }
                } else if (uri.startsWith("store:")) {
                    artifactStoreName = uri.substring(11);
                } else {
                    throw new IllegalArgumentException("Invalid repository URI: " + uri);
                }
                return artifactStoreName;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        try {
            return artifactStoreManager
                    .selectArtifactStore(storeName)
                    .orElseThrow(() -> new IllegalArgumentException("No such store: " + storeName));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
