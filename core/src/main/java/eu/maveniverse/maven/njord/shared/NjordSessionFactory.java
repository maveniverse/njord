package eu.maveniverse.maven.njord.shared;

import org.eclipse.aether.RepositorySystemSession;

public interface NjordSessionFactory {
    /**
     * Creates Njord session. Session must be closed once done with it.
     */
    NjordSession create(RepositorySystemSession session, Config config);
}
