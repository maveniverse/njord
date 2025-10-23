/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.nx3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.maveniverse.maven.njord.publisher.nx3.support.MavenInvokerHelper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for deploying and publishing releases to Nexus Repository 3.
 * <p>
 * This test verifies the complete workflow:
 * 1. Clean any existing artifact stores
 * 2. Deploy artifacts to local staging (with signing)
 * 3. Publish staged artifacts to Nexus Repository
 * </p>
 */
class DeployReleaseIT extends AbstractNexusIT {

    @TempDir
    Path tempDir;

    private File projectDir;

    @BeforeEach
    void setupTestProject() throws IOException {
        // Copy test project from src/it to temp directory (same source as maven-invoker-plugin)
        File sourceProject = Paths.get("src/it/deploy-release").toFile();
        projectDir = tempDir.resolve("deploy-release").toFile();

        FileUtils.copyDirectory(sourceProject, projectDir);

        System.out.println("[TEST] Test project copied to: " + projectDir.getAbsolutePath());
    }

    @Test
    void testDeployReleaseWithMaven39() throws MavenInvocationException, IOException {
        runDeployReleaseTest(getMaven39Version());
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_17)
    void testDeployReleaseWithMaven4() throws MavenInvocationException, IOException {
        runDeployReleaseTest(getMaven4Version());
    }

    private void runDeployReleaseTest(String mavenVersion) throws MavenInvocationException, IOException {
        System.out.println("[TEST] Running deploy-release test with Maven " + mavenVersion);

        // Set up Maven invoker
        File mavenHome =
                Paths.get("target/dependency/apache-maven-" + mavenVersion).toFile();
        if (!mavenHome.exists()) {
            throw new IllegalStateException(
                    "Maven distribution not found at: " + mavenHome + ". Run 'mvn generate-test-resources' first.");
        }

        MavenInvokerHelper invoker = new MavenInvokerHelper(mavenHome, getLocalRepository(), getUserHome());

        // Environment variables for GPG
        Map<String, String> env = new HashMap<>();
        env.put("MAVEN_GPG_PASSPHRASE", "TEST");

        // Additional properties
        Properties props = new Properties();

        // 1. Clean any existing stores
        System.out.println("[TEST] Step 1: Cleaning existing artifact stores");
        InvocationResult result1 = invoker.invoke(
                projectDir,
                java.util.Arrays.asList("-V", "-e", "njord:" + getProjectVersion() + ":drop-all", "-Dyes"),
                props,
                env);
        assertEquals(0, result1.getExitCode(), "drop-all goal should succeed");

        // 2. Deploy to local staging with signing
        System.out.println("[TEST] Step 2: Deploying artifacts to local staging");
        InvocationResult result2 = invoker.invoke(
                projectDir,
                java.util.Arrays.asList(
                        "-V", "-e",
                        "clean", "deploy",
                        "-P", "release"),
                props,
                env);
        assertEquals(0, result2.getExitCode(), "deploy goal should succeed");

        String deployOutput = invoker.getLastInvocationOutput();
        assertThat(deployOutput)
                .as("Deploy output should indicate Njord session created")
                .contains("[INFO] Njord " + getProjectVersion() + " session created");

        // 3. Publish to Nexus Repository
        System.out.println("[TEST] Step 3: Publishing staged artifacts to Nexus");
        InvocationResult result3 = invoker.invoke(
                projectDir,
                java.util.Arrays.asList(
                        "-V",
                        "-e",
                        "njord:" + getProjectVersion() + ":publish",
                        "-Dpublisher=sonatype-nx3",
                        "-Ddetails"),
                props,
                env);
        assertEquals(0, result3.getExitCode(), "publish goal should succeed");

        String publishOutput = invoker.getLastInvocationOutput();
        assertThat(publishOutput)
                .as("Publish output should indicate publishing started")
                .contains("[INFO] Publishing nx3-deploy-release-")
                .contains("sonatype-nx3");

        System.out.println("[TEST] Deploy-release test completed successfully with Maven " + mavenVersion);
    }
}
