/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */

/**
 * Shared Docker management library for integration tests.
 *
 * This script provides reusable functions for managing Docker containers during tests.
 * Import this in your test's setup.groovy or postbuild.groovy to use these functions.
 *
 * Example usage in test setup.groovy:
 *   def dockerLib = evaluate(new File(basedir, "../scripts/DockerLib.groovy"))
 *   dockerLib.restartNexus(basedir)
 */

import java.net.HttpURLConnection
import java.net.URL
import groovy.ant.AntBuilder

/**
 * Helper function to run shell commands
 */
def runDockerCommand(String[] command, File workDir, String prefix = "DOCKER") {
    println "[${prefix}] Running: ${command.join(' ')}"
    def process = new ProcessBuilder(command)
        .directory(workDir)
        .redirectErrorStream(true)
        .start()

    def output = new StringBuilder()
    process.inputStream.eachLine { line ->
        output.append(line).append('\n')
        println "[${prefix}]   ${line}"
    }

    def exitCode = process.waitFor()
    if (exitCode != 0) {
        throw new RuntimeException("Command failed with exit code ${exitCode}: ${command.join(' ')}\nOutput:\n${output}")
    }
    return output.toString()
}

/**
 * Stops any existing Docker containers
 */
def stopContainers(File testBasedir) {
    // Navigate from cloned test dir to source docker dir
    // testBasedir is like: target/mvn39-it/deploy-release
    // We need to go to: docker (at module root)
    def dockerComposeDir = new File(testBasedir.parentFile.parentFile.parentFile, "docker").canonicalFile
    println "[DOCKER] Stopping any existing containers..."
    println "[DOCKER] Docker Compose directory: ${dockerComposeDir}"

    try {
        runDockerCommand(["docker", "compose", "down", "-v"] as String[], dockerComposeDir, "DOCKER")
        println "[DOCKER] ✓ Containers stopped"
    } catch (Exception e) {
        println "[DOCKER] Warning: Failed to stop containers (may not exist): ${e.message}"
    }
}

/**
 * Starts fresh Docker containers and waits for Nexus to be ready
 */
def startNexus(File testBasedir, int maxAttempts = 60, int waitSeconds = 2) {
    // Navigate from cloned test dir to source docker dir
    // testBasedir is like: target/mvn39-it/deploy-release
    // We need to go to: docker (at module root)
    def dockerComposeDir = new File(testBasedir.parentFile.parentFile.parentFile, "docker").canonicalFile
    def dataDir = new File(dockerComposeDir, "data")
    def nexusUrl = "http://localhost:8081"
    def statusEndpoint = "${nexusUrl}/service/rest/v1/status"

    println "[DOCKER] Starting fresh Nexus container..."
    println "[DOCKER] Docker Compose directory: ${dockerComposeDir}"

    // Purge data directory (except .gitignore) for completely fresh state
    println "[DOCKER] Purging data directory: ${dataDir}"
    if (dataDir.exists()) {
        dataDir.eachFile { file ->
            if (file.name != '.gitignore') {
                if (file.isDirectory()) {
                    file.deleteDir()
                } else {
                    file.delete()
                }
            }
        }
    }

    // Copy template data (with EULA accepted) to bypass onboarding wizard
    println "[DOCKER] Copying pre-configured Nexus database (EULA accepted)..."
    def templateDir = new File(dockerComposeDir, "data-template")
    if (templateDir.exists()) {
        // Use AntBuilder to copy db directory only
        def ant = new AntBuilder()
        def templateDb = new File(templateDir, "db")
        def targetDb = new File(dataDir, "db")
        if (templateDb.exists()) {
            ant.copy(todir: targetDb, overwrite: true) {
                fileset(dir: templateDb)
            }
            println "[DOCKER] Database template copied successfully"
        } else {
            println "[DOCKER] Warning: Template database not found at ${templateDb}"
        }
    } else {
        println "[DOCKER] Warning: Template directory not found at ${templateDir}"
        println "[DOCKER] Nexus will start with onboarding wizard - tests may fail"
    }

    // Start containers
    runDockerCommand(["docker", "compose", "up", "-d"] as String[], dockerComposeDir, "DOCKER")

    // Wait for Nexus to be ready
    println "[DOCKER] Waiting for Nexus to be ready at ${nexusUrl}..."

    boolean nexusReady = false
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            URL url = new URL(statusEndpoint)
            HttpURLConnection conn = (HttpURLConnection) url.openConnection()
            conn.setRequestMethod("GET")
            conn.setConnectTimeout(2000)
            conn.setReadTimeout(2000)

            int responseCode = conn.responseCode
            if (responseCode == 200) {
                println "[DOCKER] ✓ Nexus is ready (attempt ${attempt}/${maxAttempts})"
                nexusReady = true
                break
            } else {
                println "[DOCKER] Nexus returned status ${responseCode}, waiting... (attempt ${attempt}/${maxAttempts})"
            }
            conn.disconnect()
        } catch (Exception e) {
            if (attempt % 10 == 0) {
                println "[DOCKER] Nexus not ready yet: ${e.message} (attempt ${attempt}/${maxAttempts})"
            }
        }

        if (!nexusReady && attempt < maxAttempts) {
            Thread.sleep(waitSeconds * 1000)
        }
    }

    if (!nexusReady) {
        throw new RuntimeException("""
╔════════════════════════════════════════════════════════════════════════════╗
║ ERROR: Nexus Repository failed to start!                                   ║
╟────────────────────────────────────────────────────────────────────────────╢
║ Tried to start Nexus using docker compose in:                              ║
║   ${dockerComposeDir}
║                                                                            ║
║ Check Docker logs with:                                                    ║
║   docker logs nexus-repository                                             ║
╚════════════════════════════════════════════════════════════════════════════╝
""")
    }

    println "[DOCKER] ✓ Nexus health check passed"
}

/**
 * Convenience method to stop and start fresh containers
 */
def restartNexus(File testBasedir) {
    stopContainers(testBasedir)
    startNexus(testBasedir)
}

// Return a map of available functions so they can be called from importing scripts
return [
    runDockerCommand: this.&runDockerCommand,
    stopContainers: this.&stopContainers,
    startNexus: this.&startNexus,
    restartNexus: this.&restartNexus
]
