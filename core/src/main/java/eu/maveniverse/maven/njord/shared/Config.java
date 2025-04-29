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
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Simple Njord configuration.
 */
public interface Config {
    RemoteRepository CENTRAL =
            new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/").build();

    String NJORD_AUTOPREFIX = "njord.autoprefix";

    String DEFAULT_NJORD_AUTOPREFIX = Boolean.TRUE.toString();

    String NJORD_PREFIX = "njord.prefix";

    String NJORD_TARGET = "njord.target";

    boolean enabled();

    Optional<String> version();

    Path basedir();

    Path propertiesPath();

    Map<String, String> userProperties();

    Map<String, String> systemProperties();

    Map<String, String> effectiveProperties();

    default Builder toBuilder() {
        return new Builder(
                enabled(), version().orElse(null), basedir(), propertiesPath(), userProperties(), systemProperties());
    }

    static Builder defaults() {
        return new Builder(
                null,
                Utils.discoverArtifactVersion(
                        Config.class.getClassLoader(), "eu.maveniverse.maven.njord", "core", null),
                discoverBaseDirectory(),
                Path.of("njord.properties"),
                new HashMap<>(),
                toMap(System.getProperties()));
    }

    static Builder daemonDefaults() {
        return defaults().propertiesPath(Path.of("daemon.properties"));
    }

    class Builder {
        private Boolean enabled;
        private final String version;
        private Path basedir;
        private Path propertiesPath;
        private Map<String, String> userProperties;
        private Map<String, String> systemProperties;

        private Builder(
                Boolean enabled,
                String version,
                Path basedir,
                Path propertiesPath,
                Map<String, String> userProperties,
                Map<String, String> systemProperties) {
            this.enabled = enabled;
            this.version = version;
            this.basedir = basedir;
            this.propertiesPath = propertiesPath;
            this.userProperties = new HashMap<>(userProperties);
            this.systemProperties = new HashMap<>(systemProperties);
        }

        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder basedir(Path basedir) {
            this.basedir = getCanonicalPath(basedir);
            return this;
        }

        public Builder propertiesPath(Path propertiesPath) {
            this.propertiesPath = requireNonNull(propertiesPath, "propertiesPath");
            return this;
        }

        public Builder userProperties(Map<String, String> userProperties) {
            this.userProperties = new HashMap<>(userProperties);
            return this;
        }

        public Builder setUserProperty(String key, String value) {
            requireNonNull(key, "key");
            requireNonNull(value, "value");
            this.userProperties.put(key, value);
            return this;
        }

        public Builder systemProperties(Map<String, String> systemProperties) {
            this.systemProperties = new HashMap<>(systemProperties);
            return this;
        }

        public Builder setSystemProperty(String key, String value) {
            requireNonNull(key, "key");
            requireNonNull(value, "value");
            this.systemProperties.put(key, value);
            return this;
        }

        public Config build() {
            return new Impl(enabled, version, basedir, propertiesPath, userProperties, systemProperties);
        }

        private static class Impl implements Config {
            private final Boolean enabled;
            private final String version;
            private final Path basedir;
            private final Path propertiesPath;
            private final Map<String, String> userProperties;
            private final Map<String, String> systemProperties;
            private final Map<String, String> effectiveProperties;

            private Impl(
                    Boolean enabled,
                    String version,
                    Path basedir,
                    Path propertiesPath,
                    Map<String, String> userProperties,
                    Map<String, String> systemProperties) {
                this.enabled = enabled;
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
            }

            @Override
            public boolean enabled() {
                return Objects.requireNonNullElseGet(
                        enabled,
                        () -> Boolean.parseBoolean(
                                effectiveProperties.getOrDefault("njord.enabled", Boolean.TRUE.toString())));
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
