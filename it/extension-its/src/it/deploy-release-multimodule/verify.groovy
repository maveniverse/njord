/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
File log1f = new File( basedir, 'log-1.log' )
assert log1f.exists()
var log1 = log1f.text

File log2f = new File( basedir, 'log-2.log' )
assert log2f.exists()
var log2 = log2f.text

// first run: all modules staged into single session store
assert log1.contains("[INFO] Njord ${projectVersion} session created")
assert log1.contains("[INFO] BUILD SUCCESS")
assert !log1.contains("is closed")

// second run:
assert log2.contains("[INFO] Njord ${projectVersion} session created")
assert log2.contains("[INFO] Publishing deploy-release-multimodule-00001 ")
var repo = new File( basedir, "releases-repo/eu/maveniverse/maven/njord/it")
assert new File( repo, "deploy-release-multimodule/1.0.0/deploy-release-multimodule-1.0.0.pom").isFile()
assert new File( repo, "api/1.0.0/api-1.0.0.jar").isFile()
assert new File( repo, "core/1.0.0/core-1.0.0.jar").isFile()

File log3f = new File( basedir, 'log-3.log' )
assert log3f.exists()
var log3 = log3f.text

// third run: deploy at end and auto publish
assert log3.contains("[INFO] BUILD SUCCESS")
assert !log3.contains("is closed")
assert log3.contains("[INFO] Njord auto publish: Published 1 stores created in this session")
// releases-repo still has files from second run; compare published counts instead (all modules published)
def published = ~/\[INFO\] Published (\d+) artifact\(s\) to deploy-release-service repository/
var count2 = (log2 =~ published)[0][1]
var count3 = (log3 =~ published)[0][1]
assert count2 == count3
