/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
File firstLog = new File( basedir, 'first.log' )
assert firstLog.exists()
var first = firstLog.text

File secondLog = new File( basedir, 'second.log' )
assert secondLog.exists()
var second = secondLog.text

// Lets make strict assertion
// Also, consider Maven 3 vs 4 diff: they resolve differently; do not assert counts

// first run:
assert first.contains("[INFO] Njord ${projectVersion} session created")
assert first.contains('[INFO] Njord service configuration found for server \'sonatype-cp-service\'')

// second run:
assert second.contains("[INFO] Njord ${projectVersion} session created")
assert second.contains("[INFO] No applicable validator set for publisher \'sonatype-cp\'; validation skipped")
assert second.contains("[INFO] Dry run; not publishing to \'sonatype-cp\' service at https://central.sonatype.com/repository/maven-snapshots")