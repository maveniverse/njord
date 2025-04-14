package eu.maveniverse.maven.njord.shared.store;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

public interface ArtifactStore extends Closeable {
    /**
     * Store name, never {@code null}.
     */
    String name();

    /**
     * Timestamp when this store was created, never {@code null}.
     */
    Instant created();

    /**
     * Store repository mode, never {@code null}.
     */
    RepositoryMode repositoryMode();

    /**
     * Is redeploy (artifact overwrite) allowed? Makes sense for release mode only, as since Maven 3 no snapshot is
     * overwritten.
     */
    boolean allowRedeploy();

    /**
     * The checksum algorithm factories this store uses, or empty if globally configured are wanted.
     */
    Optional<List<ChecksumAlgorithmFactory>> checksumAlgorithmFactories();

    /**
     * Index of artifacts in this store, never {@code null}.
     */
    Collection<Artifact> artifacts();

    /**
     * Index of metadata in this store, never {@code null}.
     */
    Collection<Metadata> metadata();

    /**
     * The store basedir, never {@code null}.
     */
    Path basedir();

    /**
     * Customizes the session to access this store.
     *
     * @see #storeRemoteRepository()
     */
    RepositorySystemSession storeRepositorySession(RepositorySystemSession session);

    /**
     * The Resolver remote repository access to underlying store.
     *
     * @see #storeRepositorySession(RepositorySystemSession)
     */
    RemoteRepository storeRemoteRepository();

    /**
     * Content modifying operation handle. Caller must close this instance, even if operation is canceled.
     */
    interface Operation extends Closeable {
        /**
         * Cancels the operation.
         */
        void cancel();
    }

    /**
     * Prepares artifact and metadata writes/puts and returns the handle. After write, caller must close the handle
     * to apply changes.
     */
    Operation put(Collection<Artifact> artifacts, Collection<Metadata> metadata) throws IOException;
}
