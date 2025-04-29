/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
File firstLog = new File( basedir, 'l01.log' )
assert firstLog.exists()
var first = firstLog.text

File secondLog = new File( basedir, 'l02.log' )
assert secondLog.exists()
var second = secondLog.text

File thirdLog = new File( basedir, 'l03.log' )
assert thirdLog.exists()
var third = thirdLog.text

File fourthLog = new File( basedir, 'l04.log' )
assert fourthLog.exists()
var fourth = fourthLog.text

File fifthLog = new File( basedir, 'l05.log' )
assert fifthLog.exists()
var fifth = fifthLog.text

File sixthLog = new File( basedir, 'l06.log' )
assert sixthLog.exists()
var sixth = sixthLog.text

File seventhLog = new File( basedir, 'l07.log' )
assert seventhLog.exists()
var seventh = seventhLog.text

File eighthLog = new File( basedir, 'l08.log' )
assert eighthLog.exists()
var eighth = eighthLog.text

// Lets make strict assertion
// Also, consider Maven 3 vs 4 diff: they resolve differently; do not assert counts

// first run:
assert first.contains("[INFO] Njord ${projectVersion} session created")
assert first.contains('[INFO] Using alternate deployment repository id::njord:release-sca')

// second run:
assert second.contains("[INFO] Njord ${projectVersion} session created")
assert second.contains('[INFO] Using alternate deployment repository id::njord:release-sca')

// third run:
assert third.contains("[INFO] Njord ${projectVersion} session created")
assert third.contains('[INFO] Exporting store merge-all-release-00001')

// fourth run:
assert fourth.contains("[INFO] Njord ${projectVersion} session created")
assert fourth.contains('[INFO] Exporting store merge-all-release-00002')

// fifth run:
assert fifth.contains("[INFO] Njord ${projectVersion} session created")
assert fifth.contains('[INFO] Dropped total of 2 ArtifactStore')

// sixth run:
assert sixth.contains("[INFO] Njord ${projectVersion} session created")
assert sixth.contains('[INFO] Importing stores from')

// seventh run:
assert seventh.contains("[INFO] Njord ${projectVersion} session created")
assert seventh.contains('[INFO] Merging merge-all-release-00001')
assert seventh.contains('[INFO] Merging merge-all-release-00002')

// eighth run:
assert eighth.contains("[INFO] Njord ${projectVersion} session created")
assert eighth.contains('[INFO] Total of 1 ArtifactStore.')

