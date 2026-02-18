/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.deploy;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactorySupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirementsFactory;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;

/**
 * The "deploy" publisher deploy as "Maven would do", but it operates on staged artifact store, no need to
 * invoke the build for it.
 * <p>
 * It obeys one familiar property: <code>altDeploymentRepository</code> and very same syntax as Maven Deploy Plugin
 * <code>id::url</code>. This implies that one can "deploy publish" any artifact store with command like this:
 * <pre>
 *   $ mvn njord:publish -Dpublisher=deploy -DaltDeploymentRepository=myserver::myurl
 * </pre>
 * And it simply deploys to given repository "as Maven would do". In this case auth redirection is <em>inhibited</em>,
 * and Njord will use "myserver::url" as-is, even regarding authentication (again: as Maven would do). If auth
 * redirection still needed, use {@code -DaltDeploymentRepositoryAuthRedirect=true} (defaults to false IF
 * {@code DaltDeploymentRepository} is given).
 */
@Singleton
@Named(DeployPublisherFactory.NAME)
public class DeployPublisherFactory extends ArtifactStorePublisherFactorySupport
        implements ArtifactStorePublisherFactory {
    public static final String NAME = "deploy";

    @Inject
    public DeployPublisherFactory(
            RepositorySystem repositorySystem,
            Map<String, ArtifactStoreRequirementsFactory> artifactStoreRequirementsFactories) {
        super(repositorySystem, artifactStoreRequirementsFactories);
    }

    @Override
    protected ArtifactStorePublisher doCreate(Session session) {
        DeployPublisherConfig config = new DeployPublisherConfig(session.config());
        return new DeployPublisher(session, repositorySystem, config, createArtifactStoreRequirements(session, config));
    }
}
