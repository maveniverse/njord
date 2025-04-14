File firstLog = new File( basedir, 'first.log' )
assert firstLog.exists()
String first = firstLog.text

File secondLog = new File( basedir, 'second.log' )
assert secondLog.exists()
String second = secondLog.text

File thirdLog = new File( basedir, 'third.log' )
assert thirdLog.exists()
String third = thirdLog.text

File fourthLog = new File( basedir, 'fourth.log' )
assert fourthLog.exists()
String fourth = fourthLog.text

// Lets make strict assertion
// Also, consider Maven 3 vs 4 diff: they resolve differently; do not assert counts

// first run:
assert first.contains('[INFO] Njord enabled')
assert first.contains('[INFO] Using alternate deployment repository id::njord:release')

// second run:
assert second.contains('[INFO] Njord enabled')
assert second.contains('[INFO] Using alternate deployment repository id::njord:release-sca')

// third run:
assert third.contains('[INFO] Njord enabled')
assert third.contains('[INFO] Using alternate deployment repository id::njord:release-redeploy-sca')

// fourth run:
assert fourth.contains('[INFO] Njord enabled')
assert fourth.contains('[INFO] Total of 3 ArtifactStore.')
