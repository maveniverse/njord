package eu.maveniverse.maven.njord.shared.impl.repository;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.impl.factories.ArtifactStoreMergerFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreMerger;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

@Singleton
@Named
public class DefaultArtifactStoreMergerFactory implements ArtifactStoreMergerFactory {
    private final RepositorySystem repositorySystem;

    @Inject
    public DefaultArtifactStoreMergerFactory(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public ArtifactStoreMerger create(RepositorySystemSession session, Config config) {
        return new DefaultArtifactStoreMerger(config, repositorySystem, session);
    }
}
