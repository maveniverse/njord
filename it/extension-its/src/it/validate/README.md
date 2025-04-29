# Validate

Tests validate of Njord.

The point is that the two mandatory parameters for `validate` mojo are populated, so caller is not 
specifying them:
* `target` is set in `.mvn/maven.config` to `sonatype-cp`.
* `store` is automatically discovered (prefix is set, and there is only one store with this prefix).

Outcome is build failure, as Central validation will fail since staged store does not contains
signatures, javadoc and sources.