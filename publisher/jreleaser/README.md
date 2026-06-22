# JReleaser publisher

This module integrates with JReleaser.

This publisher is **available with dedicated extension** (due JReleaser size).

* Publisher ID: `jreleaser`.
* Configuration source: `POM` or `properties` (see below)
* Configuration:

| Configuration                 | Configuration key                                       | Default value |
|-------------------------------|---------------------------------------------------------|---------------|
| Release server ID (for auth)  | `project/distributionManagement/repository/id`          | None          |
| Release server URL            | `project/distributionManagement/repository/url`         | None          |
| Snapshot server ID (for auth) | `project/distributionManagement/snapshotRepository/id`  | None          |
| Snapshot server URL           | `project/distributionManagement/snapshotRepository/url` | None          |

Note: This publisher supports `altDeploymentRepository` property as well in very same manner as 
`maven-deploy-plugin` [supports it](https://maven.apache.org/plugins/maven-deploy-plugin/deploy-mojo.html#altDeploymentRepository).
The properties `altReleaseDeploymentRepository` and `altSnapshotDeploymentRepository` are **not supported**. Also 
the publisher does **not supports legacy format** for this property, it expects `id::url` format only.
