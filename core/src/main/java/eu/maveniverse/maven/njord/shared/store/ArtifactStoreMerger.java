package eu.maveniverse.maven.njord.shared.store;

import java.io.Closeable;
import java.io.IOException;

public interface ArtifactStoreMerger extends Closeable {
    /**
     * Merges two stores by redeploying source store onto target.
     */
    void redeploy(ArtifactStore source, ArtifactStore target) throws IOException;

    /**
     * Merges two stores by inlining source store onto target.
     */
    void merge(ArtifactStore source, ArtifactStore target) throws IOException;
}
