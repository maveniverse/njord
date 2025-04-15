package eu.maveniverse.maven.njord.shared.store;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import org.eclipse.aether.RepositorySystemSession;

public interface ArtifactStoreManager {
    /**
     * Lists store "probable names". Not all element name may be a store, check with {@link #selectArtifactStore(String)}.
     */
    Collection<String> listArtifactStoreNames() throws IOException;

    /**
     * Selects artifact store. If selected (optional is not empty), caller must close it.
     */
    Optional<ArtifactStore> selectArtifactStore(String name) throws IOException;

    /**
     * Returns the default template.
     */
    ArtifactStoreTemplate defaultTemplate();

    /**
     * List templates.
     */
    Collection<ArtifactStoreTemplate> listTemplates();

    /**
     * Creates store based on template.
     */
    ArtifactStore createArtifactStore(RepositorySystemSession session, ArtifactStoreTemplate template)
            throws IOException;

    /**
     * Closes and fully deletes store.
     */
    void dropArtifactStore(ArtifactStore artifactStore) throws IOException;
}
