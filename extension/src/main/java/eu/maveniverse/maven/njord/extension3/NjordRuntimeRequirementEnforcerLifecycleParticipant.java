/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.extension3;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lifecycle participant that enforces Njord requirements.
 * <p>
 * This class is intentionally self-contained and compiled with Java 8 to make it able to run on wide range of
 * Maven and Java versions, to report sane error for user why Njord refuses to work in their environment.
 */
@Singleton
@Named
public class NjordRuntimeRequirementEnforcerLifecycleParticipant extends AbstractMavenLifecycleParticipant {
    private static final String NJORD_MAVEN_REQUIREMENT = "[3.9,)";
    private static final String NJORD_JAVA_REQUIREMENT = "[11,)";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        if (!checkRuntimeRequirements()) {
            throw new MavenExecutionException("Runtime requirements are not fulfilled", (Throwable) null);
        }
    }

    private boolean checkRuntimeRequirements() {
        String mavenVersionString =
                discoverArtifactVersion(Version.class.getClassLoader(), "org.apache.maven", "maven-core", null);
        String javaVersionString = System.getProperty("java.version");
        if (mavenVersionString == null || javaVersionString == null) {
            throw new IllegalStateException("Maven and/or Java version could not be determined");
        }
        try {
            GenericVersionScheme versionScheme = new GenericVersionScheme();
            VersionConstraint mavenConstraint = versionScheme.parseVersionConstraint(NJORD_MAVEN_REQUIREMENT);
            VersionConstraint javaConstraint = versionScheme.parseVersionConstraint(NJORD_JAVA_REQUIREMENT);
            Version mavenVersion = versionScheme.parseVersion(mavenVersionString);
            Version javaVersion = versionScheme.parseVersion(javaVersionString);

            boolean mavenOk = mavenConstraint.containsVersion(mavenVersion);
            boolean javaOk = javaConstraint.containsVersion(javaVersion);
            boolean runtimeRequirements = mavenOk && javaOk;
            if (!runtimeRequirements) {
                logger.warn("Maveniverse Njord runtime requirements are not fulfilled:");
                if (!mavenOk) {
                    logger.warn(String.format(
                            "* Unsupported Maven version %s; supported versions are %s",
                            mavenVersion, mavenConstraint));
                }
                if (!javaOk) {
                    logger.warn(String.format(
                            "* Unsupported Java version %s; supported versions are %s", javaVersion, javaConstraint));
                }
                logger.error(
                        "Maveniverse Njord cannot operate in this environment: adapt your environment or remove Njord");
            }
            return runtimeRequirements;
        } catch (InvalidVersionSpecificationException e) {
            throw new IllegalStateException("Maven and/or Java version could not be parsed", e);
        }
    }

    /**
     * Discovers artifact version, works for only Maven built ones.
     */
    private String discoverArtifactVersion(
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
