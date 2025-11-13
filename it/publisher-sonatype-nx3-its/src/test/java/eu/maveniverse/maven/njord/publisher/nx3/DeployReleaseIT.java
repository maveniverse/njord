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

import eu.maveniverse.maven.njord.publisher.nx3.support.GroovyScriptRunner;
import eu.maveniverse.maven.njord.publisher.nx3.support.InvokerPropertiesParser;
import eu.maveniverse.maven.njord.publisher.nx3.support.MavenInvokerHelper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(DeployReleaseIT.class);

    private File projectDir;

    @BeforeEach
    void setupTestProject(org.junit.jupiter.api.TestInfo testInfo) throws IOException {
        // Copy test project to target directory (like maven-invoker-plugin does)
        // This makes it easier to debug by inspecting target/test-projects/
        String testName = testInfo.getTestMethod()
                .map(m -> m.getName())
                .orElse("unknown-test");
        File targetTestProjects = Paths.get("target/test-projects").toFile();
        targetTestProjects.mkdirs();

        File sourceProject = Paths.get("src/it/deploy-release").toFile();
        projectDir = new File(targetTestProjects, testName + "/deploy-release");

        // Clean and recreate
        if (projectDir.exists()) {
            FileUtils.deleteDirectory(projectDir);
        }
        FileUtils.copyDirectory(sourceProject, projectDir);

        // Perform property interpolation (like maven-invoker-plugin does)
        interpolateFile(projectDir.toPath().resolve(".mvn/extensions.xml").toFile(), "@project.version@",
                getProjectVersion());

        log.info("Test project copied to: {}", projectDir.getAbsolutePath());
    }

    /**
     * Replaces placeholders in a file.
     */
    private void interpolateFile(File file, String placeholder, String value) throws IOException {
        if (!file.exists()) {
            return;
        }
        String content = FileUtils.readFileToString(file, "UTF-8");
        content = content.replace(placeholder, value);
        FileUtils.writeStringToFile(file, content, "UTF-8");
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
        log.info("Running deploy-release test with Maven {}", mavenVersion);

        // Set up Maven invoker
        File mavenHome = Paths.get("target/dependency/apache-maven-" + mavenVersion).toFile();
        if (!mavenHome.exists()) {
            throw new IllegalStateException(
                    "Maven distribution not found at: " + mavenHome + ". Run 'mvn generate-test-resources' first.");
        }

        MavenInvokerHelper invoker = new MavenInvokerHelper(mavenHome, getLocalRepository(), getUserHome());

        // Additional properties for interpolation
        Properties props = new Properties();
        props.setProperty("nexus.url", getNexusUrl());

        // Parse invoker.properties file
        File invokerPropsFile = new File(projectDir, "invoker.properties");
        if (!invokerPropsFile.exists()) {
            throw new IllegalStateException("invoker.properties not found at: " + invokerPropsFile);
        }

        Map<String, String> interpolationValues = new HashMap<>();
        interpolationValues.put("project.version", getProjectVersion());
        interpolationValues.put("nexus.url", getNexusUrl());

        InvokerPropertiesParser parser = new InvokerPropertiesParser(invokerPropsFile, interpolationValues);

        // Get environment variables from invoker.properties
        Map<String, String> env = parser.getEnvironmentVariables();
        log.info("Environment variables from invoker.properties: {}", env);

        // Execute all goal invocations
        List<InvokerPropertiesParser.GoalInvocation> invocations = parser.getGoalInvocations(projectDir);
        log.info("Found {} goal invocation(s) in invoker.properties", invocations.size());

        for (int i = 0; i < invocations.size(); i++) {
            InvokerPropertiesParser.GoalInvocation invocation = invocations.get(i);
            int stepNumber = i + 1;

            log.info("Step {}: Executing goals: {}", stepNumber, String.join(" ", invocation.getGoals()));

            InvocationResult result =
                    invoker.invoke(projectDir, invocation.getGoals(), props, env, invocation.getLogFile());

            assertEquals(0, result.getExitCode(), "Goal invocation " + stepNumber + " should succeed");

            if (invocation.getLogFile() != null) {
                log.info("Logs written to: {}", invocation.getLogFile().getAbsolutePath());
            }
        }

        // Run verify.groovy script if it exists (maven-invoker-plugin compatibility)
        log.info("Checking for verify.groovy script...");
        GroovyScriptRunner.runVerifyScript(projectDir, getProjectVersion());

        // Additional Java assertions (optional, verify.groovy may handle most)
        String lastOutput = invoker.getLastInvocationOutput();
        assertThat(lastOutput)
                .as("Last invocation output should indicate publishing")
                .contains("sonatype-nx3");

        log.info("Deploy-release test completed successfully with Maven {}", mavenVersion);
    }
}
