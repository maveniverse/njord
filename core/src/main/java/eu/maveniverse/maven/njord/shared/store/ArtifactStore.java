package eu.maveniverse.maven.njord.shared.store;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;

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
