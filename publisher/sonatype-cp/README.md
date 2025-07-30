# Sonatype CP publisher

This module implements publisher for use with Sonatype provided Portal service.

This publisher is **always available**.

* Publisher ID: `sonatype-cp`
* Configuration source: `properties` (`njord.properties`, Maven System properties, Maven User Properties or Project properties)
* Configuration:

| Configuration                 | Configuration key                                   | Default value                                             |
|-------------------------------|-----------------------------------------------------|-----------------------------------------------------------|
| Release server ID (for auth)  | `njord.publisher.sonatype-cp.releaseRepositoryId`   | `sonatype-cp`                                             |
| Release server URL            | `njord.publisher.sonatype-cp.releaseRepositoryUrl`  | `https://central.sonatype.com/api/v1/publisher/upload`    |
| Snapshot server ID (for auth) | `njord.publisher.sonatype-cp.snapshotRepositoryId`  | `sonatype-cp`                                             |
| Snapshot server URL           | `njord.publisher.sonatype-cp.snapshotRepositoryUrl` | `https://central.sonatype.com/repository/maven-snapshots` |

