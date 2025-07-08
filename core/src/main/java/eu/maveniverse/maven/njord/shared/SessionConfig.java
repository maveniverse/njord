/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import eu.maveniverse.maven.shared.core.maven.MavenUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

/**
 * Session config holds all the session related data.
 */
public interface SessionConfig {
    String NAME = "njord";

    String KEY_PREFIX = NAME + ".";

    String UNKNOWN_VERSION = "unknown";

    /**
     * Configuration key in properties (system, user or project) to enable/disable Njord. Defaults to {@code true}.
     */
    String CONFIG_ENABLED = KEY_PREFIX + "enabled";

    /**
     * Configuration key in properties (system, user or project) for "dry run". Defaults to {@code false}.
     */
    String CONFIG_DRY_RUN = KEY_PREFIX + "dryRun";

    /**
     * Configuration key in properties (system, user or project) for "auto publish": publish automatically all stores
     * created within session at the end of session. Defaults to {@code false}.
     */
    String CONFIG_AUTO_PUBLISH = KEY_PREFIX + "autoPublish";

    /**
     * Configuration key in properties (system, user or project) for "auto drop". Defaults to {@code true}.
     */
    String CONFIG_AUTO_DROP = KEY_PREFIX + "autoDrop";

    /**
     * Configuration key in properties (system, user or project) for explicitly set prefix to use.
     * If there is a project in context, prefix will be automatically set to top level project artifact id.
     */
    String CONFIG_PREFIX = KEY_PREFIX + "prefix";

    /**
     * Configuration key in {@code settings/servers/server/configuration} or properties (system, user or project)
     * for publisher to use with this server.
     */
    String CONFIG_PUBLISHER = KEY_PREFIX + "publisher";

    /**
     * Configuration key in {@code settings/servers/server/configuration} for release Njord URL.
     */
    String CONFIG_RELEASE_URL = KEY_PREFIX + "releaseUrl";

    /**
     * Configuration key in {@code settings/servers/server/configuration} for snapshot Njord URL.
     */
    String CONFIG_SNAPSHOT_URL = KEY_PREFIX + "snapshotUrl";

    /**
     * Configuration key in {@code settings/servers/server/configuration} for authentication redirect. If this
     * key is present, Njord will take authentication from redirected server entry instead.
     */
    String CONFIG_AUTH_REDIRECT = KEY_PREFIX + "authRedirect";

    /**
     * Configuration key in {@code settings/servers/server/configuration} for service redirect. If this
     * key is present, all other keys are ignored and Njord will take service config from redirected server entry
     * instead.
     */
    String CONFIG_SERVICE_REDIRECT = KEY_PREFIX + "serviceRedirect";

    /**
     * Is Njord enabled? If this method returns {@code false}, Njord will step aside (like it was not loaded).
     */
    boolean enabled();

    /**
     * If this returns {@code true}, no any kind of "irrevocable" operation will happen.
     */
    boolean dryRun();

    /**
     * Returns the Njord version.
     */
    String version();

    /**
     * Njord basedir, where all the config and locally staged repositories are.
     */
    Path basedir();

    /**
     * The property path to load, defaults to {@code njord.properties} in {@link #basedir()}.
     */
    Path propertiesPath();

    /**
     * Properties defined in {@link #propertiesPath()} properties file.
     */
    Map<String, String> njordProperties();

    /**
     * User properties set in environment.
     */
    Map<String, String> userProperties();

    /**
     * System properties set in environment.
     */
    Map<String, String> systemProperties();

    /**
     * Effective properties that should be used to get configuration from (applies precedence).
     */
    Map<String, String> effectiveProperties();

    /**
     * Resolver session, never {@code null}.
     */
    RepositorySystemSession session();

    /**
     * Remote repositories provided by environment, never {@code null}.
     */
    List<RemoteRepository> remoteRepositories();

    /**
     * Remote repositories provided by environment and project, if present, never {@code null}.
     */
    List<RemoteRepository> allRemoteRepositories();

    /**
     * Whether to automatically publish session created stores. Defaults to {@code false}.
     *
     * @see #CONFIG_AUTO_PUBLISH
     */
    boolean autoPublish();

    /**
     * Whether to automatically drop session created stores IF {@link #autoPublish()} is {@code true}.
     * Defaults to {@code true}.
     *
     * @see #CONFIG_AUTO_DROP
     */
    boolean autoDrop();

    /**
     * The prefix to override template prefix, if needed. This value is always
     * present if there is present {@link #currentProject()}.
     * <p>
     * User may specify it in user properties, like in {@code .mvn/maven.config} or CLI, but also in top level
     * POM as project property.
     *
     * @see #CONFIG_PREFIX
     */
    Optional<String> prefix();

    /**
     * Shim for "current project". Provides needed information from project.
     */
    interface CurrentProject {
        /**
         * The artifact of the project (bare coordinates), never {@code null}.
         */
        Artifact artifact();

        /**
         * The repository mode of project (in function of {@link #artifact()}, never {@code null}.
         */
        default RepositoryMode repositoryMode() {
            return artifact().isSnapshot() ? RepositoryMode.SNAPSHOT : RepositoryMode.RELEASE;
        }

        /**
         * The project properties.
         */
        Map<String, String> projectProperties();

        /**
         * The defined remote repositories, never {@code null}.
         */
        List<RemoteRepository> remoteRepositories();

        /**
         * The defined project distribution management repositories, never {@code null}.
         */
        Map<RepositoryMode, RemoteRepository> distributionManagementRepositories();

        /**
         * Returns the project {@code /target} directory.
         */
        Path buildDirectory();
    }

    /**
     * The current project, if available. If Maven invoked from a directory where no project is available,
     * this field with be empty.
     */
    Optional<CurrentProject> currentProject();

    /**
     * Returns this instance as builder.
     */
    default Builder toBuilder() {
        return new Builder(
                enabled(),
                dryRun(),
                version(),
                basedir(),
                propertiesPath(),
                userProperties(),
                systemProperties(),
                session(),
                remoteRepositories(),
                currentProject().orElse(null));
    }

    /**
     * Creates builder with defaults.
     */
    static Builder defaults(RepositorySystemSession session, List<RemoteRepository> remoteRepositories) {
        requireNonNull(session, "session");
        requireNonNull(remoteRepositories, "remoteRepositories");
        return new Builder(
                null,
                null,
                MavenUtils.discoverArtifactVersion(
                        SessionConfig.class.getClassLoader(), "eu.maveniverse.maven.njord", "core", UNKNOWN_VERSION),
                null,
                null,
                session.getUserProperties(),
                session.getSystemProperties(),
                session,
                remoteRepositories,
                null);
    }

    /**
     * Creates {@link CurrentProject} out of passed in project. May return {@code null}.
     */
    static CurrentProject fromMavenProject(MavenProject project) {
        requireNonNull(project);
        if (!"org.apache.maven:standalone-pom".equals(project.getGroupId() + ":" + project.getArtifactId())) {
            final Artifact artifact = RepositoryUtils.toArtifact(project.getArtifact());
            final Map<String, String> properties = J8Utils.copyOf(MavenUtils.toMap(project.getProperties()));
            final List<RemoteRepository> remoteRepositories = J8Utils.copyOf(project.getRemoteProjectRepositories());
            final Map<RepositoryMode, RemoteRepository> dmr = new HashMap<>();
            final Path buildDirectory = Paths.get(project.getBuild().getDirectory());
            if (project.getDistributionManagement() != null) {
                DeploymentRepository dr = project.getDistributionManagement().getRepository();
                if (dr != null) {
                    dmr.put(
                            RepositoryMode.RELEASE,
                            new RemoteRepository.Builder(dr.getId(), "default", dr.getUrl())
                                    .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
                                    .build());
                }
                dr = project.getDistributionManagement().getSnapshotRepository();
                if (dr != null) {
                    dmr.put(
                            RepositoryMode.SNAPSHOT,
                            new RemoteRepository.Builder(dr.getId(), "default", dr.getUrl())
                                    .setReleasePolicy(new RepositoryPolicy(false, null, null))
                                    .build());
                }
            }
            final Map<RepositoryMode, RemoteRepository> distributionManagementRepositories = J8Utils.copyOf(dmr);
            return new SessionConfig.CurrentProject() {
                @Override
                public Artifact artifact() {
                    return artifact;
                }

                @Override
                public Map<String, String> projectProperties() {
                    return properties;
                }

                @Override
                public List<RemoteRepository> remoteRepositories() {
                    return remoteRepositories;
                }

                @Override
                public Map<RepositoryMode, RemoteRepository> distributionManagementRepositories() {
                    return distributionManagementRepositories;
                }

                @Override
                public Path buildDirectory() {
                    return buildDirectory;
                }
            };
        }
        return null;
    }

    class Builder {
        private Boolean enabled;
        private Boolean dryRun;
        private final String version;
        private Path basedir;
        private Path propertiesPath;
        private Map<String, String> userProperties;
        private Map<String, String> systemProperties;
        private RepositorySystemSession session;
        private List<RemoteRepository> remoteRepositories;
        private CurrentProject currentProject;

        public Builder(
                Boolean enabled,
                Boolean dryRun,
                String version,
                Path basedir,
                Path propertiesPath,
                Map<String, String> userProperties,
                Map<String, String> systemProperties,
                RepositorySystemSession session,
                List<RemoteRepository> remoteRepositories,
                CurrentProject currentProject) {
            this.enabled = enabled;
            this.dryRun = dryRun;
            this.version = version;
            this.basedir = basedir;
            this.propertiesPath = propertiesPath;
            this.userProperties = userProperties;
            this.systemProperties = systemProperties;
            this.session = session;
            this.remoteRepositories = remoteRepositories;
            this.currentProject = currentProject;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        public Builder basedir(Path basedir) {
            this.basedir = requireNonNull(basedir);
            return this;
        }

        public Builder propertiesPath(Path propertiesPath) {
            this.propertiesPath = requireNonNull(propertiesPath);
            return this;
        }

        public Builder userProperties(Map<String, String> userProperties) {
            this.userProperties = requireNonNull(userProperties);
            return this;
        }

        public Builder systemProperties(Map<String, String> systemProperties) {
            this.systemProperties = requireNonNull(systemProperties);
            return this;
        }

        public Builder session(RepositorySystemSession session) {
            this.session = requireNonNull(session);
            return this;
        }

        public Builder remoteRepositories(List<RemoteRepository> remoteRepositories) {
            this.remoteRepositories = requireNonNull(remoteRepositories);
            return this;
        }

        public Builder currentProject(CurrentProject currentProject) {
            this.currentProject = currentProject;
            return this;
        }

        public SessionConfig build() {
            return new Impl(
                    enabled,
                    dryRun,
                    version,
                    basedir,
                    propertiesPath,
                    userProperties,
                    systemProperties,
                    session,
                    remoteRepositories,
                    currentProject);
        }

        private static class Impl implements SessionConfig {
            private final boolean enabled;
            private final boolean dryRun;
            private final String version;
            private final Path basedir;
            private final Path propertiesPath;
            private final Map<String, String> njordProperties;
            private final Map<String, String> userProperties;
            private final Map<String, String> systemProperties;
            private final Map<String, String> effectiveProperties;
            private final RepositorySystemSession session;
            private final List<RemoteRepository> remoteRepositories;
            private final List<RemoteRepository> allRemoteRepositories;
            private final boolean autoPublish;
            private final boolean autoDrop;
            private final String prefix;
            private final CurrentProject currentProject;

            private Impl(
                    Boolean enabled,
                    Boolean dryRun,
                    String version,
                    Path basedir,
                    Path propertiesPath,
                    Map<String, String> userProperties,
                    Map<String, String> systemProperties,
                    RepositorySystemSession session,
                    List<RemoteRepository> remoteRepositories,
                    CurrentProject currentProject) {
                this.version = requireNonNull(version, "version");
                this.currentProject = currentProject; // nullable

                this.basedir = basedir == null
                        ? FileUtils.discoverBaseDirectory("njord.basedir", ".njord")
                        : FileUtils.canonicalPath(basedir);
                if (!Files.isDirectory(this.basedir)) {
                    try {
                        Files.createDirectories(this.basedir);
                    } catch (IOException e) {
                        throw new UncheckedIOException("Cannot create basedir", e);
                    }
                }
                this.propertiesPath = propertiesPath == null
                        ? this.basedir.resolve("njord.properties")
                        : FileUtils.canonicalPath(this.basedir.resolve(propertiesPath));

                Properties njordProperties = new Properties();
                if (Files.isRegularFile(this.propertiesPath)) {
                    try (InputStream inputStream = Files.newInputStream(this.propertiesPath)) {
                        njordProperties.load(inputStream);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                this.njordProperties = MavenUtils.toMap(njordProperties);
                this.userProperties = J8Utils.copyOf(requireNonNull(userProperties, "userProperties"));
                this.systemProperties = J8Utils.copyOf(requireNonNull(systemProperties, "systemProperties"));
                HashMap<String, String> eff = new HashMap<>();
                eff.putAll(this.systemProperties);
                eff.putAll(this.njordProperties);
                if (currentProject != null) {
                    eff.putAll(currentProject.projectProperties());
                }
                eff.putAll(this.userProperties);
                this.effectiveProperties = J8Utils.copyOf(eff);

                this.enabled = enabled != null
                        ? enabled
                        : Boolean.parseBoolean(
                                effectiveProperties.getOrDefault(CONFIG_ENABLED, Boolean.TRUE.toString()));
                this.dryRun = dryRun != null
                        ? dryRun
                        : Boolean.parseBoolean(
                                effectiveProperties.getOrDefault(CONFIG_DRY_RUN, Boolean.FALSE.toString()));
                this.session = requireNonNull(session);
                this.remoteRepositories = J8Utils.copyOf(requireNonNull(remoteRepositories));
                ArrayList<RemoteRepository> arr = new ArrayList<>(remoteRepositories);
                if (currentProject != null) {
                    arr.addAll(currentProject.remoteRepositories());
                }
                this.allRemoteRepositories = J8Utils.copyOf(arr);

                this.autoPublish = Boolean.parseBoolean(
                        effectiveProperties.getOrDefault(CONFIG_AUTO_PUBLISH, Boolean.FALSE.toString()));
                this.autoDrop = Boolean.parseBoolean(
                        effectiveProperties.getOrDefault(CONFIG_AUTO_DROP, Boolean.TRUE.toString()));

                String prefixString = effectiveProperties.get(CONFIG_PREFIX);
                if (prefixString == null && currentProject != null) {
                    prefixString = currentProject.artifact().getArtifactId();
                }
                this.prefix = prefixString;
            }

            @Override
            public boolean enabled() {
                return enabled;
            }

            @Override
            public boolean dryRun() {
                return dryRun;
            }

            @Override
            public String version() {
                return version;
            }

            @Override
            public Path basedir() {
                return basedir;
            }

            @Override
            public Path propertiesPath() {
                return propertiesPath;
            }

            @Override
            public Map<String, String> njordProperties() {
                return njordProperties;
            }

            @Override
            public Map<String, String> userProperties() {
                return userProperties;
            }

            @Override
            public Map<String, String> systemProperties() {
                return systemProperties;
            }

            @Override
            public Map<String, String> effectiveProperties() {
                return effectiveProperties;
            }

            @Override
            public RepositorySystemSession session() {
                return session;
            }

            @Override
            public List<RemoteRepository> remoteRepositories() {
                return remoteRepositories;
            }

            @Override
            public List<RemoteRepository> allRemoteRepositories() {
                return allRemoteRepositories;
            }

            @Override
            public boolean autoPublish() {
                return autoPublish;
            }

            public boolean autoDrop() {
                return autoDrop;
            }

            @Override
            public Optional<String> prefix() {
                return Optional.ofNullable(prefix);
            }

            @Override
            public Optional<CurrentProject> currentProject() {
                return Optional.ofNullable(currentProject);
            }
        }
    }
}
