/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.nx3.support;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for invoking Maven programmatically using the maven-invoker component.
 * <p>
 * This class provides a convenient API for running Maven builds in test projects,
 * similar to how maven-invoker-plugin works but with programmatic control.
 * </p>
 */
public class MavenInvokerHelper {

    private static final Logger log = LoggerFactory.getLogger(MavenInvokerHelper.class);

    private final File mavenHome;
    private final File localRepository;
    private final File userHome;
    private final Properties systemProperties;

    /**
     * Creates a new MavenInvokerHelper.
     *
     * @param mavenHome       the Maven installation directory
     * @param localRepository the local repository directory
     * @param userHome        the user home directory (for settings.xml, etc.)
     */
    public MavenInvokerHelper(File mavenHome, File localRepository, File userHome) {
        this(mavenHome, localRepository, userHome, new Properties());
    }

    /**
     * Creates a new MavenInvokerHelper with custom system properties.
     *
     * @param mavenHome        the Maven installation directory
     * @param localRepository  the local repository directory
     * @param userHome         the user home directory (for settings.xml, etc.)
     * @param systemProperties additional system properties to pass to Maven
     */
    public MavenInvokerHelper(File mavenHome, File localRepository, File userHome, Properties systemProperties) {
        this.mavenHome = mavenHome;
        this.localRepository = localRepository;
        this.userHome = userHome;
        this.systemProperties = systemProperties;
    }

    /**
     * Invokes Maven with the specified goals and captures the output.
     *
     * @param projectDir the project directory
     * @param goals      the Maven goals to execute
     * @return the result of the invocation
     * @throws MavenInvocationException if the invocation fails
     */
    public InvocationResult invoke(File projectDir, String... goals) throws MavenInvocationException {
        return invoke(projectDir, Arrays.asList(goals), new Properties());
    }

    /**
     * Invokes Maven with the specified goals and additional properties.
     *
     * @param projectDir the project directory
     * @param goals      the Maven goals to execute
     * @param properties additional properties to pass to Maven
     * @return the result of the invocation
     * @throws MavenInvocationException if the invocation fails
     */
    public InvocationResult invoke(File projectDir, List<String> goals, Properties properties)
            throws MavenInvocationException {
        return invoke(projectDir, goals, properties, Collections.emptyMap(), null);
    }

    /**
     * Invokes Maven with the specified goals, properties, and environment variables.
     *
     * @param projectDir  the project directory
     * @param goals       the Maven goals to execute
     * @param properties  additional properties to pass to Maven
     * @param environment environment variables to set
     * @return the result of the invocation
     * @throws MavenInvocationException if the invocation fails
     */
    public InvocationResult invoke(
            File projectDir, List<String> goals, Properties properties, Map<String, String> environment)
            throws MavenInvocationException {
        return invoke(projectDir, goals, properties, environment, null);
    }

    /**
     * Invokes Maven with the specified goals, properties, environment variables, and optional log file.
     *
     * @param projectDir  the project directory
     * @param goals       the Maven goals to execute
     * @param properties  additional properties to pass to Maven
     * @param environment environment variables to set
     * @param logFile     optional log file to write output to (like maven-invoker-plugin's -l option)
     * @return the result of the invocation
     * @throws MavenInvocationException if the invocation fails
     */
    public InvocationResult invoke(
            File projectDir,
            List<String> goals,
            Properties properties,
            Map<String, String> environment,
            File logFile)
            throws MavenInvocationException {
        // Build output handler that captures, prints, and optionally writes to log file
        StringBuilder outputBuffer = new StringBuilder();
        InvocationOutputHandler outputHandler = line -> {
            String lineWithNewline = line + System.lineSeparator();
            outputBuffer.append(lineWithNewline);
            log.info("[MVN] {}", line);

            // Write to log file if specified (like maven-invoker-plugin's -l option)
            if (logFile != null) {
                try {
                    Files.write(
                            logFile.toPath(),
                            lineWithNewline.getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND);
                } catch (IOException e) {
                    log.error("Failed to write to log file {}: {}", logFile, e.getMessage());
                }
            }
        };

        // Merge system properties and convert environment variables to properties
        // (since maven-invoker doesn't support shell environment in all versions)
        Properties allProperties = new Properties();
        allProperties.putAll(this.systemProperties);
        allProperties.putAll(properties);

        // Convert certain environment variables to Maven properties
        for (Map.Entry<String, String> entry : environment.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Convert common environment variables to Maven properties
            if ("MAVEN_GPG_PASSPHRASE".equals(key)) {
                allProperties.setProperty("env.MAVEN_GPG_PASSPHRASE", value);
            } else {
                // Log warning for other environment variables
                log.warn("Environment variable {} may not be fully supported", key);
            }
        }

        // Build invocation request
        InvocationRequest request = new DefaultInvocationRequest()
                .setBaseDirectory(projectDir)
                .setGoals(goals)
                .setLocalRepositoryDirectory(localRepository)
                .setMavenHome(mavenHome)
                .setUserSettingsFile(
                        userHome.toPath().resolve(".m2/settings.xml").toFile())
                .setProperties(allProperties)
                .setOutputHandler(outputHandler)
                .setErrorHandler(outputHandler)
                .setShowErrors(true)
                .setBatchMode(true);

        // Execute
        Invoker invoker = new DefaultInvoker();
        InvocationResult result = invoker.execute(request);

        // Store output for retrieval
        lastInvocationOutput = outputBuffer.toString();

        return result;
    }

    private String lastInvocationOutput = "";

    /**
     * Gets the output from the last invocation.
     */
    public String getLastInvocationOutput() {
        return lastInvocationOutput;
    }
}
