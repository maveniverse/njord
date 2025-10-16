/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */

// Find the scripts directory
def scriptsDir = new File(basedir.parentFile.parentFile.parentFile, "src/it/scripts")
if (!scriptsDir.exists()) {
    scriptsDir = new File(basedir, "../scripts")
}

try {
    File firstLog = new File(basedir, 'first.log')
    assert firstLog.exists()
    var first = firstLog.text

    File secondLog = new File(basedir, 'second.log')
    assert secondLog.exists()
    var second = secondLog.text

    // Lets make strict assertion
    // Also, consider Maven 3 vs 4 diff: they resolve differently; do not assert counts

    // first run:
    assert first.contains("[INFO] Njord ${projectVersion} session created")

    // second run:
    assert second.contains("[INFO] Njord ${projectVersion} session created")
    assert second.contains("[INFO] Publishing nx3-deploy-release-00001 ")
    assert second.contains("sonatype-nx3")
} finally {
    // Teardown: Always stop Docker containers
    println "[TEARDOWN] Stopping Docker containers..."

    def dockerLib = evaluate(new File(scriptsDir, "DockerLib.groovy"))
    dockerLib.stopContainers(basedir)
}
