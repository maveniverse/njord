/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */

// Pre-build setup for deploy-release test

// Find the scripts directory - invoker clones tests to target/mvn*-it,
// so we need to go back to the source location
def scriptsDir = new File(basedir.parentFile.parentFile.parentFile, "src/it/scripts")
if (!scriptsDir.exists()) {
    // Fallback: try relative to basedir (for local dev)
    scriptsDir = new File(basedir, "../scripts")
}

// Load shared Docker management library
def dockerLib = evaluate(new File(scriptsDir, "DockerLib.groovy"))

println "[SETUP] Starting Docker setup for deploy-release test..."

// Restart Nexus with fresh state
dockerLib.restartNexus(basedir)

println "[SETUP] Setup complete. Proceeding with test..."
return true
