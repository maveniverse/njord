package eu.maveniverse.maven.njord.shared.store;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;

public interface ArtifactStoreTemplate {
    /**
     * The default checksum algorithms.
     */
    List<String> DEFAULT_CHECKSUM_ALGORITHMS = List.of("SHA-1", "MD5");

    /**
     * The default checksum algorithms.
     */
    List<String> STRONG_CHECKSUM_ALGORITHMS = List.of("SHA-512", "SHA-256", "SHA-1", "MD5");

    /**
     * The default extensions to omit checksums for.
     */
    List<String> DEFAULT_OMIT_CHECKSUMS_FOR_EXTENSIONS = List.of(".asc", ".sigstore", ".sigstore.json");

    /**
     * Template name.
     */
    String name();

    /**
     * Template prefix to use when creating distinct store names.
     */
    default String prefix() {
        return name();
    }

    /**
     * Repository mode to create store.
     */
    RepositoryMode repositoryMode();

    /**
     * Allow to redeploy mode to create store.
     */
    boolean allowRedeploy();

    /**
     * The checksum algorithm factories this template created store uses, or empty, if globally configured value should be used.
     */
    Optional<List<String>> checksumAlgorithmFactories();

    /**
     * The extensions that checksum creation should be omitted for, or empty, if globally configured value should be used.
     */
    Optional<List<String>> omitChecksumsForExtensions();

    ArtifactStoreTemplate RELEASE = create(
            "release",
            RepositoryMode.RELEASE,
            false,
            DEFAULT_CHECKSUM_ALGORITHMS,
            DEFAULT_OMIT_CHECKSUMS_FOR_EXTENSIONS);

    ArtifactStoreTemplate RELEASE_SCA = create(
            "release-sca",
            RepositoryMode.RELEASE,
            false,
            STRONG_CHECKSUM_ALGORITHMS,
            DEFAULT_OMIT_CHECKSUMS_FOR_EXTENSIONS);

    ArtifactStoreTemplate RELEASE_REDEPLOY = create(
            "release-redeploy",
            RepositoryMode.RELEASE,
            true,
            DEFAULT_CHECKSUM_ALGORITHMS,
            DEFAULT_OMIT_CHECKSUMS_FOR_EXTENSIONS);

    ArtifactStoreTemplate RELEASE_REDEPLOY_SCA = create(
            "release-redeploy-sca",
            RepositoryMode.RELEASE,
            true,
            STRONG_CHECKSUM_ALGORITHMS,
            DEFAULT_OMIT_CHECKSUMS_FOR_EXTENSIONS);

    ArtifactStoreTemplate SNAPSHOT = create(
            "snapshot",
            RepositoryMode.SNAPSHOT,
            false,
            DEFAULT_CHECKSUM_ALGORITHMS,
            DEFAULT_OMIT_CHECKSUMS_FOR_EXTENSIONS);

    ArtifactStoreTemplate SNAPSHOT_SCA = create(
            "snapshot-sca",
            RepositoryMode.SNAPSHOT,
            false,
            STRONG_CHECKSUM_ALGORITHMS,
            DEFAULT_OMIT_CHECKSUMS_FOR_EXTENSIONS);

    static ArtifactStoreTemplate create(
            String name,
            RepositoryMode repositoryMode,
            boolean allowRedeploy,
            List<String> checksumAlgorithmFactories,
            List<String> omitChecksumsForExtensions) {
        return new Impl(name, repositoryMode, allowRedeploy, checksumAlgorithmFactories, omitChecksumsForExtensions);
    }

    class Impl implements ArtifactStoreTemplate {
        private final String name;
        private final RepositoryMode repositoryMode;
        private final boolean allowRedeploy;
        private final List<String> checksumAlgorithmFactories;
        private final List<String> omitChecksumsForExtensions;

        private Impl(
                String name,
                RepositoryMode repositoryMode,
                boolean allowRedeploy,
                List<String> checksumAlgorithmFactories,
                List<String> omitChecksumsForExtensions) {
            this.name = requireNonNull(name);
            this.repositoryMode = requireNonNull(repositoryMode);
            this.allowRedeploy = allowRedeploy;
            this.checksumAlgorithmFactories = checksumAlgorithmFactories;
            this.omitChecksumsForExtensions = omitChecksumsForExtensions;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public RepositoryMode repositoryMode() {
            return repositoryMode;
        }

        @Override
        public boolean allowRedeploy() {
            return allowRedeploy;
        }

        @Override
        public Optional<List<String>> checksumAlgorithmFactories() {
            return Optional.ofNullable(checksumAlgorithmFactories);
        }

        public Optional<List<String>> omitChecksumsForExtensions() {
            return Optional.ofNullable(omitChecksumsForExtensions);
        }
    }
}
