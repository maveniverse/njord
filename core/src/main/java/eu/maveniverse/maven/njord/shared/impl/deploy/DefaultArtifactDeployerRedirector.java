/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.deploy;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.deploy.ArtifactDeployerRedirector;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.util.Map;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.repository.RemoteRepository;

@Singleton
@Named
public class DefaultArtifactDeployerRedirector extends ComponentSupport implements ArtifactDeployerRedirector {
    @Override
    public String getRepositoryUrl(Session ns, RemoteRepository repository) {
        String url = repository.getUrl();
        if (!url.startsWith(SessionConfig.NAME + ":")
                && ns.config().currentProject().isPresent()) {
            return getRepositoryUrl(
                    ns, repository, ns.config().currentProject().orElseThrow().repositoryMode());
        }
        return url;
    }

    @Override
    public String getRepositoryUrl(Session ns, RemoteRepository repository, RepositoryMode repositoryMode) {
        String url = repository.getUrl();
        Optional<Map<String, String>> sco = ns.config().serviceConfiguration(repository.getId());
        if (!url.startsWith(SessionConfig.NAME + ":") && sco.isPresent()) {
            Map<String, String> config = sco.orElseThrow();
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
}
