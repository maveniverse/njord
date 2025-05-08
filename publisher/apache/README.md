# Apache publisher

This module implements one publisher for ASF https://repository.apache.org/ hosted Sonatype Nexus 2 instance.
If you are set up for Apache releases against this instance already (ie using ASF Maven Parent POM)
there is nothing needed to be done.

This publisher is **always available**.

* Publisher ID: `apache-rao`.
* Configuration source: `properties` (`njord.properties`, Maven System properties, Maven User Properties or Project properties)
* Configuration:

| Configuration                 | Configuration key                                  | Default value                                                       |
|-------------------------------|----------------------------------------------------|---------------------------------------------------------------------|
| Release server ID (for auth)  | `njord.publisher.apache-rao.releaseRepositoryId`   | `apache.releases.https`                                             |
| Release server URL            | `njord.publisher.apache-rao.releaseRepositoryUrl`  | `https://repository.apache.org/service/local/staging/deploy/maven2` |
| Snapshot server ID (for auth) | `njord.publisher.apache-rao.snapshotRepositoryId`  | `apache.releases.https`                                             |
| Snapshot server URL           | `njord.publisher.apache-rao.snapshotRepositoryUrl` | `https://repository.apache.org/content/repositories/snapshots`      |

