# Deploy publisher

This module implements a generic "Deploy" publisher that publishes locally staged artifacts in pretty much same
manner as `maven-deploy-plugin` would. Using this publisher one can insert a "local staging" step for any reason,
into projects otherwise completely happy with vanilla Maven `deploy` phase.

Using this publisher, one can instead of `mvn deploy`, where Maven build deploys as last step, make Maven build
"locally stage", and then `mvn publish` performs the deployment the very same way as `mvn deploy` would do without
Njord extension.

This publisher is **always available**.

* Publisher ID: `deploy`.
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
