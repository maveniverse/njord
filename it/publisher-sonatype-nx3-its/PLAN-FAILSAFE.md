# Refactoring Plan: Maven Invoker Plugin → Failsafe + Testcontainers

## Executive Summary

This plan outlines the refactoring of `publisher-sonatype-nx3-its` from maven-invoker-plugin to maven-failsafe-plugin with Testcontainers. The goal is to improve test reliability, lifecycle management, and CI/CD flexibility while maintaining the same test coverage.

## Current State Analysis

### Existing Architecture

**Test Framework:** maven-invoker-plugin
- Downloads Maven distributions (3.9.x, 4.x) via maven-dependency-plugin
- Clones test projects to `target/mvn39-it/` and `target/mvn4-it/`
- Uses Groovy scripts for setup/teardown:
  - `setup.groovy`: Starts Docker containers via DockerLib
  - `verify.groovy`: Asserts results + tears down containers in finally block
  - `invoker.properties`: Defines multiple Maven goal invocations

**Docker Management:**
- docker-compose.yml in `docker/` directory
- Manual process management via shell commands in Groovy
- Data template copying for fresh Nexus instances
- Health check polling via HTTP status endpoint

**Test Execution:**
- Two separate executions: mvn39-its-run and mvn4-its-run
- Maven 4 tests skip on Java < 17 via profile activation
- Tests run sequentially within each Maven version

**CI/CD:**
- Single GitHub Actions job using Maven 3.9.11
- Matrix testing happens within the build via invoker executions

### Pain Points

1. **Unreliable Cleanup:** Teardown in finally blocks can fail silently
2. **Limited Parallelization:** Tests run sequentially per Maven version
3. **Manual Docker Management:** Shell commands prone to environment issues
4. **CI Matrix Limitations:** Cannot easily test different Java/Maven combinations
5. **Debugging Difficulty:** Groovy script failures don't integrate well with standard tooling

## Target Architecture

### Test Framework: maven-failsafe-plugin + JUnit 5

**Structure:**
```
publisher-sonatype-nx3-its/
├── src/
│   ├── main/java/
│   │   └── (empty - packaging:jar required for failsafe)
│   └── test/
│       ├── java/
│       │   └── eu/maveniverse/maven/njord/publisher/nx3/
│       │       ├── AbstractNexusIT.java         # Base test class
│       │       ├── DeployReleaseIT.java         # Test case
│       │       └── support/
│       │           ├── NexusContainer.java      # Testcontainers wrapper
│       │           └── MavenInvokerHelper.java  # Maven invocation utilities
│       └── resources/
│           └── test-projects/
│               └── deploy-release/              # Test project POMs
│                   ├── pom.xml
│                   └── .mvn/
│                       └── signing-key.asc
├── docker/
│   ├── docker-compose.yml                       # (Optional reference)
│   └── data-template/db/                        # Database template for mounting
└── pom.xml
```

### Testcontainers Integration

**Container Lifecycle:**
```java
@Testcontainers
public abstract class AbstractNexusIT {

    @Container
    static final GenericContainer<?> nexus = new GenericContainer<>("sonatype/nexus3:latest")
        .withExposedPorts(8081)
        .withEnv("INSTALL4J_ADD_VM_PARAMS", "-Xms512m -Xmx1024m ...")
        .withFileSystemBind("docker/data-template/db", "/nexus-data/db", READ_ONLY)
        .waitingFor(Wait.forHttp("/service/rest/v1/status")
            .forStatusCode(200)
            .withStartupTimeout(Duration.ofMinutes(3)));

    @BeforeAll
    static void setupNexus() {
        // Container already started by @Testcontainers
        // Configure test environment
    }
}
```

**Benefits:**
- Automatic container lifecycle (start before tests, stop after)
- Built-in wait strategies (no manual polling)
- Parallel test execution support (isolated containers per test class)
- Better logging and debugging integration

### Maven Invoker Component Usage

**Programmatic Invocation:**
```java
public class MavenInvokerHelper {

    private final File mavenHome;
    private final File localRepo;
    private final File userHome;

    public InvocationResult invoke(File projectDir, String... goals) {
        InvocationRequest request = new DefaultInvocationRequest()
            .setBaseDirectory(projectDir)
            .setGoals(Arrays.asList(goals))
            .setLocalRepositoryDirectory(localRepo)
            .setMavenHome(mavenHome)
            .setUserSettingsFile(new File(userHome, ".m2/settings.xml"))
            .setOutputHandler(line -> System.out.println("[MVN] " + line));

        Invoker invoker = new DefaultInvoker();
        return invoker.execute(request);
    }
}
```

**Test Project Management:**
```java
@TempDir
File tempDir;

@BeforeEach
void setupTestProject() throws IOException {
    // Copy test project from resources to temp directory
    Path source = Paths.get("src/test/resources/test-projects/deploy-release");
    Path target = tempDir.toPath().resolve("deploy-release");
    FileUtils.copyDirectory(source.toFile(), target.toFile());
}
```

### Test Implementation Example

```java
@Testcontainers
class DeployReleaseIT extends AbstractNexusIT {

    @TempDir
    File tempDir;

    private MavenInvokerHelper invoker;

    @BeforeEach
    void setup() throws IOException {
        // Copy test project
        Path projectPath = tempDir.toPath().resolve("deploy-release");
        FileUtils.copyDirectory(
            new File("src/test/resources/test-projects/deploy-release"),
            projectPath.toFile()
        );

        // Initialize invoker with Maven version from system property
        String mavenVersion = System.getProperty("maven.version", "3.9.11");
        File mavenHome = new File("target/dependency/apache-maven-" + mavenVersion);
        invoker = new MavenInvokerHelper(mavenHome, getLocalRepo(), getUserHome());
    }

    @Test
    void testDeployRelease() throws Exception {
        File projectDir = tempDir.toPath().resolve("deploy-release").toFile();

        // 1. Clean any existing stores
        InvocationResult result1 = invoker.invoke(projectDir,
            "njord:" + getProjectVersion() + ":drop-all", "-Dyes");
        assertEquals(0, result1.getExitCode());

        // 2. Deploy to local staging
        InvocationResult result2 = invoker.invoke(projectDir,
            "clean", "deploy", "-P", "release");
        assertEquals(0, result2.getExitCode());

        // 3. Publish to Nexus
        InvocationResult result3 = invoker.invoke(projectDir,
            "njord:" + getProjectVersion() + ":publish",
            "-Dpublisher=sonatype-nx3", "-Ddetails");
        assertEquals(0, result3.getExitCode());

        // Assert build logs contain expected output
        assertThat(result2.getOutput())
            .contains("[INFO] Njord " + getProjectVersion() + " session created");
        assertThat(result3.getOutput())
            .contains("[INFO] Publishing nx3-deploy-release-")
            .contains("sonatype-nx3");
    }
}
```

## CI/CD Matrix Strategy

### GitHub Actions Workflow Enhancement

**Current:**
```yaml
jobs:
  build:
    uses: maveniverse/parent/.github/workflows/ci.yml@release-41
    with:
      maven-matrix: '[ "3.9.11" ]'
      maven-test: './mvnw clean verify -e -B -V -P run-its -f it'
```

**Proposed:**
```yaml
jobs:
  integration-tests:
    strategy:
      matrix:
        os: [ubuntu-latest]
        java: [21]
        maven: [3.9.11, 4.0.0-rc-3]
        exclude:
          # Maven 4 requires Java 17+
          - java: 8
            maven: 4.0.0-rc-3

    runs-on: ${{ matrix.os }}

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK ${{ matrix.java }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: 'temurin'

      - name: Download Maven ${{ matrix.maven }}
        run: |
          wget https://archive.apache.org/dist/maven/maven-3/${{ matrix.maven }}/binaries/apache-maven-${{ matrix.maven }}-bin.zip
          unzip -q apache-maven-${{ matrix.maven }}-bin.zip -d $HOME/maven-distros

      - name: Run Integration Tests
        run: |
          ./mvnw clean verify -P run-its -f it/publisher-sonatype-nx3-its \
            -Dmaven.version=${{ matrix.maven }} \
            -Dmaven.home=$HOME/maven-distros/apache-maven-${{ matrix.maven }}
```

**Benefits:**
- Visual matrix in GitHub Actions UI
- Parallel execution across matrix dimensions
- Independent test isolation
- Better failure reporting per combination

## Implementation Steps

### Phase 1: Setup (No Test Changes)

1. **Update POM packaging:**
   - Change from `<packaging>pom</packaging>` to `<packaging>jar</packaging>`
   - Required for maven-failsafe-plugin to execute

2. **Add dependencies:**
   ```xml
   <dependencies>
     <!-- Existing publisher dependencies... -->

     <!-- Testing Framework -->
     <dependency>
       <groupId>org.junit.jupiter</groupId>
       <artifactId>junit-jupiter</artifactId>
       <version>5.11.4</version>
       <scope>test</scope>
     </dependency>

     <!-- Testcontainers -->
     <dependency>
       <groupId>org.testcontainers</groupId>
       <artifactId>testcontainers</artifactId>
       <version>1.20.4</version>
       <scope>test</scope>
     </dependency>
     <dependency>
       <groupId>org.testcontainers</groupId>
       <artifactId>junit-jupiter</artifactId>
       <version>1.20.4</version>
       <scope>test</scope>
     </dependency>

     <!-- Maven Invoker (programmatic usage) -->
     <dependency>
       <groupId>org.apache.maven.shared</groupId>
       <artifactId>maven-invoker</artifactId>
       <version>3.3.0</version>
       <scope>test</scope>
     </dependency>

     <!-- Utilities -->
     <dependency>
       <groupId>org.assertj</groupId>
       <artifactId>assertj-core</artifactId>
       <version>3.27.3</version>
       <scope>test</scope>
     </dependency>
     <dependency>
       <groupId>commons-io</groupId>
       <artifactId>commons-io</artifactId>
       <version>2.18.0</version>
       <scope>test</scope>
     </dependency>
   </dependencies>
   ```

3. **Configure maven-failsafe-plugin:**
   ```xml
   <plugin>
     <groupId>org.apache.maven.plugins</groupId>
     <artifactId>maven-failsafe-plugin</artifactId>
     <executions>
       <execution>
         <goals>
           <goal>integration-test</goal>
           <goal>verify</goal>
         </goals>
       </execution>
     </executions>
     <configuration>
       <systemPropertyVariables>
         <maven.version>${maven39Version}</maven.version>
         <project.version>${project.version}</project.version>
       </systemPropertyVariables>
     </configuration>
   </plugin>
   ```

### Phase 2: Test Infrastructure

4. **Create base test class:**
   - `AbstractNexusIT.java`: Testcontainers setup, common utilities

5. **Create support classes:**
   - `NexusContainer.java`: Custom Testcontainers wrapper for Nexus
   - `MavenInvokerHelper.java`: Wraps maven-invoker API

6. **Migrate test projects:**
   - Move `src/it/deploy-release/` to `src/test/resources/test-projects/deploy-release/`
   - Keep POM, .mvn directory, etc.

### Phase 3: Migrate Tests

7. **Implement first test:**
   - `DeployReleaseIT.java`: Port deploy-release test logic
   - Verify test passes with Maven 3.9.x

8. **Add Maven 4 support:**
   - Parameterized test or separate test class
   - System property to select Maven version

### Phase 4: Remove Old Implementation

9. **Keep maven-invoker-plugin temporarily:**
   - Disable by default, enable with `-P invoker-tests` profile
   - Allows comparison during transition

10. **Remove Groovy scripts:**
    - Delete `src/it/scripts/DockerLib.groovy`
    - Delete `setup.groovy`, `verify.groovy` files

11. **Update documentation:**
    - Update CLAUDE.md with new test patterns
    - Add comments explaining Testcontainers usage

### Phase 5: CI/CD Integration

12. **Update `.github/workflows/ci.yml`:**
    - Add matrix strategy for Maven versions
    - Configure system properties for test execution

13. **Verify parallel execution:**
    - Ensure tests can run concurrently (isolated containers)

## Benefits

### Reliability
- **Guaranteed Cleanup:** Testcontainers ensures containers stop even on test failures
- **Better Wait Strategies:** Built-in health checks vs manual polling
- **JUnit Lifecycle:** Standard test framework guarantees vs custom Groovy scripts

### Developer Experience
- **Standard Tooling:** Run tests from IDE with standard JUnit runners
- **Better Debugging:** Breakpoints work in Java tests (vs Groovy scripts)
- **Faster Iteration:** No need to run full build to test Docker integration

### CI/CD Flexibility
- **Matrix Testing:** Easy to test multiple Java/Maven combinations
- **Parallel Execution:** GitHub Actions can run matrix jobs concurrently
- **Better Reporting:** JUnit reports integrate with CI dashboards

### Maintainability
- **Type Safety:** Java vs Groovy scripts
- **Reusable Components:** Share test utilities across test classes
- **Standard Patterns:** Familiar to Java developers

## Migration Risks & Mitigations

### Risk: Test Coverage Gaps
**Mitigation:** Keep invoker-based tests in parallel profile until parity verified

### Risk: Testcontainers Performance
**Mitigation:**
- Use testcontainers.reuse.enable for local development
- Share containers across test classes where possible

### Risk: CI Environment Compatibility
**Mitigation:**
- GitHub Actions supports Docker natively
- Document system requirements (Docker, disk space)

### Risk: Maven Version Management
**Mitigation:**
- Continue using maven-dependency-plugin to download distributions
- Reference via system property: `maven.home`

## Open Questions

### 1. Container Lifecycle Scope
**Question:** Should we use one Nexus container for all tests or one per test class?

**Options:**
- **Shared (`@Container` static):** Faster, but tests must not conflict
- **Per-class:** Slower, but complete isolation
- 
**Recommendation:** Start with shared, add per-class if needed

> We may have to do per-test, since the tests run in different versions of Maven and Java,
> but produce the same artifacts deployed to the repository.

### 2. Test Project Management
**Question:** Should test projects be in `src/test/resources/` or unpacked during build?

**Current:** maven-invoker-plugin clones to `target/mvn*-it/`

**Recommendation:** Copy from resources to `@TempDir` in each test for isolation

> Please follow the same style from maven-invoker-plugin.  As some of the files need to have 
> interpolation.  Check if the https://maven.apache.org/shared/maven-invoker/ component can help.

### 3. Maven Distribution Download
**Question:** Keep maven-dependency-plugin or let tests download on-demand?

**Options:**
- **Build-time (current):** Downloaded once, cached in target/dependency/
- **Test-time:** Download per test run (slower, more disk usage)

**Recommendation:** Keep build-time download, reference via system property

> Leave it as build-time for now.  We can revisit this later. 


### 4. Settings.xml Management
**Question:** How to manage user-home/.m2/settings.xml for tests?

**Options:**
- **Copy to @TempDir:** Full isolation, but repetitive
- **Shared location:** maven-antrun-plugin copies to target/it-user/

**Recommendation:** Copy settings.xml template to each @TempDir for isolation

> reproduce what maven-invoker-plugin is doing, its setting the HOME directory explicitly
> so that all files can be isolated in test workspaces "home directory".  This is what
> src/it/user-home is for to be the source of the home directory when copied to target/it-user/.


### 5. Local Repository Isolation
**Question:** Should each test use isolated local repository?

**Current:** All tests share `target/it-repo/`

**Recommendation:** Continue sharing (faster), document cleanup strategy

> keep the same behavior that maven-invoker-plugin is doing.  use a share repo, but this is
> isolated from the normal users repo.


### 6. Maven 4 Conditional Execution
**Question:** How to skip Maven 4 tests on Java < 17?

**Options:**
- **JUnit @EnabledOnJre:** Annotate test classes
- **Maven profile + surefire skip:** Build-time decision
- **Parameterized tests:** Single test class, skip certain parameters

**Recommendation:** Use JUnit `@EnabledOnJre(JAVA_17)` for Maven 4 test classes

> Sure use, Junit extensions to control this if that is possible.

### 7. Groovy Script Dependencies
**Question:** Can we remove Groovy dependencies entirely?

**Answer:** Yes - all logic moves to Java test classes

> Yes we can consider that.  check what the Maven Invoker component (not plugin) supports.
> Groovy may still be easiest for assering the actual behavior of the test, but lets see
> what it supports and then what the pure-java impl looks like.  

### 8. Data Template Handling
**Question:** How to mount data-template/db/ into Testcontainers?

**Options:**
- **withFileSystemBind:** Bind mount (simple, but host path dependent)
- **withCopyFileToContainer:** Copy during container startup (portable)
- **Custom image:** Bake template into Docker image (overkill)

**Recommendation:** Use `withFileSystemBind` for local dev, consider `withCopyFileToContainer` for CI

> bind as its doing now should be fine.


### 9. Backward Compatibility
**Question:** Should we keep maven-invoker-plugin tests during transition?

**Recommendation:** Yes, under disabled profile (`-P invoker-tests`) until full migration verified

> There is a branch with the current tests.  Only change the publisher-sonatype-nx3-its tests for now.
> you'll have to leave the other config asis for the extension-its to keep functioning.

### 10. Parallel Test Execution
**Question:** Can Failsafe run tests in parallel safely?

**Considerations:**
- Testcontainers can run multiple containers concurrently
- Each test class gets own Nexus instance (port mapping handled automatically)
- Must ensure test projects don't conflict in local repository

**Recommendation:** Enable with `<parallel>classes</parallel>` in failsafe configuration

> lets keep it simple for now, run serially.  we can revisit this later.

## Success Criteria

- [ ] All existing test scenarios pass with Failsafe
- [ ] Tests pass with both Maven 3.9.x and Maven 4.x
- [ ] CI workflow uses matrix strategy
- [ ] No manual Docker commands required
- [ ] Tests can run from IDE
- [ ] Cleanup is reliable (no leaked containers)
- [ ] Documentation updated

## Timeline Estimate

- Phase 1 (Setup): 1-2 hours
- Phase 2 (Infrastructure): 2-3 hours
- Phase 3 (Migrate Tests): 3-4 hours
- Phase 4 (Cleanup): 1 hour
- Phase 5 (CI/CD): 1-2 hours

**Total:** 8-12 hours

## References

- [Testcontainers Documentation](https://testcontainers.com/)
- [Maven Failsafe Plugin](https://maven.apache.org/surefire/maven-failsafe-plugin/)
- [Maven Invoker API](https://maven.apache.org/shared/maven-invoker/)
- [GitHub Actions Matrix Strategy](https://docs.github.com/en/actions/using-jobs/using-a-matrix-for-your-jobs)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)

---

## Appendix A: Composition vs Inheritance Pattern Analysis

### Current Implementation: Inheritance with @Testcontainers

The implementation uses the **inheritance pattern** with `@Testcontainers` annotation, which is the standard approach recommended by official Testcontainers documentation.

#### Architecture Overview

```java
@Testcontainers
public abstract class AbstractNexusIT {
    @Container
    protected static GenericContainer<?> nexus;

    @BeforeAll
    static void setupTestEnvironment() {
        // Setup shared test environment
    }

    @AfterEach
    void cleanupNexusData() {
        // Restart container with fresh data between tests
        nexus.stop();
        // Reset data template
        nexus.start();
    }
}

class DeployReleaseIT extends AbstractNexusIT {
    @Test
    void testDeployRelease() {
        // Test uses getNexusUrl(), getLocalRepository(), etc.
    }
}
```

### Pattern Comparison

#### Option 1: Inheritance + @Testcontainers (CURRENT - CHOSEN)

**How It Works:**
- Test classes extend `AbstractNexusIT`
- `@Testcontainers` JUnit extension finds `@Container` fields
- Static container field: Started once before all tests, stopped after all tests
- Manual restart in `@AfterEach` provides fresh state between tests

**Pros:**
- ✅ **Simple and standard**: Official Testcontainers pattern
- ✅ **Minimal boilerplate**: Test classes just extend base class
- ✅ **Automatic lifecycle**: Extension manages start/stop
- ✅ **Shared utilities**: Helper methods inherited by all tests
- ✅ **Familiar**: Most developers recognize this pattern

**Cons:**
- ❌ **Single inheritance**: Can only extend one base class
- ❌ **Less flexible**: All tests coupled to base class structure
- ❌ **Cannot compose**: Can't easily mix multiple container types

**When to Use:**
- ✅ Single container type (our case: only Nexus)
- ✅ No other base class needed
- ✅ Want minimal boilerplate in test classes

#### Option 2: Composition + @RegisterExtension

**How It Works:**
- Create custom JUnit extension implementing lifecycle callbacks
- Test classes register extension as a field
- Extension manages container lifecycle programmatically

**Implementation Example:**

```java
// NexusExtension.java - Custom JUnit Extension
public class NexusExtension implements BeforeAllCallback, AfterAllCallback, AfterEachCallback {

    private GenericContainer<?> nexus;
    private File localRepository;
    private File userHome;
    private final Path dataTemplatePath;

    public NexusExtension(Path dataTemplatePath) {
        this.dataTemplatePath = dataTemplatePath;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        // Start Nexus container
        nexus = new GenericContainer<>("sonatype/nexus3:latest")
            .withExposedPorts(8081)
            .withFileSystemBind(...)
            .waitingFor(Wait.forHttp("/service/rest/v1/status"));
        nexus.start();

        // Setup test environment
        setupTestEnvironment();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        // Restart with fresh data
        nexus.stop();
        // Copy fresh data template
        nexus.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (nexus != null) {
            nexus.stop();
        }
    }

    // Getters for test access
    public String getNexusUrl() {
        return "http://" + nexus.getHost() + ":" + nexus.getMappedPort(8081);
    }

    public File getLocalRepository() { return localRepository; }
    public File getUserHome() { return userHome; }
}

// Test class using composition
class DeployReleaseIT {

    @RegisterExtension
    static NexusExtension nexusExtension = new NexusExtension(
        Paths.get("docker/data-template")
    );

    private MavenInvokerHelper invoker;

    @BeforeEach
    void setup() {
        String nexusUrl = nexusExtension.getNexusUrl();
        File localRepo = nexusExtension.getLocalRepository();
        File userHome = nexusExtension.getUserHome();

        invoker = new MavenInvokerHelper(mavenHome, localRepo, userHome);
    }

    @Test
    void testDeployRelease() {
        // Test logic using nexusExtension
    }
}
```

**Pros:**
- ✅ **Composition over inheritance**: More flexible design
- ✅ **Multiple extensions**: Can combine multiple container types
- ✅ **Explicit dependencies**: Extension as field shows what test needs
- ✅ **Reusable**: Package extension as library for other projects
- ✅ **Testable**: Extension logic can be tested independently

**Cons:**
- ❌ **More complex**: Custom extension requires JUnit API knowledge
- ❌ **More boilerplate**: Each test class must wire up extension
- ❌ **Less familiar**: Custom pattern harder for new developers
- ❌ **Maintenance overhead**: Must maintain extension implementation

**When to Use:**
- ✅ Multiple container types needed (Nexus + Redis + PostgreSQL)
- ✅ Different configurations per test class
- ✅ Inheritance conflicts (need to extend another base class)
- ✅ Want to package as reusable library

#### Option 3: Wrapper Class (Simplified Composition)

**How It Works:**
- Create helper class (not a JUnit extension)
- Test classes manage lifecycle manually in `@BeforeAll`/`@AfterAll`

**Implementation Example:**

```java
// NexusTestEnvironment.java - Helper class
public class NexusTestEnvironment {

    private final GenericContainer<?> nexus;
    private final File localRepository;
    private final File userHome;

    public NexusTestEnvironment(Path dataTemplatePath) {
        this.nexus = new GenericContainer<>("sonatype/nexus3:latest")
            .withExposedPorts(8081)
            .withFileSystemBind(...)
            .waitingFor(Wait.forHttp("/service/rest/v1/status"));
    }

    public void start() {
        nexus.start();
        // Setup logic
    }

    public void restart() {
        nexus.stop();
        // Copy fresh data
        nexus.start();
    }

    public void stop() {
        nexus.stop();
    }

    public String getNexusUrl() { return ...; }
}

// Test class with manual lifecycle
class DeployReleaseIT {

    private static NexusTestEnvironment testEnv;

    @BeforeAll
    static void setupAll() {
        testEnv = new NexusTestEnvironment(Paths.get("docker/data-template"));
        testEnv.start();
    }

    @AfterEach
    void cleanupEach() {
        testEnv.restart();
    }

    @AfterAll
    static void teardownAll() {
        testEnv.stop();
    }

    @Test
    void testDeployRelease() {
        String url = testEnv.getNexusUrl();
        // Test logic
    }
}
```

**Pros:**
- ✅ **Simpler than custom extension**: No JUnit API knowledge needed
- ✅ **Explicit control**: Manual start/stop calls
- ✅ **Composition benefits**: Can use multiple helpers

**Cons:**
- ❌ **Manual lifecycle**: Must remember to call start/stop
- ❌ **Boilerplate in tests**: Each test needs lifecycle methods
- ❌ **Error-prone**: Easy to forget cleanup

**When to Use:**
- ⚠️ Generally not recommended - use inheritance or @RegisterExtension instead

### @RegisterExtension: Static vs Instance Fields

If using composition pattern, field type matters:

#### Static Extension (Recommended for Containers)

```java
@RegisterExtension
static NexusExtension nexus = new NexusExtension();
```

- ✅ Can implement `BeforeAllCallback`, `AfterAllCallback`
- ✅ Shared across all test methods
- ✅ Access to class-level lifecycle
- ✅ Container started once per test class

**Lifecycle Order:**
1. Extension registered after `@ExtendWith` but before test instance creation
2. `beforeAll()` called before any `@BeforeAll` methods
3. `afterAll()` called after all `@AfterAll` methods

#### Instance Extension (Limited Use)

```java
@RegisterExtension
NexusExtension nexus = new NexusExtension();
```

- ❌ **Cannot** implement `BeforeAllCallback`, `AfterAllCallback`
- ⚠️ Only method-level callbacks: `BeforeEachCallback`, `AfterEachCallback`
- New instance created per test method (if lifecycle is PER_METHOD)
- Limited access to container state

**Rule:** For container management, **always use static fields** with `@RegisterExtension`.

### Container Sharing Strategies

#### Strategy 1: Shared Static Container + Manual Restart (CURRENT)

```java
@Container
static GenericContainer<?> nexus = ...;

@AfterEach
void cleanup() {
    nexus.stop();
    // Reset state (copy fresh data)
    nexus.start();
}
```

**Characteristics:**
- ✅ Fast: Container started once, restarted (not recreated) between tests
- ✅ Isolated: Fresh state via restart
- ✅ Explicit: Clear what happens between tests

**Use Case:** Our implementation - balance between speed and isolation

#### Strategy 2: Per-Test Instance Container

```java
@Container
GenericContainer<?> nexus = ...;  // Non-static
```

**Characteristics:**
- ❌ Slow: Full container startup/shutdown per test method
- ✅ Complete isolation: Guaranteed fresh container
- ⚠️ Higher resource usage

**Use Case:** When tests heavily modify container state and restart isn't enough

#### Strategy 3: Reusable Containers (Development Only)

```java
static GenericContainer<?> nexus = new GenericContainer<>("nexus:latest")
    .withReuse(true);
```

With `~/.testcontainers.properties`:
```properties
testcontainers.reuse.enable=true
```

**Characteristics:**
- ✅ Fastest: Container persists across test runs
- ❌ State pollution: Requires manual cleanup between runs
- ❌ Not for CI/CD: Only for local development

**Use Case:** Local development with frequent test runs

### Critical Warning: Singleton + @Testcontainers Conflict

From official Testcontainers documentation:

> **"Using Singleton Containers Pattern with a common base class and using Testcontainers JUnit 5 Extension is a bad approach and will not work."**

The `@Testcontainers` extension tears down all containers after each test class, breaking subsequent tests that depend on singleton containers (e.g., through shared Spring application context).

**BAD - Extension conflicts with singleton:**
```java
@Testcontainers  // ❌ Will tear down singleton!
abstract class BaseTest {
    static Container c;
    static {
        c = new Container();
        c.start();
    }
}
```

**GOOD - Manual lifecycle for singleton:**
```java
// No @Testcontainers annotation
abstract class BaseTest {
    static Container c;
    static {
        c = new Container();
    }

    @BeforeAll
    static void start() {
        c.start();
    }

    @AfterAll
    static void stop() {
        c.stop();
    }
}
```

**Our Implementation:** ✅ Correctly uses `@Testcontainers` with per-class lifecycle and explicit restart (not singleton pattern).

### Decision Matrix

| Requirement | Inheritance | @RegisterExtension | Wrapper |
|-------------|-------------|-------------------|---------|
| Single container | ✅ Perfect | ⚠️ Overkill | ⚠️ Verbose |
| Multiple containers | ❌ Limited | ✅ Ideal | ✅ Good |
| No other base class | ✅ No conflict | ✅ No conflict | ✅ No conflict |
| Need other base class | ❌ Conflict | ✅ Compatible | ✅ Compatible |
| Minimal boilerplate | ✅ Minimal | ❌ More code | ❌ Most code |
| Standard pattern | ✅ Official | ⚠️ Less common | ❌ Manual |
| Reusable library | ❌ Coupled | ✅ Portable | ✅ Portable |
| Easy debugging | ✅ Simple | ⚠️ Extension API | ✅ Simple |

### Recommendation

**Keep the current inheritance pattern** because:

1. **Single container**: Only need Nexus, no composition required
2. **Standard approach**: Official Testcontainers pattern
3. **Works well**: Successfully manages lifecycle with restart logic
4. **Simple**: Minimal boilerplate in test classes
5. **Maintainable**: Future developers immediately recognize pattern

**Consider composition only if:**

- Need multiple container types (Nexus + Redis + PostgreSQL) in different combinations
- Different Nexus configurations per test class
- Must extend another base class (e.g., Spring test framework)
- Want to package NexusExtension as reusable library across projects

For current requirements, **inheritance is the right choice**.

### Additional Considerations

#### Container Log Management

Container logs can be captured via:

**Option 1: SLF4J (Removed)**
```java
.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("NEXUS")))
```

**Option 2: File-based (Current Implementation)**
```java
Path logFile = targetDir.resolve("nexus-container.log");
.withLogConsumer(outputFrame -> {
    Files.write(logFile, outputFrame.getUtf8String().getBytes(),
        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
})
```

Benefits of file-based logging:
- ✅ Cleaner console output
- ✅ Full logs preserved in `target/nexus-container.log`
- ✅ Easy to inspect failed CI runs
- ✅ No SLF4J configuration needed

#### Enhanced Restart for Debugging

```java
@AfterEach
void cleanupNexusData() throws IOException {
    // Skip restart for faster local development
    if (Boolean.getBoolean("skipNexusRestart")) {
        System.out.println("[DEBUG] Skipping Nexus restart (set by skipNexusRestart property)");
        return;
    }

    System.out.println("[CLEANUP] Restarting Nexus with fresh data");
    restartNexusWithFreshData();
}
```

Usage: `mvn verify -Prun-its -DskipNexusRestart=true`

### References

- [Testcontainers JUnit 5 Integration](https://java.testcontainers.org/test_framework_integration/junit_5/)
- [Container Lifecycle Management](https://testcontainers.com/guides/testcontainers-container-lifecycle/)
- [JUnit 5 @RegisterExtension API](https://junit.org/junit5/docs/current/api/org.junit.jupiter.api/org/junit/jupiter/api/extension/RegisterExtension.html)
- [Manual Lifecycle Control](https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/)
- [Singleton Containers Pattern](https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers)
