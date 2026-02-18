# Install publisher

This module implements a generic "Install" publisher that publishes locally staged artifacts in pretty much same
manner as `maven-install-plugin` would. The publisher installs into local repository. 

This publisher is **always available**.

* Publisher ID: `install`.
* Configuration source: `POM` or `properties` (see below)
* Configuration: none
