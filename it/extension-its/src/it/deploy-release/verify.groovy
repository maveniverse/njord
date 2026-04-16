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

File log3f = new File( basedir, 'log-3.log' )
assert log3f.exists()
var log3 = log3f.text

File log4f = new File( basedir, 'log-4.log' )
assert log4f.exists()
var log4 = log4f.text

File log5f = new File( basedir, 'log-5.log' )
assert log5f.exists()
var log5 = log5f.text

// Lets make strict assertion
// Also, consider Maven 3 vs 4 diff: they resolve differently; do not assert counts

// first run:
assert log1.contains("[INFO] Njord ${projectVersion} session created")

// second run:
assert log2.contains("[INFO] Njord ${projectVersion} session created")
assert log2.contains("[INFO] Publishing deploy-release-00001 ")
assert log2.contains("[INFO] Published 8 artifact(s) to deploy-release-service repository")
assert new File( basedir, "releases-repo").isDirectory()

// third run:
assert log3.contains("[INFO] Njord ${projectVersion} session created")

// fourth run:
assert log4.contains("[INFO] Njord ${projectVersion} session created")

// fifth run:
assert log5.contains("[INFO] Njord ${projectVersion} session created")
assert log5.contains("[INFO] Publishing deploy-release-00002 ")
assert log5.contains("[INFO] Published 8 artifact(s) to deploy-snapshot-service repository")
assert new File( basedir, "snapshots-repo").isDirectory()
