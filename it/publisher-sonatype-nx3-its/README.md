# Sonatype Nexus Repository 3 Integration Tests

This module contains integration tests for the `sonatype-nx3` publisher.

## Overview

These tests verify that the sonatype-nx3 publisher correctly uploads Maven artifacts to a Nexus Repository 3 instance using the Components API.

## Architecture

The integration tests use:
- **docker compose** (via exec-maven-plugin) to start/stop a Nexus Repository 3 Docker container
- **maven-invoker-plugin** to run test Maven projects
- **maven-antrun-plugin** to prepare test user home directory
- **maven-dependency-plugin** to prepare Maven distributions for testing

## Requirements

- Docker with Docker Compose installed and running
- Maven 3.9.9 or higher
- Java 8 or higher (Java 17+ for Maven 4 tests)
- Port 8081 available for Nexus

## Running the Tests

### Run all integration tests

```bash
cd /Users/jason/ws/maveniverse/njord
mvn clean install -Prun-its -pl it/publisher-sonatype-nx3-its
```

### Run only the publisher-sonatype-nx3-its module

```bash
cd /Users/jason/ws/maveniverse/njord/it/publisher-sonatype-nx3-its
mvn clean verify -Prun-its
```

## What Happens During Tests

1. **Pre-integration-test phase**:
   - Docker Compose starts Nexus Repository 3 container (using `docker/docker-compose.yml`)
   - Waits for Nexus to be ready via `--wait` flag (health check on `/service/rest/v1/status`)
   - Maven distributions are unpacked for test execution
   - Test user home directory is prepared with settings.xml

2. **Integration-test phase**:
   - Test projects are cloned to target directory
   - Maven Invoker Plugin runs test builds using both Maven 3.9 and Maven 4
   - Each test project goes through:
     - Clean staged artifacts
     - Build and stage artifacts (with sources, javadoc, signatures)
     - Publish to NXRM3 using sonatype-nx3 publisher
   - Verification scripts check that publish was successful

3. **Post-integration-test phase**:
   - Docker Compose stops and removes Nexus container (with volumes cleaned up)

## Test Cases

### deploy-release

Tests basic release deployment to NXRM3:
- Builds a simple Maven project with sources, javadoc, and GPG signatures
- Stages artifacts using Njord
- Publishes to maven-releases repository in Nexus
- Verifies successful upload via log output

Configuration:
- Repository: `maven-releases`
- URL: `http://localhost:8081`
- Tag: `test-deployment` (configured, but will be omitted on OSS with a warning)

## Configuration

### NXRM3 Container

The test uses Docker Compose (`docker/docker-compose.yml`) with the official `sonatype/nexus3:latest` image:
- Port 8081 exposed
- 512MB initial heap, 1GB max heap
- Health check polling `/service/rest/v1/status`
- 120 second startup grace period
- Data stored in `docker/data/` directory

### Maven Settings

Test settings.xml includes:
- Server ID: `nexus-test`
- Credentials: `admin/admin123` (default Nexus admin, password set by test)
- Publisher: `sonatype-nx3`
- Local staging configured

### Repository Names

- **Release**: `maven-releases` (default NXRM3 hosted repository)
- **Snapshot**: `maven-snapshots` (default NXRM3 hosted repository)

### Important Limitations

**Automatic Tagging Detection (OSS vs Pro):**
- The test uses Nexus Repository OSS (via `sonatype/nexus3:latest` Docker image)
- Component tagging is only available in Nexus Repository Pro
- The publisher automatically detects the server edition by checking the `Server` header from `/v1/status`:
  - OSS/Community: `Nexus/3.x.x (COMMUNITY)` or `Nexus/3.x.x (OSS)`
  - Pro: `Nexus/3.x.x (PRO)`
- If a tag is configured (`njord.tag`) but the server is OSS:
  - A warning is logged: `Tag 'xxx' is configured but Nexus Repository OSS does not support tagging...`
  - The tag is automatically omitted from the upload
  - The upload succeeds without the tag
- If the server is Pro and a tag is configured, it will be included in the upload

## Troubleshooting

### Port 8081 already in use

Check if another Nexus instance is running:
```bash
lsof -i :8081
docker ps
```

Stop the conflicting container:
```bash
docker stop nexus3-its
```

### Nexus container fails to start

Check Docker logs:
```bash
docker logs nexus3-its
```

### Tests timeout waiting for Nexus

Docker Compose `--wait` flag waits for the health check to pass. If it times out:
- Check Docker resources (memory, CPU)
- Check Docker logs: `docker logs nexus3-its`
- Try pulling the latest Nexus image: `docker pull sonatype/nexus3:latest`

### Authentication failures

The tests assume Nexus admin password is `admin123`. On first start, Nexus generates a random password. If you see 401 errors:
- Check that the container is fresh (volumes are cleaned up after each test run)
- Verify settings.xml has correct credentials
- Check `docker/data/admin.password` for the actual password

### Test fails but container keeps running

Manually stop and clean up:
```bash
cd docker
docker compose down -v
```

## Manual Testing

You can also manually test against the Docker container:

1. Start Nexus:
```bash
cd /Users/jason/ws/maveniverse/njord/it/publisher-sonatype-nx3-its/docker
docker compose up -d
```

2. Wait for startup and get admin password:
```bash
docker compose logs -f nexus
# Wait for "Started Sonatype Nexus" message
docker exec nexus3-its cat /nexus-data/admin.password
```

3. Login and change password:
- Open http://localhost:8081
- Login as `admin` with password from step 2
- Change password to `admin123`

4. Run a test deployment:
```bash
cd ../src/it/deploy-release
mvn clean deploy -P release -Dnjord.publisher=sonatype-nx3
mvn njord:publish -Ddetails
```

5. Stop Nexus:
```bash
cd ../../docker
docker compose down
```

## References

- [Docker Maven Plugin Documentation](https://dmp.fabric8.io/)
- [Maven Invoker Plugin Documentation](https://maven.apache.org/plugins/maven-invoker-plugin/)
- [Nexus Repository 3 Docker Image](https://hub.docker.com/r/sonatype/nexus3)
