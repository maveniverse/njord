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
assert first.contains("[INFO] BUILD SUCCESS")

// second run:
assert second.contains("[INFO] Njord ${projectVersion} session created")
assert second.contains("[INFO] Total of 1 ArtifactStore.")
// this is same line, in between is date/timestamp
assert second.contains("[INFO] - basic-pom-all-props-00001 staged from eu.maveniverse.maven.njord.it:basic-pom-all-props:jar:1.0.0")
// note: mvn3 stages 2 and mvn4 stages 3 artifacts!


