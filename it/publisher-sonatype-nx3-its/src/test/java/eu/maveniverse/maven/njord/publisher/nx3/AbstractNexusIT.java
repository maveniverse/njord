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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Comparator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
        // We need to use a writable bind mount (not copy) so Nexus can write to its data directory
        try {
            File projectBaseDir = Paths.get(System.getProperty("user.dir")).toFile();
            File dataTemplateDir = projectBaseDir.toPath().resolve("docker/data-template").toFile();

            // Create a writable directory in target/ and copy template there
            Path targetDir = projectBaseDir.toPath().resolve("target");
            File nexusDataDir = targetDir.resolve("nexus-data").toFile();

            // Clean and recreate the data directory
            if (nexusDataDir.exists()) {
                deleteDirectory(nexusDataDir.toPath());
            }
            nexusDataDir.mkdirs();

            // Copy template to writable location
            System.out.println("[SETUP] Copying Nexus data template from: " + dataTemplateDir);
            System.out.println("[SETUP] To writable directory: " + nexusDataDir);
            copyDirectory(dataTemplateDir.toPath(), nexusDataDir.toPath());

            nexus = new GenericContainer<>(NEXUS_IMAGE)
                    .withExposedPorts(NEXUS_PORT)
                    .withEnv(
                            "INSTALL4J_ADD_VM_PARAMS",
                            "-Xms512m -Xmx1024m -XX:MaxDirectMemorySize=512m "
                                    + "-Djava.util.prefs.userRoot=/nexus-data/javaprefs "
                                    + "-Dnexus.security.randompassword=false")
                    .withFileSystemBind(nexusDataDir.getAbsolutePath(), "/nexus-data")
                    .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("NEXUS-CONTAINER")))
                    .waitingFor(Wait.forHttp("/service/rest/v1/status")
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(3)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to set up Nexus data directory", e);
        }
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

    @AfterEach
    void cleanupNexusData() throws IOException {
        // Restart container with fresh data for next test to avoid conflicts
        System.out.println("[CLEANUP] Restarting Nexus with fresh data for next test");
        nexus.stop();

        // Recreate nexus data from template
        File projectBaseDir = Paths.get(System.getProperty("user.dir")).toFile();
        File dataTemplateDir = projectBaseDir.toPath().resolve("docker/data-template").toFile();
        Path targetDir = projectBaseDir.toPath().resolve("target");
        File nexusDataDir = targetDir.resolve("nexus-data").toFile();

        deleteDirectory(nexusDataDir.toPath());
        nexusDataDir.mkdirs();
        copyDirectory(dataTemplateDir.toPath(), nexusDataDir.toPath());

        nexus.start();
        System.out.println("[CLEANUP] Nexus restarted at: " + getNexusUrl());
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
        deleteDirectory(userHome.toPath());

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
     * Recursively deletes a directory.
     */
    private static void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
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
     * Falls back to reading from POM if not set (e.g., when running from IDE).
     */
    protected static String getProjectVersion() {
        String version = System.getProperty("project.version");
        if (version == null) {
            // Fallback for IDE execution: read from POM
            version = readVersionFromPom();
            System.out.println("[WARN] project.version not set in system properties, using fallback: " + version);
        }
        return version;
    }

    /**
     * Gets the Maven 3.9 version from system properties.
     * Falls back to default if not set (e.g., when running from IDE).
     */
    protected static String getMaven39Version() {
        String version = System.getProperty("maven39Version");
        if (version == null) {
            version = "3.9.11"; // Default from pom.xml
            System.out.println("[WARN] maven39Version not set in system properties, using default: " + version);
        }
        return version;
    }

    /**
     * Gets the Maven 4 version from system properties.
     * Falls back to default if not set (e.g., when running from IDE).
     */
    protected static String getMaven4Version() {
        String version = System.getProperty("maven4Version");
        if (version == null) {
            version = "4.0.0-rc-3"; // Default from pom.xml
            System.out.println("[WARN] maven4Version not set in system properties, using default: " + version);
        }
        return version;
    }

    /**
     * Reads the project version from the POM file.
     * This is a fallback for when tests are run from IDE without maven-failsafe-plugin.
     */
    private static String readVersionFromPom() {
        try {
            Path pomPath = Paths.get(System.getProperty("user.dir")).resolve("pom.xml");
            String pomContent = new String(Files.readAllBytes(pomPath), StandardCharsets.UTF_8);

            // Simple regex to extract version from POM
            // Looking for: <parent>...<version>X.Y.Z-SNAPSHOT</version>...
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "<parent>.*?<version>([^<]+)</version>",
                java.util.regex.Pattern.DOTALL
            );
            java.util.regex.Matcher matcher = pattern.matcher(pomContent);
            if (matcher.find()) {
                return matcher.group(1);
            }

            // Fallback: look for any version tag
            pattern = java.util.regex.Pattern.compile("<version>([^<]+)</version>");
            matcher = pattern.matcher(pomContent);
            if (matcher.find()) {
                return matcher.group(1);
            }

            throw new IllegalStateException("Could not find version in POM: " + pomPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read version from POM", e);
        }
    }
}
