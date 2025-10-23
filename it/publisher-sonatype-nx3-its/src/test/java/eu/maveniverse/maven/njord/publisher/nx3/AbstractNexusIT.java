/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.nx3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Comparator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * Base class for Nexus integration tests using Testcontainers.
 * <p>
 * This class sets up a Nexus Repository OSS container with a pre-configured database
 * template to bypass the EULA wizard. Each test class gets its own isolated Nexus
 * instance.
 * </p>
 */
@Testcontainers
public abstract class AbstractNexusIT {

    protected static final String NEXUS_IMAGE = "sonatype/nexus3:latest";
    protected static final int NEXUS_PORT = 8081;

    @Container
    protected static GenericContainer<?> nexus;

    protected static File localRepository;
    protected static File userHome;

    static {
        // Set up Nexus container with pre-configured database
        File projectBaseDir = Paths.get(System.getProperty("user.dir")).toFile();
        File dataTemplateDir =
                projectBaseDir.toPath().resolve("docker/data-template").toFile();

        nexus = new GenericContainer<>(NEXUS_IMAGE)
                .withExposedPorts(NEXUS_PORT)
                .withEnv(
                        "INSTALL4J_ADD_VM_PARAMS",
                        "-Xms512m -Xmx1024m -XX:MaxDirectMemorySize=512m "
                                + "-Djava.util.prefs.userRoot=/nexus-data/javaprefs "
                                + "-Dnexus.security.randompassword=false")
                .withCopyFileToContainer(MountableFile.forHostPath(dataTemplateDir.toPath()), "/nexus-data")
                .waitingFor(Wait.forHttp("/service/rest/v1/status")
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(3)));
    }

    @BeforeAll
    static void setupTestEnvironment() throws IOException {
        // Set up isolated local repository (shared across tests, like maven-invoker-plugin)
        Path targetDir = Paths.get(System.getProperty("user.dir")).resolve("target");
        localRepository = targetDir.resolve("it-repo").toFile();
        localRepository.mkdirs();

        // Set up isolated user home directory
        userHome = targetDir.resolve("it-user").toFile();
        recreateUserHome();

        System.out.println("[SETUP] Nexus container started at: " + getNexusUrl());
        System.out.println("[SETUP] Local repository: " + localRepository.getAbsolutePath());
        System.out.println("[SETUP] User home: " + userHome.getAbsolutePath());
    }

    @AfterAll
    static void teardownTestEnvironment() {
        // Testcontainers will automatically stop and remove the container
        System.out.println("[TEARDOWN] Nexus container stopped");
    }

    /**
     * Recreates the user home directory from the template.
     */
    protected static void recreateUserHome() throws IOException {
        // Clean existing user home
        if (userHome.exists()) {
            Files.walk(userHome.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

        // Copy template from src/it/user-home to target/it-user
        File userHomeTemplate = Paths.get(System.getProperty("user.dir"))
                .resolve("src/it/user-home")
                .toFile();
        if (!userHomeTemplate.exists()) {
            throw new IllegalStateException("User home template not found at: " + userHomeTemplate);
        }

        copyDirectory(userHomeTemplate.toPath(), userHome.toPath());
    }

    /**
     * Recursively copies a directory.
     */
    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                } else {
                    Files.copy(sourcePath, targetPath);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy " + sourcePath, e);
            }
        });
    }

    /**
     * Gets the Nexus URL accessible from the host.
     */
    protected static String getNexusUrl() {
        return "http://" + nexus.getHost() + ":" + nexus.getMappedPort(NEXUS_PORT);
    }

    /**
     * Gets the local repository directory.
     */
    protected static File getLocalRepository() {
        return localRepository;
    }

    /**
     * Gets the user home directory.
     */
    protected static File getUserHome() {
        return userHome;
    }

    /**
     * Gets the project version from system properties.
     */
    protected static String getProjectVersion() {
        return System.getProperty("project.version");
    }

    /**
     * Gets the Maven 3.9 version from system properties.
     */
    protected static String getMaven39Version() {
        return System.getProperty("maven39Version");
    }

    /**
     * Gets the Maven 4 version from system properties.
     */
    protected static String getMaven4Version() {
        return System.getProperty("maven4Version");
    }
}
