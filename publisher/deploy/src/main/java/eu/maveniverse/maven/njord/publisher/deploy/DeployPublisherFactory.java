/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.deploy;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

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
public class DeployPublisherFactory implements ArtifactStorePublisherFactory {
    public static final String NAME = "deploy";

    private static final String PROP_ALT_DEPLOYMENT_REPOSITORY = "altDeploymentRepository";
    private static final String PROP_ALT_DEPLOYMENT_REPOSITORY_AUTH_REDIRECT = "altDeploymentRepositoryAuthRedirect";

    private final RepositorySystem repositorySystem;

    @Inject
    public DeployPublisherFactory(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public ArtifactStorePublisher create(Session session) {
        requireNonNull(session);

        RemoteRepository releasesRepository = null;
        RemoteRepository snapshotsRepository = null;
        boolean followAuthRedirection = true;
        if (session.config().effectiveProperties().containsKey(PROP_ALT_DEPLOYMENT_REPOSITORY)) {
            String altDeploymentRepository =
                    session.config().effectiveProperties().get(PROP_ALT_DEPLOYMENT_REPOSITORY);
            String[] split = altDeploymentRepository.split("::");
            if (split.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid alt deployment repository syntax (supported is id::url): " + altDeploymentRepository);
            }
            String id = split[0];
            String url = split[1];
            releasesRepository = new RemoteRepository.Builder(id, "default", url)
                    .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                    .build();
            snapshotsRepository = new RemoteRepository.Builder(id, "default", url)
                    .setReleasePolicy(new RepositoryPolicy(false, null, null))
                    .build();
            followAuthRedirection = Boolean.parseBoolean(session.config()
                    .effectiveProperties()
                    .getOrDefault(
                            PROP_ALT_DEPLOYMENT_REPOSITORY_AUTH_REDIRECT,
                            "false")); // user specified explicitly what he wants here, but may override it
        } else if (session.config().currentProject().isPresent()) {
            SessionConfig.CurrentProject project =
                    session.config().currentProject().orElseThrow(J8Utils.OET);
            releasesRepository = project.distributionManagementRepositories().get(RepositoryMode.RELEASE);
            snapshotsRepository = project.distributionManagementRepositories().get(RepositoryMode.SNAPSHOT);
        }

        return new DeployPublisher(
                session,
                repositorySystem,
                releasesRepository,
                snapshotsRepository,
                ArtifactStoreRequirements.NONE,
                followAuthRedirection);
    }
}
