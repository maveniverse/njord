# Nexus Repository 3 Test Environment

This guide helps you set up a local Nexus Repository 3 instance for testing the `sonatype-nx3` publisher.

## Starting Nexus

```bash
docker-compose up -d
```

This will start Nexus on http://localhost:8081

## Initial Setup

1. Wait for Nexus to start (can take 1-2 minutes). Check logs:
   ```bash
   docker-compose logs -f nexus
   ```

2. Get the initial admin password:
   ```bash
   docker exec nexus3-test cat /nexus-data/admin.password
   ```

3. Login to Nexus:
   - Open http://localhost:8081
   - Click "Sign in" (top right)
   - Username: `admin`
   - Password: (from step 2)

4. Follow the setup wizard:
   - Change admin password (e.g., to `admin123`)
   - Enable or disable anonymous access (your choice)

## Repository Configuration

By default, Nexus 3 comes with these Maven repositories:
- **maven-releases** (hosted) - for release artifacts
- **maven-snapshots** (hosted) - for snapshot artifacts
- **maven-central** (proxy) - proxy to Maven Central
- **maven-public** (group) - combines the above

These default repositories are perfect for testing the publisher.

## Testing the Publisher

### Configure settings.xml

Add server credentials to `~/.m2/settings.xml`:

```xml
<servers>
  <server>
    <id>nexus-test</id>
    <username>admin</username>
    <password>admin123</password>
  </server>
</servers>
```

### Configure pom.xml

Add to your project's `pom.xml`:

```xml
<distributionManagement>
  <repository>
    <id>nexus-test</id>
    <url>http://localhost:8081</url>
  </repository>
  <snapshotRepository>
    <id>nexus-test</id>
    <url>http://localhost:8081</url>
  </snapshotRepository>
</distributionManagement>

<properties>
  <njord.publisher.sonatype-nx3.releaseRepositoryName>maven-releases</njord.publisher.sonatype-nx3.releaseRepositoryName>
  <njord.publisher.sonatype-nx3.snapshotRepositoryName>maven-snapshots</njord.publisher.sonatype-nx3.snapshotRepositoryName>
  <njord.tag>test-deployment</njord.tag>
</properties>
```

### Publish with Njord

```bash
# Stage artifacts first
mvn njord:stage -Dstore=test-release

# Publish to Nexus
mvn njord:publish -Dstore=test-release -Dpublisher=sonatype-nx3
```

Or use the altDeploymentRepository approach:

```bash
mvn njord:publish \
  -Dstore=test-release \
  -Dpublisher=sonatype-nx3 \
  -DaltDeploymentRepository=nexus-test::http://localhost:8081 \
  -Dnjord.publisher.sonatype-nx3.releaseRepositoryName=maven-releases \
  -Dnjord.tag=my-custom-tag
```

## Verify Deployment

1. Browse to http://localhost:8081
2. Click "Browse" in left menu
3. Select "maven-releases" or "maven-snapshots"
4. Search for your artifacts
5. Check if tags are applied (if configured)

## API Testing

Test the Components API directly:

```bash
# Check API status
curl -u admin:admin123 http://localhost:8081/service/rest/v1/status

# List components in maven-releases
curl -u admin:admin123 http://localhost:8081/service/rest/v1/components?repository=maven-releases
```

## Stopping and Cleanup

```bash
# Stop Nexus (keeps data)
docker-compose stop

# Stop and remove container (keeps data volume)
docker-compose down

# Remove everything including data
docker-compose down -v
```

## Troubleshooting

### Nexus won't start
- Check if port 8081 is already in use: `lsof -i :8081`
- Check container logs: `docker-compose logs nexus`

### Cannot login
- Make sure you're using the correct initial password from `/nexus-data/admin.password`
- Try resetting by removing the data volume and restarting

### Upload fails with 403
- Verify credentials in settings.xml match Nexus
- Check that the repository exists and is not read-only
- Ensure the user has deployment privileges

### Upload fails with 422
- Check that repository name is correct
- Verify all required multipart fields are present
- Check Nexus logs: `docker exec nexus3-test tail -f /nexus-data/log/nexus.log`
