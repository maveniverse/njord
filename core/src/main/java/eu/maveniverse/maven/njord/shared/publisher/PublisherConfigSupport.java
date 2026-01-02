/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.impl.ResolverUtils;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.ConfigUtils;

/**
 * Publisher config support class. The basic support for all publisher configuration, supports artifact store requirements
 * and target and service repositories. Target repositories are those configured by user explicitly or via project.
 * Service repositories are those where actual publishing happens. In some cases these two are completely equal (are same)
 * like in case of deploy, but in some cases, like Sonatype CP they are different: to publish to central (one URL)
 * you in fact use some other service URL. The target repository IDs play important role, as they are used to source
 * auth, so they are reused when service repositories are being altered.
 */
public abstract class PublisherConfigSupport {
    /**
     * The Maven deploy option to override deployment repository. To be used by those publishers that can
     * support it. It is NOT supported by all publishers.
     */
    protected static final String PROP_ALT_DEPLOYMENT_REPOSITORY = "altDeploymentRepository";

    /**
     * The Maven deploy option to override release deployment repository. To be used by those publishers that can
     * support it. It is NOT supported by all publishers.
     */
    protected static final String PROP_ALT_RELEASE_DEPLOYMENT_REPOSITORY = "altReleaseDeploymentRepository";

    /**
     * The Maven deploy option to override snapshot deployment repository. To be used by those publishers that can
     * support it. It is NOT supported by all publishers.
     */
    protected static final String PROP_ALT_SNAPSHOT_DEPLOYMENT_REPOSITORY = "altSnapshotDeploymentRepository";

    protected final String name;
    protected final SessionConfig sessionConfig;
    protected final String artifactStoreRequirements;

    protected final RemoteRepository targetReleaseRepository;
    protected final RemoteRepository targetSnapshotRepository;
    protected final RemoteRepository serviceReleaseRepository;
    protected final RemoteRepository serviceSnapshotRepository;

    public PublisherConfigSupport(String name, SessionConfig sessionConfig) {
        this.name = requireNonNull(name);
        this.sessionConfig = requireNonNull(sessionConfig);
        this.artifactStoreRequirements = ConfigUtils.getString(
                sessionConfig.effectiveProperties(),
                ArtifactStoreRequirements.NONE.name(),
                keyNames("artifactStoreRequirements"));

        this.targetReleaseRepository = createTargetReleaseRepository();
        this.targetSnapshotRepository = createTargetSnapshotRepository();
        this.serviceReleaseRepository = createServiceReleaseRepository();
        this.serviceSnapshotRepository = createServiceSnapshotRepository();
    }

    protected String keyName(String property) {
        requireNonNull(property);
        return "njord.publisher." + name + "." + property;
    }

    protected String[] keyNames(String property) {
        return new String[] {keyName(property), SessionConfig.KEY_PREFIX + property};
    }

    protected String repositoryId(RepositoryMode mode, String defaultRepositoryId) {
        requireNonNull(mode);
        if (sessionConfig.currentProject().isPresent()) {
            RemoteRepository repository = sessionConfig
                    .currentProject()
                    .orElseThrow(J8Utils.OET)
                    .distributionManagementRepositories()
                    .get(mode);
            if (repository != null) {
                return repository.getId();
            }
        }
        String property = mode == RepositoryMode.RELEASE ? "releaseRepositoryId" : "snapshotRepositoryId";
        return ConfigUtils.getString(sessionConfig.effectiveProperties(), defaultRepositoryId, keyName(property));
    }

    protected final RemoteRepository createTargetReleaseRepository() {
        RemoteRepository releaseRepository = null;
        if (sessionConfig.effectiveProperties().containsKey(PROP_ALT_DEPLOYMENT_REPOSITORY)) {
            RemoteRepository bare = ResolverUtils.parseRemoteRepositoryString(
                    sessionConfig.effectiveProperties().get(PROP_ALT_DEPLOYMENT_REPOSITORY));
            releaseRepository = new RemoteRepository.Builder(bare)
                    .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                    .build();
        } else if (sessionConfig.effectiveProperties().containsKey(PROP_ALT_RELEASE_DEPLOYMENT_REPOSITORY)) {
            RemoteRepository bare = ResolverUtils.parseRemoteRepositoryString(
                    sessionConfig.effectiveProperties().get(PROP_ALT_RELEASE_DEPLOYMENT_REPOSITORY));
            releaseRepository = new RemoteRepository.Builder(bare)
                    .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                    .build();
        } else if (sessionConfig.currentProject().isPresent()) {
            SessionConfig.CurrentProject project =
                    sessionConfig.currentProject().orElseThrow(J8Utils.OET);
            releaseRepository = project.distributionManagementRepositories().get(RepositoryMode.RELEASE);
        }
        return releaseRepository;
    }

    protected final RemoteRepository createTargetSnapshotRepository() {
        RemoteRepository snapshotRepository = null;
        if (sessionConfig.effectiveProperties().containsKey(PROP_ALT_DEPLOYMENT_REPOSITORY)) {
            RemoteRepository bare = ResolverUtils.parseRemoteRepositoryString(
                    sessionConfig.effectiveProperties().get(PROP_ALT_DEPLOYMENT_REPOSITORY));
            snapshotRepository = new RemoteRepository.Builder(bare)
                    .setReleasePolicy(new RepositoryPolicy(false, null, null))
                    .build();
        } else if (sessionConfig.effectiveProperties().containsKey(PROP_ALT_SNAPSHOT_DEPLOYMENT_REPOSITORY)) {
            RemoteRepository bare = ResolverUtils.parseRemoteRepositoryString(
                    sessionConfig.effectiveProperties().get(PROP_ALT_SNAPSHOT_DEPLOYMENT_REPOSITORY));
            snapshotRepository = new RemoteRepository.Builder(bare)
                    .setReleasePolicy(new RepositoryPolicy(false, null, null))
                    .build();
        } else if (sessionConfig.currentProject().isPresent()) {
            SessionConfig.CurrentProject project =
                    sessionConfig.currentProject().orElseThrow(J8Utils.OET);
            snapshotRepository = project.distributionManagementRepositories().get(RepositoryMode.SNAPSHOT);
        }
        return snapshotRepository;
    }

    protected RemoteRepository createServiceReleaseRepository() {
        return targetReleaseRepository;
    }

    protected RemoteRepository createServiceSnapshotRepository() {
        return targetSnapshotRepository;
    }

    public String artifactStoreRequirements() {
        return artifactStoreRequirements;
    }

    public RemoteRepository targetReleaseRepository() {
        return targetReleaseRepository;
    }

    public RemoteRepository targetSnapshotRepository() {
        return targetSnapshotRepository;
    }

    public RemoteRepository serviceReleaseRepository() {
        return serviceReleaseRepository;
    }

    public RemoteRepository serviceSnapshotRepository() {
        return serviceSnapshotRepository;
    }
}
