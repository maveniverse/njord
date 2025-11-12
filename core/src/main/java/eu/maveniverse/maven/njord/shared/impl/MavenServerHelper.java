/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Helper class to mangle Maven Xpp3Dom/PlexusConfiguration stuff.
 */
public class MavenServerHelper {
    private MavenServerHelper() {}

    /**
     * Extracts Njord configuration from Maven Resolver configuration properties.
     * @param configProperties the Maven resolver config properties
     * @return A map containing a Map with configuration entries. The key of the outer map is the server id.
     * The key of the inner map is the configuration property name.
     */
    public static Map<String, Map<String, String>> extractServerConfigurations(Map<String, Object> configProperties) {
        return configProperties.entrySet().stream()
                .map(e -> {
                    final String serverId;
                    serverId = extractServerId(e.getKey());
                    if (serverId == null) {
                        return null;
                    }
                    Map<String, String> serviceConfiguration = extractNjordConfiguration(e.getValue(), serverId);
                    return new AbstractMap.SimpleEntry<>(serverId, serviceConfiguration);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Extracts the populating server id from a Maven Resolver configuration key.
     * @param configKey the configuration key
     * @return the server id to which the given key belongs or {@code null} if not belonging to a server section
     * @see <a href="https://github.com/apache/maven/blob/f06732524589040a63f8595bb4d1f24cca82d14c/impl/maven-core/src/main/java/org/apache/maven/internal/aether/DefaultRepositorySystemSessionFactory.java#L241">Maven 4 DefaultRepositorySystemSessionFactory</a>
     * @see <a href="https://github.com/apache/maven/blob/3e54c93a704957b63ee3494413a2b544fd3d825b/maven-core/src/main/java/org/apache/maven/internal/aether/DefaultRepositorySystemSessionFactory.java#L252">Maven 3 DefaultRepositorySystemSessionFactory</a>
     */
    protected static String extractServerId(String configKey) {
        final String serverId;
        if (configKey.startsWith("aether.transport.wagon.config.")) {
            serverId = configKey.substring("aether.transport.wagon.config.".length());
        } else if (configKey.startsWith("aether.connector.wagon.config.")) {
            serverId = configKey.substring("aether.connector.wagon.config.".length());
        } else {
            serverId = null;
        }
        return serverId;
    }

    /**
     * Extracts njord config from Plexus configuration set by Maven.
     */
    protected static Map<String, String> extractNjordConfiguration(Object configValue, String serverId) {
        PlexusConfiguration config;
        if (configValue instanceof PlexusConfiguration) {
            config = (PlexusConfiguration) configValue;
        } else if (configValue instanceof Xpp3Dom) {
            config = new XmlPlexusConfiguration((Xpp3Dom) configValue);
        } else {
            throw new IllegalArgumentException(
                    "unexpected configuration type: " + configValue.getClass().getName());
        }
        Map<String, String> serviceConfiguration = new HashMap<>(config.getChildCount() + 1);
        serviceConfiguration.put(SessionConfig.SERVER_ID_KEY, serverId);
        for (PlexusConfiguration child : config.getChildren()) {
            if (child.getName().startsWith(SessionConfig.KEY_PREFIX) && child.getValue() != null) {
                serviceConfiguration.put(child.getName(), child.getValue());
            }
        }
        return serviceConfiguration;
    }
}
