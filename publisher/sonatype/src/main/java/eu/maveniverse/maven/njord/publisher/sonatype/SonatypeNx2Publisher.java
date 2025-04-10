package eu.maveniverse.maven.njord.publisher.sonatype;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.impl.repository.ArtifactStoreDeployer;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public class SonatypeNx2Publisher implements ArtifactStorePublisher {
    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession session;
    private final RemoteRepository releasesRepository;
    private final RemoteRepository snapshotsRepository;

    public SonatypeNx2Publisher(
            RepositorySystem repositorySystem,
            RepositorySystemSession session,
            RemoteRepository releasesRepository,
            RemoteRepository snapshotsRepository) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.session = requireNonNull(session);
        this.releasesRepository = releasesRepository;
        this.snapshotsRepository = snapshotsRepository;
    }

    @Override
    public void publish(ArtifactStore artifactStore) throws IOException {
        requireNonNull(artifactStore);

        RemoteRepository repository = artifactStore.repositoryMode() == RepositoryMode.RELEASE
                ? this.releasesRepository
                : this.snapshotsRepository;
        if (repository == null) {
            throw new IllegalArgumentException("Repository mode " + artifactStore.repositoryMode()
                    + " not supported; provide RemoteRepository for it");
        }
        new ArtifactStoreDeployer(repositorySystem, session, repository).deploy(artifactStore);
    }
}
