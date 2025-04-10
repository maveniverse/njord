/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.stream.Collectors;

public final class Utils {
    private Utils() {}

    /**
     * Converts passed in {@link Properties} to mutable plain {@link HashMap}.
     */
    public static HashMap<String, String> toMap(Properties properties) {
        requireNonNull(properties, "properties");
        return properties.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> String.valueOf(e.getValue()),
                        (prev, next) -> next,
                        HashMap::new));
    }

    /**
     * Discovers artifact version.
     */
    public static String discoverArtifactVersion(
            ClassLoader classLoader, String groupId, String artifactId, String defVersion) {
        String version = defVersion;
        String resource = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        final Properties props = new Properties();
        try (InputStream is = classLoader.getResourceAsStream(resource)) {
            if (is != null) {
                props.load(is);
            }
            version = props.getProperty("version", defVersion);
        } catch (IOException e) {
            // fall through
        }
        if (version != null) {
            version = version.trim();
            if (version.startsWith("${")) {
                version = defVersion;
            }
        }
        return version;
    }
}
