/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.nx3.support;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for executing Groovy scripts in tests.
 * <p>
 * This provides maven-invoker-plugin compatibility by exe.groovy scripts
 * with the same bindings (basedir, projectVersion, etc.)
 * </p>
 */
public class GroovyScriptRunner {

    private static final Logger log = LoggerFactory.getLogger(GroovyScriptRunner.class);

    /**
     * Executes a Groovy script file with provided bindings.
     *
     * @param scriptFile the Groovy script to execute
     * @param bindings   variable bindings to pass to the script
     * @throws IOException if the script cannot be read
     * @throws AssertionError if assertions in the script fail
     */
    public static void runScript(File scriptFile, Map<String, Object> bindings) throws IOException {
        if (!scriptFile.exists()) {
            log.debug("Script file does not exist, skipping: {}", scriptFile);
            return;
        }

        log.info("Executing Groovy script: {}", scriptFile.getName());

        // Read script content
        String scriptContent = new String(Files.readAllBytes(scriptFile.toPath()), StandardCharsets.UTF_8);

        // Create bindings
        Binding binding = new Binding();
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            binding.setVariable(entry.getKey(), entry.getValue());
            log.debug("Binding variable: {} = {}", entry.getKey(), entry.getValue());
        }

        // Execute script
        GroovyShell shell = new GroovyShell(binding);
        try {
            Object result = shell.evaluate(scriptContent, scriptFile.getName());
            log.info("Script executed successfully: {}", scriptFile.getName());
            if (result != null) {
                log.debug("Script result: {}", result);
            }
        } catch (AssertionError e) {
            log.error("Assertion failed in {}: {}", scriptFile.getName(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error executing {}: {}", scriptFile.getName(), e.getMessage(), e);
            throw new RuntimeException("Failed to execute " + scriptFile.getName(), e);
        }
    }

    /**
     * Runs verify.groovy script if it exists in the project directory.
     *
     * @param projectDir     the test project directory (basedir)
     * @param projectVersion the project version for interpolation
     * @throws IOException if the script cannot be read
     */
    public static void runVerifyScript(File projectDir, String projectVersion) throws IOException {
        File verifyScript = new File(projectDir, "verify.groovy");
        if (!verifyScript.exists()) {
            log.debug("No verify.groovy found in {}, skipping verification script", projectDir);
            return;
        }

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("basedir", projectDir);
        bindings.put("projectVersion", projectVersion);

        runScript(verifyScript, bindings);
    }
}
