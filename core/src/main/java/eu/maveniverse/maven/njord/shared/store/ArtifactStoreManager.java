package eu.maveniverse.maven.njord.shared.store;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

public interface ArtifactStoreManager extends Closeable {
    /**
     * Lists store "probable names". Not all element name may be a store, check with {@link #selectArtifactStore(String)}.
     */
    Collection<String> listArtifactStoreNames() throws IOException;

    /**
     * Selects artifact store. If selected (optional is not empty), caller must close it.
     */
    Optional<ArtifactStore> selectArtifactStore(String name) throws IOException;

    /**
     * Creates store based on template name.
     */
    ArtifactStore createArtifactStore(String templateName) throws IOException;

    /**
     * Creates store based on template.
     */
    ArtifactStore createArtifactStore(ArtifactStoreTemplate template) throws IOException;

    /**
     * Closes and fully deletes store.
     */
    void dropArtifactStore(ArtifactStore artifactStore) throws IOException;
}
