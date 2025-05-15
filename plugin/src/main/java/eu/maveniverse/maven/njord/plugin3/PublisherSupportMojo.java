/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.deploy.ArtifactDeployerRedirector;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Publisher support mojo.
 */
public abstract class PublisherSupportMojo extends NjordMojoSupport {
    @Inject
    private ArtifactDeployerRedirector artifactDeployerRedirector;

    /**
     * The name of the store to publish. If not given, Njord will try to figure it out: will look for existing
     * stores created from staging this project and will <em>choose latest (newest)</em> out of them if multiple
     * found.
     * <p>
     * This "heuristic" will work only if there is current project. While invoking mojo is possible outside a
     * project as well (validate and publish mojos does not require project), in such cases this parameter is
     * mandatory, as there will be no contextual information to choose store from.
     */
    @Parameter(property = "store")
    protected String store;

    /**
     * The name of the publisher to publish to. If not given, Njord will try to figure it out: it will look in
     * user properties, project properties (if available) and user Settings server configuration.
     * <p>
     * This "heuristic" will work only if there is current project. While invoking mojo is possible outside a
     * project as well (validate and publish mojos does not require project), in such cases this parameter is
     * mandatory, as there will be no contextual information to choose publisher from.
     */
    @Parameter(property = "publisher")
    protected String publisher;

    /**
     * Returns artifact store candidate names.
     * <ul>
     *     <li>if {@code store} parameter is set, it is returned as singleton list</li>
     *     <li>if {@link SessionConfig#prefix()} is present (so is set in user or project properties, if project is available), is used to look for existing stores using it</li>
     * </ul>
     */
    protected List<String> getArtifactStoreNameCandidates(Session ns) throws IOException {
        if (store == null && ns.config().prefix().isPresent()) {
            String prefix = ns.config().prefix().orElseThrow();
            return ns.artifactStoreManager().listArtifactStoreNamesForPrefix(prefix);
        }
        if (store != null) {
            return List.of(store);
        } else {
            return List.of();
        }
    }

    /**
     * Returns last (newest, last staged) Artifact Store name, if exists.
     */
    protected Optional<String> getArtifactStoreName(Session ns) throws IOException {
        List<String> storeNames = getArtifactStoreNameCandidates(ns);
        if (!storeNames.isEmpty()) {
            if (storeNames.size() == 1) {
                store = storeNames.get(0);
                logger.info("Found one store, using it: '{}'", store);
            } else {
                store = storeNames.get(storeNames.size() - 1);
                logger.info("Found multiple stores, using latest: '{}'", store);
            }
            return Optional.of(store);
        } else {
            return Optional.empty();
        }
    }

    protected ArtifactStore getArtifactStore(Session ns) throws IOException, MojoFailureException {
        Optional<String> storeName = getArtifactStoreName(ns);
        if (storeName.isEmpty()) {
            throw new MojoFailureException("ArtifactStore name was not specified nor could be found");
        }
        String store = storeName.orElseThrow();
        Optional<ArtifactStore> storeOptional = ns.artifactStoreManager().selectArtifactStore(store);
        if (storeOptional.isEmpty()) {
            logger.warn("ArtifactStore with given name not found: {}", store);
            throw new MojoFailureException("ArtifactStore with given name not found: " + store);
        }
        return storeOptional.orElseThrow();
    }

    /**
     * Returns publisher name, if possible.
     * <ul>
     *     <li>if {@code publisher} parameter is set, is returned</li>
     *     <li>if {@link SessionConfig#publisher()} is present (so is set in user or project properties, if project is available), is returned</li>
     *     <li>if there is project available, distribution management repository ID is checked is it publisher name, if yes, is returned</li>
     *     <li>if there is configuration in settings for distribution management repository ID server, it is checked and if configuration exists, is returned</li>
     * </ul>
     */
    protected Optional<String> getArtifactStorePublisherName(Session ns) {
        if (publisher != null) {
            return Optional.of(publisher);
        }
        return artifactDeployerRedirector.getArtifactStorePublisherName(ns.config());
    }

    protected ArtifactStorePublisher getArtifactStorePublisher(Session ns) throws MojoFailureException {
        Optional<String> publisherName = getArtifactStorePublisherName(ns);
        if (publisherName.isEmpty()) {
            throw new MojoFailureException("Publisher name was not specified nor could be discovered");
        }
        String publisher = publisherName.orElseThrow();
        Optional<ArtifactStorePublisher> po = ns.selectArtifactStorePublisher(publisher);
        if (po.isEmpty()) {
            throw new MojoFailureException("Publisher not found: " + publisher);
        }
        return po.orElseThrow();
    }
}
