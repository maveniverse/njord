# Double Load

Njord is loaded twice: as core extension (`.mvn/extensions.xml`) and as POM build extension (older version).
Njord must refuse to run, as the two copies would step on each other (ie. "PathArtifactStore is closed").
