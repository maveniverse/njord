# Sonatype Nx2 publisher

This module implements 2 publishers for use with Sonatype Nexus 2 provided services.

These publishers are **always available**.

* Publisher ID: `sonatype-oss` (to be sunset in 2025!).
* Configuration source: `properties` (`njord.properties`, Maven System properties, Maven User Properties or Project properties)
* Configuration:

| Configuration                 | Configuration key                                    | Default value                                                  |
|-------------------------------|------------------------------------------------------|----------------------------------------------------------------|
| Release server ID (for auth)  | `njord.publisher.sonatype-oss.releaseRepositoryId`   | `sonatype-oss`                                                 |
| Release server URL            | `njord.publisher.sonatype-oss.releaseRepositoryUrl`  | `https://oss.sonatype.org/service/local/staging/deploy/maven2` |
| Snapshot server ID (for auth) | `njord.publisher.sonatype-oss.snapshotRepositoryId`  | `sonatype-oss`                                                 |
| Snapshot server URL           | `njord.publisher.sonatype-oss.snapshotRepositoryUrl` | `https://oss.sonatype.org/content/repositories/snapshots`      |

* Publisher ID: `sonatype-s01` (to be sunset in 2025!).
* Configuration source: `properties` (`njord.properties`, Maven System properties, Maven User Properties or Project properties)
* Configuration:

| Configuration                 | Configuration key                                    | Default value                                                      |
|-------------------------------|------------------------------------------------------|--------------------------------------------------------------------|
| Release server ID (for auth)  | `njord.publisher.sonatype-s01.releaseRepositoryId`   | `sonatype-s01`                                                     |
| Release server URL            | `njord.publisher.sonatype-s01.releaseRepositoryUrl`  | `https://s01.oss.sonatype.org/service/local/staging/deploy/maven2` |
| Snapshot server ID (for auth) | `njord.publisher.sonatype-s01.snapshotRepositoryId`  | `sonatype-s01`                                                     |
| Snapshot server URL           | `njord.publisher.sonatype-s01.snapshotRepositoryUrl` | `https://s01.oss.sonatype.org/content/repositories/snapshots`      |
