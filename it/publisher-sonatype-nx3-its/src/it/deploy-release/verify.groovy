/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */

/*
 * Verification script for deploy-release test.
 * This script is compatible with both maven-invoker-plugin and maven-failsafe-plugin.
 *
 * When used with failsafe-plugin + Testcontainers:
 * - Docker cleanup is handled by Testcontainers automatically
 * - Bindings provided: basedir, projectVersion
 */

// Read and verify deploy.log (deploy to staging)
File deployLog = new File(basedir, 'deploy.log')
assert deployLog.exists(), "deploy.log not found"
def deployOutput = deployLog.text

// Read and verify publish.log (publish to Nexus)
File publishLog = new File(basedir, 'publish.log')
assert publishLog.exists(), "publish.log not found"
def publishOutput = publishLog.text

// Assertions for deploy phase
assert deployOutput.contains("[INFO] Njord ${projectVersion} session created"),
    "Deploy log should contain session created message"

// Assertions for publish phase
assert publishOutput.contains("[INFO] Publishing nx3-deploy-release-"),
    "Publish log should contain publishing message"
assert publishOutput.contains("sonatype-nx3"),
    "Publish log should contain publisher name"

println "[VERIFY] All assertions passed successfully"
