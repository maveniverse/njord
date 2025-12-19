/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.extensions.mmr.ModelRequest;
import eu.maveniverse.maven.mima.extensions.mmr.ModelResponse;
import eu.maveniverse.maven.mima.extensions.mmr.internal.MavenModelReaderImpl;
import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactPublisherRedirector;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactPublisherRedirectorFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreComparator;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreComparatorFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMerger;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMergerFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreTemplate;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreWriter;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreWriterFactory;
import eu.maveniverse.maven.shared.core.component.CloseableConfigSupport;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.maven.model.Model;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

public class DefaultSession extends CloseableConfigSupport<SessionConfig> implements Session {
    private final String sessionBoundStoresKey;
    private final InternalArtifactStoreManager internalArtifactStoreManager;
    private final ArtifactStoreWriter artifactStoreWriter;
    private final ArtifactStoreMerger artifactStoreMerger;
    private final ArtifactPublisherRedirector artifactPublisherRedirector;
    private final Map<String, ArtifactStorePublisherFactory> artifactStorePublisherFactories;
    private final Map<String, ArtifactStoreComparatorFactory> artifactStoreComparatorFactories;
    private final MavenModelReaderImpl mavenModelReader;

    public DefaultSession(
            SessionConfig sessionConfig,
            InternalArtifactStoreManagerFactory internalArtifactStoreManagerFactory,
            ArtifactStoreWriterFactory artifactStoreWriterFactory,
            ArtifactStoreMergerFactory artifactStoreMergerFactory,
            ArtifactPublisherRedirectorFactory artifactPublisherRedirectorFactory,
            Map<String, ArtifactStorePublisherFactory> artifactStorePublisherFactories,
            Map<String, ArtifactStoreComparatorFactory> artifactStoreComparatorFactories,
            MavenModelReaderImpl mavenModelReader) {
        super(sessionConfig);
        this.sessionBoundStoresKey = Session.class.getName() + "." + ArtifactStore.class + "." + UUID.randomUUID();
        this.internalArtifactStoreManager = internalArtifactStoreManagerFactory.create(sessionConfig);
        this.artifactStoreWriter = requireNonNull(artifactStoreWriterFactory).create(sessionConfig);
        this.artifactStoreMerger = requireNonNull(artifactStoreMergerFactory).create(sessionConfig);
        this.artifactPublisherRedirector =
                requireNonNull(artifactPublisherRedirectorFactory).create(this);
        this.artifactStorePublisherFactories = requireNonNull(artifactStorePublisherFactories);
        this.artifactStoreComparatorFactories = requireNonNull(artifactStoreComparatorFactories);
        this.mavenModelReader = requireNonNull(mavenModelReader);

        logger.info("Njord {} session created", sessionConfig.version());
    }

    @Override
    public SessionConfig config() {
        return config;
    }

    @Override
    public ArtifactStoreManager artifactStoreManager() {
        checkClosed();
        return internalArtifactStoreManager;
    }

    @Override
    public ArtifactStoreWriter artifactStoreWriter() {
        checkClosed();
        return artifactStoreWriter;
    }

    @Override
    public ArtifactStoreMerger artifactStoreMerger() {
        checkClosed();
        return artifactStoreMerger;
    }

    @Override
    public ArtifactPublisherRedirector artifactPublisherRedirector() {
        checkClosed();
        return artifactPublisherRedirector;
    }

    @Override
    public Collection<ArtifactStorePublisher> availablePublishers() {
        checkClosed();
        return artifactStorePublisherFactories.values().stream()
                .map(f -> f.create(this))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ArtifactStoreComparator> availableComparators() {
        checkClosed();
        return artifactStoreComparatorFactories.values().stream()
                .map(f -> f.create(this))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Model> readEffectiveModel(Artifact artifact, List<RemoteRepository> remoteRepositories) {
        requireNonNull(artifact);
        requireNonNull(remoteRepositories);
        checkClosed();

        try {
            ModelResponse response = mavenModelReader.readModel(
                    new DefaultRepositorySystemSession(config.session())
                            .setTransferListener(new NjordTransferListener()),
                    ModelRequest.builder()
                            .setArtifact(artifact)
                            .setRepositories(remoteRepositories)
                            .setRequestContext("njord")
                            .build());
            return Optional.ofNullable(response.getEffectiveModel());
        } catch (Exception e) {
            logger.warn("Could not read effective model: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public ArtifactStoreTemplate selectSessionArtifactStoreTemplate(String uri) {
        requireNonNull(uri);
        checkClosed();
        try {
            if (!uri.contains(":")) {
                if (uri.isEmpty()) {
                    // empty -> default IF project is available
                    if (config.currentProject().isPresent()) {
                        return internalArtifactStoreManager.defaultTemplate(
                                config.currentProject().orElseThrow(J8Utils.OET).repositoryMode());
                    } else {
                        throw new IllegalStateException(
                                "No project present, cannot deduce repository mode: specify template explicitly as `njord:template:<TEMPLATE>`");
                    }
                } else {
                    // non-empty -> template name
                    return selectTemplate(uri);
                }
            } else if (uri.startsWith("template:")) {
                // template:xxx
                return selectTemplate(uri.substring(9));
            } else if (uri.startsWith("store:")) {
                // store:xxx
                try (ArtifactStore artifactStore = internalArtifactStoreManager
                        .selectArtifactStore(uri.substring(6))
                        .orElseThrow(J8Utils.OET)) {
                    return artifactStore.template();
                }
            } else {
                throw new IllegalArgumentException("Invalid repository URI: " + uri);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean handleRemoteRepository(RemoteRepository repository) {
        requireNonNull(repository);
        checkClosed();
        // BEGIN: workaround for Maven 4.0.0-rc-5 (only)
        // IF: repoId is in form '[some string]-hex' (hex part is 40 char long; sha1)
        // THEN: chop it off, and check if "some string" ID repo is already present
        if (repository.getId().length() > 41
                && repository
                        .getId()
                        .substring(repository.getId().length() - 41)
                        .matches("-[0-9a-f]+")) {
            // case1: snapshot deploy tries to download metadata to get build number; ignore this
            if (repository.getPolicy(true).isEnabled()) {
                return false;
            }
            // case2: release deploy tries to download metadata for merging; ignore if match found
            String id = repository.getId().substring(0, repository.getId().length() - 41);
            return getSessionBoundStores().keySet().stream().noneMatch(r -> id.equals(r.getId()));
        }
        return true;
    }

    @Override
    public ArtifactStore getOrCreateSessionArtifactStore(RemoteRepository repository, String uri) {
        requireNonNull(repository);
        requireNonNull(uri);
        checkClosed();
        ConcurrentMap<RemoteRepository, String> sessionBoundStores = getSessionBoundStores();
        String storeName = sessionBoundStores.computeIfAbsent(repository, r -> {
            try {
                String artifactStoreName;
                boolean isNewArtifactStore = true;
                if (!uri.contains(":")) {
                    if (uri.isEmpty()) {
                        // empty -> default IF project is available
                        if (config.currentProject().isPresent()) {
                            artifactStoreName = createUsingTemplate(internalArtifactStoreManager
                                    .defaultTemplate(config.currentProject()
                                            .orElseThrow(J8Utils.OET)
                                            .repositoryMode())
                                    .name());
                        } else {
                            throw new IllegalStateException(
                                    "No project present, cannot deduce repository mode: specify template explicitly as `njord:template:<TEMPLATE>`");
                        }
                    } else {
                        // non-empty -> template name
                        artifactStoreName = createUsingTemplate(uri);
                    }
                } else if (uri.startsWith("template:")) {
                    // template:xxx
                    artifactStoreName = createUsingTemplate(uri.substring(9));
                } else if (uri.startsWith("store:")) {
                    isNewArtifactStore = false;
                    // store:xxx
                    artifactStoreName = uri.substring(6);
                    Optional<ArtifactStore> existingStore =
                            internalArtifactStoreManager.selectArtifactStore(artifactStoreName);
                    if (!existingStore.isPresent()) {
                        throw new IllegalArgumentException("Non existing store: " + artifactStoreName);
                    }
                    existingStore.orElseThrow(J8Utils.OET).close();
                } else {
                    throw new IllegalArgumentException("Invalid repository URI: " + uri);
                }
                if (isNewArtifactStore) {
                    logger.info("Created Njord artifact store {} for remote repository {}", artifactStoreName, r);
                }
                return artifactStoreName;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        try {
            return internalArtifactStoreManager.selectArtifactStore(storeName).orElseThrow(J8Utils.OET);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ArtifactStoreTemplate selectTemplate(String templateName) {
        List<ArtifactStoreTemplate> templates = internalArtifactStoreManager.listTemplates().stream()
                .filter(t -> t.name().equals(templateName))
                .collect(Collectors.toList());
        if (templates.size() != 1) {
            throw new IllegalArgumentException("Unknown template: " + templateName);
        } else {
            ArtifactStoreTemplate template = templates.get(0);
            if (config.prefix().isPresent()) {
                template = template.withPrefix(config.prefix().orElseThrow(J8Utils.OET));
            }
            return template;
        }
    }

    private String createUsingTemplate(String templateName) throws IOException {
        try (ArtifactStore artifactStore = internalArtifactStoreManager.createArtifactStore(
                selectTemplate(templateName),
                config.currentProject().isPresent()
                        ? config.currentProject().orElseThrow(J8Utils.OET).artifact()
                        : null)) {
            return artifactStore.name();
        }
    }

    @Override
    public int publishSessionArtifactStores() throws IOException {
        checkClosed();
        ConcurrentMap<RemoteRepository, String> sessionBoundStores = getSessionBoundStores();
        if (sessionBoundStores.isEmpty()) {
            return 0;
        }
        AtomicInteger result = new AtomicInteger(0);
        Optional<String> pno = artifactPublisherRedirector.getArtifactStorePublisherName();
        if (pno.isPresent()) {
            String publisherName = pno.orElseThrow(J8Utils.OET);
            Optional<ArtifactStorePublisher> po = selectArtifactStorePublisher(publisherName);
            if (po.isPresent()) {
                ArtifactStorePublisher p = po.orElseThrow(J8Utils.OET);
                for (String storeName : sessionBoundStores.values()) {
                    logger.info("Publishing {} with {}", storeName, publisherName);
                    try (ArtifactStore as = internalArtifactStoreManager
                            .selectArtifactStore(storeName)
                            .orElseThrow(J8Utils.OET)) {
                        if (!as.isEmpty()) {
                            p.publish(as);
                            result.addAndGet(1);
                        } else {
                            logger.warn("Skipping publishing of empty artifact store {}", storeName);
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException("Publisher not found: " + publisherName);
            }
        } else {
            throw new IllegalStateException("Publisher name was not specified nor could be discovered");
        }
        return result.get();
    }

    @Override
    public int dropSessionArtifactStores() {
        checkClosed();
        ConcurrentMap<RemoteRepository, String> sessionBoundStores = getSessionBoundStores();
        if (sessionBoundStores.isEmpty()) {
            return 0;
        }
        AtomicInteger result = new AtomicInteger(0);
        for (String storeName : sessionBoundStores.values()) {
            try {
                if (internalArtifactStoreManager.dropArtifactStore(storeName)) {
                    result.addAndGet(1);
                }
            } catch (IOException e) {
                logger.warn("Could not drop session bound ArtifactStore with name {}", storeName, e);
            }
        }
        return result.get();
    }

    @Override
    protected void doClose() throws IOException {
        internalArtifactStoreManager.close();
    }

    /**
     * Returns map of "Njord URI" to "storeName" that were created in current session.
     */
    @SuppressWarnings("unchecked")
    private ConcurrentMap<RemoteRepository, String> getSessionBoundStores() {
        return (ConcurrentHashMap<RemoteRepository, String>)
                config.session().getData().computeIfAbsent(sessionBoundStoresKey, ConcurrentHashMap::new);
    }
}
