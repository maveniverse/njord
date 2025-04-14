package eu.maveniverse.maven.njord.shared.store;

import static java.util.Objects.requireNonNull;

import java.util.List;

public interface ArtifactStoreTemplate {
    /**
     * The Maven default checksum algorithms.
     */
    List<String> DEFAULT_CHECKSUM_ALGORITHMS = List.of("SHA-1", "MD5");

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
     * The checksum algorithm factories this template created store uses.
     */
    List<String> checksumAlgorithmFactories();

    ArtifactStoreTemplate RELEASE = create("release", RepositoryMode.RELEASE, false, DEFAULT_CHECKSUM_ALGORITHMS);

    ArtifactStoreTemplate RELEASE_REDEPLOY =
            create("release-redeploy", RepositoryMode.RELEASE, true, DEFAULT_CHECKSUM_ALGORITHMS);

    ArtifactStoreTemplate SNAPSHOT = create("snapshot", RepositoryMode.SNAPSHOT, false, DEFAULT_CHECKSUM_ALGORITHMS);

    static ArtifactStoreTemplate create(
            String name,
            RepositoryMode repositoryMode,
            boolean allowRedeploy,
            List<String> checksumAlgorithmFactories) {
        return new Impl(name, repositoryMode, allowRedeploy, checksumAlgorithmFactories);
    }

    class Impl implements ArtifactStoreTemplate {
        private final String name;
        private final RepositoryMode repositoryMode;
        private final boolean allowRedeploy;
        private final List<String> checksumAlgorithmFactories;

        private Impl(
                String name,
                RepositoryMode repositoryMode,
                boolean allowRedeploy,
                List<String> checksumAlgorithmFactories) {
            this.name = requireNonNull(name);
            this.repositoryMode = requireNonNull(repositoryMode);
            this.allowRedeploy = allowRedeploy;
            this.checksumAlgorithmFactories = requireNonNull(checksumAlgorithmFactories);
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
        public List<String> checksumAlgorithmFactories() {
            return List.copyOf(checksumAlgorithmFactories);
        }
    }
}
