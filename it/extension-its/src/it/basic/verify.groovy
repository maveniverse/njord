File firstLog = new File( basedir, 'first.log' )
assert firstLog.exists()
var first = firstLog.text

File secondLog = new File( basedir, 'second.log' )
assert secondLog.exists()
var second = secondLog.text

File thirdLog = new File( basedir, 'third.log' )
assert thirdLog.exists()
var third = thirdLog.text

File fourthLog = new File( basedir, 'fourth.log' )
assert fourthLog.exists()
var fourth = fourthLog.text

// Lets make strict assertion
// Also, consider Maven 3 vs 4 diff: they resolve differently; do not assert counts

// first run:
assert first.contains("[INFO] Njord ${projectVersion} session created")
assert first.contains('[INFO] Using alternate deployment repository id::njord:release')

// second run:
assert second.contains("[INFO] Njord ${projectVersion} session created")
assert second.contains('[INFO] Using alternate deployment repository id::njord:release-sca')

// third run:
assert third.contains("[INFO] Njord ${projectVersion} session created")
assert third.contains('[INFO] Using alternate deployment repository id::njord:release-redeploy-sca')

// fourth run:
assert fourth.contains("[INFO] Njord ${projectVersion} session created")
assert fourth.contains('[INFO] Total of 3 ArtifactStore.')
