# Deploy Release Test for Sonatype NX3

This integration test verifies that the sonatype-nx3 publisher can successfully deploy artifacts to a Nexus Repository 3 instance.

## Test Lifecycle

### Pre-build (setup.groovy)
1. Loads shared `DockerLib.groovy` library from `src/it/scripts/`
2. Stops any existing Docker containers
3. Purges `docker/data/` directory for fresh state
4. Copies pre-configured database from `docker/data-template/db/` (with EULA accepted)
5. Starts fresh Nexus container via `docker compose up -d`
6. Waits for Nexus health check at `http://localhost:8081/service/rest/v1/status`

### Build and Deploy
1. **First run** (`invoker.goals.1`): Drop all existing staged artifacts, then deploy locally
   - Uses `release` profile (generates sources, javadoc, and signatures)
   - Stages artifacts to local Njord store using `njord:` altDeploymentRepository
2. **Second run** (`invoker.goals.2`): Publish staged artifacts to NXRM3
   - Uses `sonatype-nx3` publisher
   - Uploads to Nexus running at `http://localhost:8081`

### Post-build (verify.groovy)
1. Asserts log files contain expected messages (Njord session creation, publishing)
2. **Finally block**: Tears down Docker containers with `docker compose down -v`

## Configuration

- **Repository URL**: `http://localhost:8081` (NXRM3 instance managed by Docker Compose)
- **Release Repository**: `maven-releases`
- **Snapshot Repository**: `maven-snapshots`
- **Tag**: `test-deployment`
- **Credentials**: `admin/admin123` (configured in test settings.xml)

## Docker Management

This test uses the shared `DockerLib.groovy` library for Docker lifecycle management:
- **Template Database**: `docker/data-template/db/` contains pre-configured H2 database with EULA accepted
- **Fresh State**: Each test run gets completely isolated Nexus instance
- **Health Checks**: Waits up to 60 attempts (2s each) for Nexus to be ready
- **Guaranteed Cleanup**: Docker teardown in finally block ensures no leftover containers

## Verification

The test verifies:
- Njord session is created successfully (first.log)
- Artifacts are staged during the deploy phase
- Publisher successfully uploads to NXRM3 (second.log)
- Log output contains expected publish messages with correct publisher ID
