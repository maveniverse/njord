# Deploy Release Test for Sonatype NX3

This integration test verifies that the sonatype-nx3 publisher can successfully deploy artifacts to a Nexus Repository 3 instance.

## Test Flow

1. **Clean**: Drop all existing staged artifacts
2. **Deploy**: Build and stage artifacts locally using the `release` profile (generates sources, javadoc, and signatures)
3. **Publish**: Publish staged artifacts to NXRM3 using the sonatype-nx3 publisher

## Configuration

- **Repository URL**: `http://localhost:8081` (NXRM3 instance started by docker-maven-plugin)
- **Release Repository**: `maven-releases`
- **Snapshot Repository**: `maven-snapshots`
- **Tag**: `test-deployment`
- **Credentials**: `admin/admin123` (configured in settings.xml)

## Verification

The test verifies:
- Njord session is created successfully
- Artifacts are staged during the deploy phase
- Publisher successfully uploads to NXRM3
- Log output contains expected publish messages
