/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared;

import static eu.maveniverse.maven.njord.shared.impl.Utils.toMap;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.impl.Utils;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.ConfigUtils;

/**
 * Session config holds all the session related data.
 */
public interface SessionConfig {
    String NAME = "njord";

    String KEY_PREFIX = NAME + ".";

    String CONFIG_ENABLED = KEY_PREFIX + "enabled";

    String CONFIG_DRY_RUN = KEY_PREFIX + "dryRun";

    String CONFIG_AUTOPREFIX = KEY_PREFIX + "autoprefix";

    String CONFIG_PREFIX = KEY_PREFIX + "prefix";

    String CONFIG_PUBLISHER = KEY_PREFIX + "publisher";

    String CONFIG_RELEASE_URL = KEY_PREFIX + "releaseUrl";

    String CONFIG_SNAPSHOT_URL = KEY_PREFIX + "snapshotUrl";

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
    Optional<String> version();

    /**
     * Njord basedir, where all the config and locally staged repositories are.
     */
    Path basedir();

    /**
     * The property path to load, defaults to {@code njord.properties} in {@link #basedir()}.
     */
    Path propertiesPath();

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
    default List<RemoteRepository> allRemoteRepositories() {
        ArrayList<RemoteRepository> remoteRepositories = new ArrayList<>(remoteRepositories());
        if (currentProject().isPresent()) {
            remoteRepositories.addAll(currentProject().orElseThrow().remoteRepositories());
        }
        return remoteRepositories;
    }

    /**
     * Whether to apply "auto prefix" (user provided one or derived from current project) or use "template prefix" for
     * created store names. Defaults to {@code true}: when {@link #currentProject()} is available, store prefix is
     * derived from the project, otherwise from template.
     */
    default boolean autoPrefix() {
        String value = effectiveProperties().get(CONFIG_AUTOPREFIX);
        if (value == null && currentProject().isPresent()) {
            CurrentProject cp = currentProject().orElseThrow();
            value = cp.projectProperties().get(CONFIG_AUTOPREFIX);
        }
        return value == null || Boolean.parseBoolean(value);
    }

    /**
     * The prefix to override template prefix, if needed. If {@link #autoPrefix()} is {@code true}, this value is always
     * present if there is present {@link #currentProject()}.
     * <p>
     * User may specify it in user properties, like in {@code .mvn/maven.config} or CLI, but also in top level
     * POM as project property.
     */
    default Optional<String> prefix() {
        String value = effectiveProperties().get(CONFIG_PREFIX);
        if (value == null && currentProject().isPresent()) {
            CurrentProject cp = currentProject().orElseThrow();
            value = cp.projectProperties().get(CONFIG_PREFIX);
            if (value == null && autoPrefix()) {
                value = cp.artifact().getArtifactId();
            }
        }
        return Optional.ofNullable(value);
    }

    /**
     * The publisher to use, if specified.
     * <p>
     * User may specify it in user properties, like in {@code .mvn/maven.config} or CLI, but also in top level
     * POM as project property.
     */
    default Optional<String> publisher() {
        String value = effectiveProperties().get(CONFIG_PUBLISHER);
        if (value == null && currentProject().isPresent()) {
            CurrentProject cp = currentProject().orElseThrow();
            value = cp.projectProperties().get(CONFIG_PUBLISHER);
        }
        return Optional.ofNullable(value);
    }

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
    }

    /**
     * The current project, if available. If Maven invoked from a directory where no project is available,
     * this field with be empty.
     */
    Optional<CurrentProject> currentProject();

    /**
     * Returns the "service configuration" for given service ID.
     */
    default Optional<Map<String, String>> serviceConfiguration(String serviceId) {
        requireNonNull(serviceId);
        Object configuration = ConfigUtils.getObject(
                session(),
                null,
                "aether.connector.wagon.config." + serviceId,
                "aether.transport.wagon.config." + serviceId);
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
            HashMap<String, String> serviceConfiguration = new HashMap<>();
            for (PlexusConfiguration child : config.getChildren()) {
                if (child.getName().startsWith(KEY_PREFIX) && child.getValue() != null) {
                    serviceConfiguration.put(child.getName(), child.getValue());
                }
            }
            return Optional.of(serviceConfiguration);
        }
        return Optional.empty();
    }

    /**
     * Returns this instance as builder.
     */
    default Builder toBuilder() {
        return new Builder(
                enabled(),
                dryRun(),
                version().orElse(null),
                basedir(),
                propertiesPath(),
                userProperties(),
                systemProperties(),
                session(),
                remoteRepositories(),
                currentProject().orElse(null));
    }

    /**
     * Creates {@link CurrentProject} out of passed in project. May return {@code null}.
     */
    static CurrentProject fromMavenProject(MavenProject project) {
        requireNonNull(project);
        if (!"org.apache.maven:standalone-pom".equals(project.getGroupId() + ":" + project.getArtifactId())) {
            final Artifact artifact = RepositoryUtils.toArtifact(project.getArtifact());
            final Map<String, String> properties = Map.copyOf(Utils.toMap(project.getProperties()));
            final List<RemoteRepository> remoteRepositories = project.getRemoteProjectRepositories();
            final Map<RepositoryMode, RemoteRepository> dmr = new HashMap<>();
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
            final Map<RepositoryMode, RemoteRepository> distributionManagementRepositories = Map.copyOf(dmr);
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
            };
        }
        return null;
    }

    /**
     * Creates builder with defaults.
     */
    static Builder defaults(RepositorySystemSession session, List<RemoteRepository> remoteRepositories) {
        requireNonNull(session, "session");
        requireNonNull(remoteRepositories, "remoteRepositories");
        return new Builder(
                ConfigUtils.getBoolean(session, true, CONFIG_ENABLED),
                ConfigUtils.getBoolean(session, false, CONFIG_DRY_RUN),
                Utils.discoverArtifactVersion(
                        SessionConfig.class.getClassLoader(), "eu.maveniverse.maven.njord", "core", null),
                discoverBaseDirectory(),
                Path.of("njord.properties"),
                session.getSystemProperties(),
                session.getUserProperties(),
                session,
                remoteRepositories,
                null);
    }

    class Builder {
        private boolean enabled;
        private boolean dryRun;
        private final String version;
        private Path basedir;
        private Path propertiesPath;
        private Map<String, String> userProperties;
        private Map<String, String> systemProperties;
        private RepositorySystemSession session;
        private List<RemoteRepository> remoteRepositories;
        private CurrentProject currentProject;

        public Builder(
                boolean enabled,
                boolean dryRun,
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
            private final Map<String, String> userProperties;
            private final Map<String, String> systemProperties;
            private final Map<String, String> effectiveProperties;
            private final RepositorySystemSession session;
            private final List<RemoteRepository> remoteRepositories;
            private final CurrentProject currentProject;

            private Impl(
                    boolean enabled,
                    boolean dryRun,
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
                this.basedir = requireNonNull(basedir, "basedir");
                requireNonNull(propertiesPath, "propertiesPath");
                this.propertiesPath = getCanonicalPath(basedir.resolve(propertiesPath));

                Properties properties = new Properties();
                if (Files.isRegularFile(this.propertiesPath)) {
                    try (InputStream inputStream = Files.newInputStream(this.propertiesPath)) {
                        properties.load(inputStream);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                this.userProperties = Map.copyOf(requireNonNull(userProperties, "userProperties"));
                this.systemProperties = Map.copyOf(requireNonNull(systemProperties, "systemProperties"));
                HashMap<String, String> eff = new HashMap<>();
                eff.putAll(systemProperties);
                eff.putAll(toMap(properties));
                eff.putAll(userProperties);
                this.effectiveProperties = Map.copyOf(eff);

                this.session = requireNonNull(session);
                this.remoteRepositories = List.copyOf(requireNonNull(remoteRepositories));
                this.currentProject = currentProject;
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
            public Optional<String> version() {
                return Optional.ofNullable(version);
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
            public Optional<CurrentProject> currentProject() {
                return Optional.ofNullable(currentProject);
            }
        }
    }

    static Path discoverBaseDirectory() {
        String basedir = System.getProperty("njord.basedir");
        if (basedir == null) {
            return getCanonicalPath(discoverUserHomeDirectory().resolve(".njord"));
        }
        return getCanonicalPath(Path.of(System.getProperty("user.dir")).resolve(basedir));
    }

    static Path discoverUserHomeDirectory() {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            throw new IllegalStateException("requires user.home Java System Property set");
        }
        return getCanonicalPath(Path.of(userHome));
    }

    static Path getCanonicalPath(Path path) {
        requireNonNull(path, "path");
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return getCanonicalPath(path.getParent()).resolve(path.getFileName());
        }
    }
}
