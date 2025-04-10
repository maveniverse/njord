package eu.maveniverse.maven.njord.shared.repository;

import static java.util.Objects.requireNonNull;

public interface ArtifactStoreTemplate {
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

    ArtifactStoreTemplate RELEASE = create("release", RepositoryMode.RELEASE, false);

    ArtifactStoreTemplate RELEASE_REDEPLOY = create("release-redeploy", RepositoryMode.RELEASE, true);

    ArtifactStoreTemplate SNAPSHOT = create("snapshot", RepositoryMode.SNAPSHOT, false);

    static ArtifactStoreTemplate create(String name, RepositoryMode repositoryMode, boolean allowRedeploy) {
        return new Impl(name, repositoryMode, allowRedeploy);
    }

    class Impl implements ArtifactStoreTemplate {
        private final String name;
        private final RepositoryMode repositoryMode;
        private final boolean allowRedeploy;

        private Impl(String name, RepositoryMode repositoryMode, boolean allowRedeploy) {
            this.name = requireNonNull(name);
            this.repositoryMode = requireNonNull(repositoryMode);
            this.allowRedeploy = allowRedeploy;
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
    }
}
