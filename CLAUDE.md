# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Njord (Njörðr) is a Maven extension and plugin suite that provides non-intrusive artifact publishing and local staging functionality. The goal is to decouple publishing logic from the build process, allowing projects to publish to various services (Sonatype Central Portal, Apache RAO, Nexus instances) without modifying their POMs.

**Key Concept**: Publishers are pluggable components that handle the "how" and "where" of artifact publishing, independent of the Maven build configuration.

## Build and Test Commands

### Building
```bash
# Full build (requires Java 21)
./mvnw clean install

# Build without tests
./mvnw clean install -DskipTests

# Build without integration tests
./mvnw clean install -DskipITs
```

### Testing
```bash
# Run unit tests only
./mvnw test

# Run integration tests (requires Docker for publisher tests)
./mvnw verify -Prun-its

# Run specific integration test module
mvnw verify -Prun-its -f it/publisher-sonatype-nx3-its
```

**Integration Test Requirements**:
- Integration tests in `it/publisher-sonatype-nx3-its/` use Testcontainers to spin up Nexus Repository OSS
- Docker must be running before executing integration tests
- Tests use maven-failsafe-plugin with JUnit 5 and Testcontainers
- Maven invoker component is used programmatically for invoking Maven builds in test projects

## High-Level Architecture

### Module Structure

```
njord/
├── core/                    # Core publishing abstractions and store management
├── extension3/              # Maven 3.x/4.x extension for altDeploymentRepository support
├── plugin/                  # Maven plugin with goals (publish, list, drop, validate, etc.)
├── publisher/               # Publisher implementations (each is a separate module)
│   ├── deploy/             # Generic deploy publisher
│   ├── apache/             # Apache RAO publisher (apache-rao)
│   ├── sonatype-cp/        # Sonatype Central Portal (sonatype-cp)
│   ├── sonatype-nx2/       # Nexus 2 instances (sonatype-oss, sonatype-s01)
│   └── sonatype-nx3/       # Nexus 3 instances (sonatype-nx3)
└── it/                      # Integration tests
    └── publisher-sonatype-nx3-its/  # Tests for Nexus 3 publisher
```

### Core Architecture Concepts

1. **ArtifactStore**: Local staging area for artifacts before publishing. Stores are created with templates (`release`, `snapshot`) and have unique names (e.g., `release-abc123`).

2. **Publisher SPI**: Publishers implement `ArtifactStorePublisher` interface:
   - Each publisher has a unique ID (e.g., `sonatype-cp`, `apache-rao`)
   - Publishers are discovered via Eclipse Sisu dependency injection
   - Factory pattern: `ArtifactStorePublisherFactory` creates publisher instances with configuration

3. **Configuration Resolution**: Publisher configuration can come from multiple sources (in priority order):
   - Plugin/goal parameters
   - User/project properties
   - Server configuration in settings.xml (with `njord.publisher` config element)
   - Server ID indirection (settings.xml can redirect one server ID to another)

4. **Extension vs Plugin**:
   - **extension3/**: Intercepts Maven deploy lifecycle, provides `njord:` URL protocol for altDeploymentRepository
   - **plugin/**: Provides CLI goals for managing stores and publishing (list, publish, drop, validate, etc.)

### Publisher Implementation Pattern

All publishers follow this structure:
```
publisher/publisher-name/
└── src/main/java/
    ├── PublisherFactory.java      # Creates publisher instances, registers with Sisu
    ├── PublisherConfig.java        # Configuration POJO
    └── Publisher.java              # Implementation of ArtifactStorePublisher
```

Publishers must:
- Implement `ArtifactStorePublisher` interface
- Provide a factory implementing `ArtifactStorePublisherFactory`
- Annotate factory with `@Named("publisher-id")` for Sisu discovery
- Define artifact store requirements (e.g., must have checksums, signatures)

### Integration Test Architecture (maven-failsafe-plugin + Testcontainers)

The `it/publisher-sonatype-nx3-its/` module demonstrates the integration test pattern:

1. **maven-failsafe-plugin + JUnit 5**: Runs integration tests as standard JUnit tests
   - Tests extend `AbstractNexusIT` base class which sets up Testcontainers
   - Downloads and uses specific Maven versions (3.9.x and 4.0.x) via maven-dependency-plugin
   - Test projects copied from `src/test/resources/test-projects/` to temp directories

2. **Testcontainers**: Manages Docker containers programmatically
   - `AbstractNexusIT` sets up Nexus Repository OSS container
   - Uses pre-configured database template to bypass EULA wizard
   - Automatic container lifecycle management (start before tests, stop after)
   - Built-in health checks via wait strategies

3. **Maven Invoker Component**: Programmatic Maven invocation
   - `MavenInvokerHelper` wraps maven-invoker API for easy test project execution
   - Supports multiple Maven goals, properties, and environment variables
   - Captures build output for assertions
   - Provides isolated user home and local repository directories

4. **Test Lifecycle**:
   - **@BeforeAll**: Sets up Nexus container and test environment
   - **@BeforeEach**: Copies test project to temp directory
   - **@Test**: Invokes Maven builds programmatically and asserts results
   - **@AfterAll**: Testcontainers automatically stops and removes container

5. **Multi-version Maven Testing**:
   - Separate test methods for Maven 3.9.x and Maven 4.x
   - Maven 4 tests use `@EnabledForJreRange(min = JRE.JAVA_17)` to skip on older JDKs
   - Maven distributions downloaded during `generate-test-resources` phase

### Property Naming Convention

Follow this pattern for version properties:
- `maven39Version` (not `maven.39.version`)
- `groovyVersion` (not `groovy.version`)
- `maven4Version` (not `maven.4.version`)

Rationale: Consistent camelCase naming matches existing patterns in the codebase.

## Working with Publishers

### Adding a New Publisher

1. Create new module under `publisher/` directory
2. Add module to `publisher/pom.xml`
3. Implement the three required classes (Factory, Config, Publisher)
4. Add dependency to parent `dependencyManagement` section
5. Register with Sisu using `@Named("publisher-id")` annotation
6. Create integration tests following the failsafe + Testcontainers pattern

### Testing Publisher Changes

When modifying a publisher, always test against both Maven 3.9.x and Maven 4.x:
```bash
cd it/publisher-YOUR-PUBLISHER-its/
mvn clean verify -Prun-its
```

Integration tests will:
1. Install artifacts to local test repository
2. Run tests with maven-failsafe-plugin
3. Tests execute against Maven 3.9.x (always) and Maven 4.x (only on Java 17+)
4. Testcontainers manages Docker lifecycle automatically

## Docker Integration Test Setup

Tests requiring external services (like Nexus) use Testcontainers:

**Directory Structure**:
```
module/
├── docker/
│   └── data-template/       # Pre-configured state templates
│       ├── db/             # H2 database with EULA accepted
│       └── .gitattributes  # Ensures binary treatment of .db files
└── src/test/
    ├── java/
    │   └── eu/maveniverse/maven/njord/publisher/nx3/
    │       ├── AbstractNexusIT.java       # Base test with Testcontainers setup
    │       ├── DeployReleaseIT.java       # Test case
    │       └── support/
    │           └── MavenInvokerHelper.java
    └── resources/
        └── test-projects/
            └── deploy-release/
                ├── pom.xml
                └── .mvn/
```

**Key Pattern**: Testcontainers automatically manages Docker lifecycle:
1. `@Testcontainers` annotation enables automatic container lifecycle management
2. `@Container` static field defines Nexus container with data template mounted
3. `Wait.forHttp()` strategy waits for health check endpoint to return 200
4. Container starts before `@BeforeAll` and stops after `@AfterAll`
5. No manual cleanup required - Testcontainers handles everything

## Documentation and Resources

- Project site: https://maveniverse.eu/docs/njord/
- Maven Central: https://search.maven.org/artifact/eu.maveniverse.maven.njord/extension3
- GitHub: https://github.com/maveniverse/njord

## Runtime vs Build Requirements

**Build Time**: Java 21, Maven 3.9.9+
**Runtime**: Java 8+, Maven 3.9+

Code must compile with `-release 8` target while being built on Java 21.
