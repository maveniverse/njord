# TODO Items for publisher-sonatype-nx3-its

## Docker Container Cleanup

**Issue**: Need to ensure Docker containers are torn down after tests, even if tests fail.

**Current Behavior**:
- `pre-integration-test` phase: `docker compose up`
- `post-integration-test` phase: `docker compose down`
- `post-integration-test` runs even if tests fail when using `mvn verify`
- But may leave containers if Maven itself crashes or is interrupted

**Options to Consider**:

1. **Add maven-failsafe-plugin** - Standard pattern for integration tests with proper lifecycle handling
2. **Shell script wrapper with trap** - Guarantees cleanup even if Maven crashes
3. **Document requirement** - Ensure users/CI always use `mvn verify` not `mvn integration-test`
4. **Add cleanup verification** - Check for running containers and warn/fail if found

## Handling Multiple Maven Version Test Runs

**Issue**: Maven 3.9 and Maven 4 tests both upload the same GAV to Nexus. NXRM3 doesn't allow overwriting releases.

**Current Behavior**:
- Tests run sequentially: Maven 3.9 first, then Maven 4
- Maven 4 test fails because component already exists

**Options to Consider**:

1. **Unique versions per Maven version** - Use `1.0.0-mvn39` and `1.0.0-mvn4`
2. **REST API cleanup** - Add `setup.groovy` script to delete components before each test
3. **Restart Nexus between runs** - Clean slate but slow (~2-3 min startup)
4. **Use snapshot versions** - SNAPSHOT allows overwrites but doesn't test release workflow
5. **Delete components via REST API in teardown** - Clean up after each Maven version test completes

**Recommended Approach**:
- Add `setup.groovy` script that calls Nexus REST API to delete existing components
- Or use unique versions: `1.0.0-IT-${maven.version}` (e.g., `1.0.0-IT-3.9.11`)

## Nexus Setup Wizard Bypass

**Issue**: Would like to bypass the initial Nexus setup wizard for faster test startup.

**Current Solution**:
- Using `-Dnexus.security.randompassword=false` to disable random password
- Still requires manual password change on first run

**Options to Explore**:
- Pre-seed Nexus database with configured state
- Programmatically complete wizard via REST API on first startup
- Mount pre-configured nexus-data directory
- For now, documented in docker-compose.yml and accepted as manual step
