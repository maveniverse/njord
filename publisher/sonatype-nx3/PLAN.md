# Sonatype Nexus Repository 3 Publisher - Implementation Plan

## Overview
This document outlines the plan for implementing a new publisher for Sonatype Nexus Repository 3 (NXRM3) using the Components API. The publisher will upload locally staged artifacts grouped by Maven component (GAV coordinates) with optional tagging support.

**Key Design Decision**: This publisher follows the same repository configuration approach as the `deploy` publisher - using either `altDeploymentRepository` or the POM's `<distributionManagement>` section. The only NXRM3-specific configuration required is the repository name (the hosted repository name in Nexus) and an optional tag (defaults to `${groupId}-${artifactId}-${version}`).

## Background

### Current State
The njord project currently supports several publishers:
- **sonatype-nx2**: Deploys to Nexus Repository 2 (OSS/S01) using Maven deploy protocol
- **sonatype-cp**: Deploys to Sonatype Central Portal using custom API (ZIP bundle upload)
- **apache**: Deploys to Apache RAO
- **deploy**: Generic deploy to any Maven repository

### NXRM3 Components API
Nexus Repository 3 provides a REST API at `/v1/components` that allows uploading Maven components via multipart form-data. Key features:
- Upload multiple assets (jar, pom, sources, javadoc, etc.) as a single Maven component
- Supports Maven classifiers and extensions
- Optional tag support for logical grouping
- Supports both release and snapshot repositories

## Architecture

### Module Structure
```
publisher/sonatype-nx3/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── eu/maveniverse/maven/njord/publisher/sonatype/nx3/
                ├── SonatypeNx3Publisher.java         # Main publisher implementation
                ├── SonatypeNx3PublisherFactory.java  # DI factory (singleton)
                └── SonatypeNx3PublisherConfig.java   # Configuration
```

### Key Components

#### 1. SonatypeNx3Publisher
**Purpose**: Main publisher implementation that groups artifacts by GAV and uploads them as components

**Extends**: `ArtifactStorePublisherSupport`

**Constructor**: Receives RemoteRepository instances (release and snapshot) from factory, along with config for NXRM3-specific settings (repository names and tag)

**Key Methods**:
- `doPublish(ArtifactStore artifactStore)`: Main publishing logic
  - Group artifacts by GAV (groupId:artifactId:version)
  - For each GAV group, create multipart upload with all assets
  - Handle POM, JAR, sources, javadoc, and other classifiers
  - Apply tag to components (from config)

**Implementation Approach**:
1. Select the appropriate repository (release vs snapshot) - obtained from factory (either altDeploymentRepository or distributionManagement)
2. Get the NXRM3 repository name from config (required for Components API)
3. Get the tag from config (defaults to ${groupId}-${artifactId}-${version} from top-level project)
4. Check for dry run mode
5. Group artifacts from the store by GAV coordinates
6. For each component (GAV group):
   - Build multipart form-data request with all assets
   - Identify POM, main artifact, and classified artifacts
   - Apply tag to the component
   - Execute HTTP POST to `{repositoryUrl}/service/rest/v1/components?repository={repoName}`
7. Handle authentication via `ArtifactPublisherRedirector`
8. Use MavenHttpClient4FactoryImpl for HTTP client creation

**API Interaction**:
- Endpoint: `POST {baseUrl}/service/rest/v1/components?repository={repositoryName}`
- Content-Type: `multipart/form-data`
- Authentication: Bearer token or Basic Auth from settings.xml

**Multipart Fields**:
- `maven2.groupId`: Maven group ID
- `maven2.artifactId`: Maven artifact ID
- `maven2.version`: Maven version
- `maven2.generate-pom`: false (we provide the POM)
- `maven2.asset1`: POM file
- `maven2.asset1.extension`: pom
- `maven2.asset2`: Main artifact (jar, etc.)
- `maven2.asset2.extension`: jar (or other)
- `maven2.asset3`: Classified artifact (sources)
- `maven2.asset3.classifier`: sources
- `maven2.asset3.extension`: jar
- ... (additional assets as needed)
- `tag`: Optional tag name

**Error Handling**:
- 403: Insufficient permissions - throw meaningful exception
- 422: Missing parameters - validate before upload
- Other errors: Wrap in PublishFailedException

#### 2. SonatypeNx3PublisherFactory
**Purpose**: Factory for creating publisher instances

**Annotations**:
- `@Singleton`
- `@Named(SonatypeNx3PublisherFactory.NAME)`

**Name Constant**: `"sonatype-nx3"`

**Implements**: `ArtifactStorePublisherFactory` (NOT MavenCentralPublisherFactory, as this is for private/corporate repos)

**Dependencies**:
- `RepositorySystem` (injected)

**create() Method** (follows same pattern as `deploy` publisher):
1. Check for `altDeploymentRepository` property first (id::url format)
   - If present, parse it and use for both release and snapshot repositories
2. Fallback to POM's `<distributionManagement>` repositories if no `altDeploymentRepository`
3. Create `SonatypeNx3PublisherConfig` from session for NXRM3-specific settings
4. Create and return `SonatypeNx3Publisher` instance
5. Pass basic requirements (no specific validation needed for NX3)

#### 3. SonatypeNx3PublisherConfig
**Purpose**: Configuration handling for NXRM3-specific settings

**Does NOT extend**: `PublisherConfig` (repository configuration handled by factory via altDeploymentRepository/distributionManagement)

**Configuration Properties** (prefix: `njord.publisher.sonatype-nx3.`):
- `releaseRepositoryName`: Name of the hosted release repository in NXRM3 (required, e.g., "maven-releases")
- `snapshotRepositoryName`: Name of the hosted snapshot repository in NXRM3 (optional, e.g., "maven-snapshots")
- `tag`: Optional tag to apply to all uploaded components (default: `${project.groupId}-${project.artifactId}-${project.version}`)
- `connectTimeout`: HTTP connect timeout (default: PT30S)
- `requestTimeout`: HTTP request timeout (default: PT5M)

**Additional Configuration Aliases** (for convenience):
- `njord.tag`: Alias for tag configuration

**Methods**:
- `releaseRepositoryName()`: Returns the release repo name (required)
- `snapshotRepositoryName()`: Returns the snapshot repo name (optional)
- `tag()`: Returns tag string, with default from project coordinates
- `connectTimeout()`: Returns Duration
- `requestTimeout()`: Returns Duration

### Artifact Grouping Strategy

The key challenge is grouping artifacts from the ArtifactStore by their Maven coordinates:

```java
// Pseudo-code for grouping
Map<String, List<Artifact>> componentGroups = new HashMap<>();
for (Artifact artifact : artifactStore.artifacts()) {
    String gav = artifact.getGroupId() + ":" +
                 artifact.getArtifactId() + ":" +
                 artifact.getVersion();
    componentGroups.computeIfAbsent(gav, k -> new ArrayList<>()).add(artifact);
}
```

Each group represents a single component upload with multiple assets.

### Asset Ordering and Identification

For each component group, we need to identify:
1. **POM file**: `extension="pom"`, no classifier
2. **Main artifact**: Primary artifact (often jar), no classifier (unless POM specifies packaging)
3. **Classified artifacts**: sources, javadoc, tests, etc.
4. **Signature files**: .asc files should be uploaded as assets with appropriate extensions

### HTTP Client Implementation

Use the existing MavenHttpClient4FactoryImpl pattern from sonatype-cp:
```java
MavenHttpClient4FactoryImpl mhc4 = new MavenHttpClient4FactoryImpl(repositorySystem);
CloseableHttpClient httpClient = mhc4.createDeploymentClient(
    session.config().session(), repository).build();
```

### Authentication

Leverage the existing authentication infrastructure:
1. Use `session.artifactPublisherRedirector().getAuthRepositoryId(repository)` to get auth source
2. Use `AuthenticationContext.forRepository()` to retrieve credentials
3. Support both Basic Auth and Bearer tokens
4. Include extra headers from session configuration

## Module Dependencies

### pom.xml
```xml
<dependencies>
  <dependency>
    <groupId>eu.maveniverse.maven.njord</groupId>
    <artifactId>core</artifactId>
  </dependency>
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <scope>provided</scope>
  </dependency>
  <dependency>
    <groupId>org.apache.maven.resolver</groupId>
    <artifactId>maven-resolver-api</artifactId>
    <scope>provided</scope>
  </dependency>
  <dependency>
    <groupId>javax.inject</groupId>
    <artifactId>javax.inject</artifactId>
    <scope>provided</scope>
  </dependency>
  <dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
    <scope>provided</scope>
  </dependency>
  <dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpmime</artifactId>
    <scope>provided</scope>
  </dependency>
</dependencies>
```

## Configuration Example

### Approach 1: Using POM's distributionManagement (recommended)

Configure authentication in `settings.xml`:
```xml
<server>
  <id>my-nexus</id>
  <username>deployment-user</username>
  <password>deployment-password</password>
</server>
```

Configure repository in `pom.xml`:
```xml
<distributionManagement>
  <repository>
    <id>my-nexus</id>
    <url>https://nexus.example.com</url>
  </repository>
  <snapshotRepository>
    <id>my-nexus</id>
    <url>https://nexus.example.com</url>
  </snapshotRepository>
</distributionManagement>

<properties>
  <!-- NXRM3-specific: the name of the hosted repository in Nexus -->
  <njord.publisher.sonatype-nx3.releaseRepositoryName>maven-releases</njord.publisher.sonatype-nx3.releaseRepositoryName>
  <njord.publisher.sonatype-nx3.snapshotRepositoryName>maven-snapshots</njord.publisher.sonatype-nx3.snapshotRepositoryName>
  <!-- Optional: custom tag (defaults to ${groupId}-${artifactId}-${version}) -->
  <njord.tag>release-2024-10</njord.tag>
</properties>
```

Then publish:
```bash
mvn njord:publish -Dstore=release-xxx -Dpublisher=sonatype-nx3
```

### Approach 2: Using altDeploymentRepository (like maven-deploy-plugin)

```bash
mvn njord:publish \
  -Dstore=release-xxx \
  -Dpublisher=sonatype-nx3 \
  -DaltDeploymentRepository=my-nexus::https://nexus.example.com \
  -Dnjord.publisher.sonatype-nx3.releaseRepositoryName=maven-releases \
  -Dnjord.tag=my-custom-tag
```

### Approach 3: Minimal (using defaults)

If you configure distributionManagement in POM and the repository names are standard:
```bash
mvn njord:publish \
  -Dstore=release-xxx \
  -Dpublisher=sonatype-nx3 \
  -Dnjord.publisher.sonatype-nx3.releaseRepositoryName=maven-releases
```

The tag will automatically default to `${groupId}-${artifactId}-${version}` from the top-level project.

## Implementation Steps

### Phase 1: Module Setup
1. Create `publisher/sonatype-nx3/pom.xml` with dependencies
2. Add module to `publisher/pom.xml`
3. Add dependency management to root `pom.xml`

### Phase 2: Core Implementation
1. Implement `SonatypeNx3PublisherConfig`
   - Extend `PublisherConfig`
   - Add NXRM3-specific configuration properties
   - Implement getters and validation

2. Implement `SonatypeNx3PublisherFactory`
   - Add `@Singleton` and `@Named` annotations
   - Implement factory pattern
   - Wire up dependencies

3. Implement `SonatypeNx3Publisher`
   - Extend `ArtifactStorePublisherSupport`
   - Implement `doPublish()` method
   - Add artifact grouping logic
   - Implement multipart upload
   - Add authentication handling
   - Add error handling

### Phase 3: Testing
1. Create integration test with test NXRM3 instance
2. Test single-artifact upload
3. Test multi-artifact upload (POM + JAR + sources + javadoc)
4. Test with tags
5. Test authentication scenarios
6. Test error conditions (permissions, missing repo, etc.)

### Phase 4: Documentation
1. Update README.md with new publisher
2. Add configuration examples
3. Document supported properties
4. Add troubleshooting section

## Technical Considerations

### Asset Limit
Nexus Repository 3 API supports up to 3 assets per upload in some documentation examples, but the actual limit may be higher. We should:
- Start with supporting up to 10 assets (typical for most Maven components)
- Log a warning if more assets are found
- Consider batching if needed

### Checksum Handling
NXRM3 automatically generates checksums for uploaded artifacts. We should:
- NOT upload .md5 or .sha1 files as separate assets
- Filter out checksum artifacts from the upload
- Let NXRM3 generate its own checksums

### POM Generation
The API supports `maven2.generate-pom=true`, but we should always:
- Set `maven2.generate-pom=false`
- Upload the actual POM from the artifact store
- This ensures metadata integrity

### Signature Files
GPG signature files (.asc) should be:
- Uploaded as assets with classifier matching the signed artifact
- Example: `artifact-1.0.0.jar.asc` → asset with `.jar` extension and `.asc` extension variant
- Need to investigate exact API support for signatures

### Metadata Files
Maven metadata files (`maven-metadata.xml`) are typically:
- Auto-generated by NXRM3
- Should NOT be uploaded via Components API
- Filter these out from the artifact store

### Snapshot Handling
For snapshots:
- Use snapshot repository name
- NXRM3 handles timestamp generation
- Unique snapshots are supported

## Alternative Approaches Considered

### 1. Use Maven Deploy Protocol
- **Pros**: Simpler, reuse existing deployment infrastructure
- **Cons**: Cannot group artifacts or apply tags, same as nx2 publisher
- **Decision**: Rejected - doesn't meet requirement for component grouping and tagging

### 2. Upload as Bundle (like sonatype-cp)
- **Pros**: Single upload operation
- **Cons**: NXRM3 doesn't support ZIP bundle upload for Maven repositories
- **Decision**: Rejected - not supported by NXRM3 API

### 3. Upload Each Artifact Individually
- **Pros**: Simplest implementation
- **Cons**: Creates separate components for each artifact, doesn't group properly
- **Decision**: Rejected - doesn't meet grouping requirement

## Open Questions

1. **Asset Limit**: What is the actual limit for assets per component upload in NXRM3?
   - **Investigation needed**: Test with NXRM3 instance or check latest API docs
   - **Mitigation**: If limit exists, we could upload base component first, then use update API

2. **Signature File Handling**: How should .asc files be uploaded?
   - **Investigation needed**: Test with NXRM3 to see if they should be separate assets or handled differently
   - **Possible approach**: Upload as assets with appropriate extension/classifier

3. **Metadata Handling**: Should we filter maven-metadata.xml files?
   - **Decision**: Yes, filter them out as NXRM3 auto-generates these

4. **Repository Name vs ID**: Should we support repository name in URL path vs query param?
   - **Investigation needed**: Check if NXRM3 API supports different URL patterns
   - **Current approach**: Use query parameter as shown in API docs

## Success Criteria

1. Successfully upload a multi-module Maven project to NXRM3
2. All artifacts grouped correctly by component (GAV)
3. Tags applied correctly when configured
4. Authentication works with settings.xml credentials
5. Error handling provides clear messages
6. Both release and snapshot repositories supported
7. Dry-run mode works correctly

## References

- [Nexus Repository API Documentation](https://help.sonatype.com/en/api-reference.html)
- [OpenAPI Specification](https://sonatype.github.io/sonatype-documentation/api/nexus-repository/latest/nexus-repository-api.json)
- [Components API Guide](https://help.sonatype.com/en/components-api.html)
- [Tagging Guide](https://help.sonatype.com/en/tagging.html)
- Existing publisher implementations: `sonatype-nx2`, `sonatype-cp`
