package eu.maveniverse.maven.njord.publisher.sonatype;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.impl.CloseableSupport;
import eu.maveniverse.maven.njord.shared.impl.repository.ArtifactStoreDeployer;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreValidator;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public class SonatypeNx2Publisher extends CloseableSupport implements ArtifactStorePublisher {
    private final String serviceName;
    private final String serviceDescription;
    private final RemoteRepository targetReleaseRepository;
    private final RemoteRepository targetSnapshotRepository;

    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession session;
    private final RemoteRepository releasesRepository;
    private final RemoteRepository snapshotsRepository;

    public SonatypeNx2Publisher(
            String serviceName,
            String serviceDescription,
            RemoteRepository targetReleaseRepository,
            RemoteRepository targetSnapshotRepository,
            RepositorySystem repositorySystem,
            RepositorySystemSession session,
            RemoteRepository releasesRepository,
            RemoteRepository snapshotsRepository) {
        this.serviceName = requireNonNull(serviceName);
        this.serviceDescription = requireNonNull(serviceDescription);
        this.targetReleaseRepository = targetReleaseRepository;
        this.targetSnapshotRepository = targetSnapshotRepository;
        this.repositorySystem = requireNonNull(repositorySystem);
        this.session = requireNonNull(session);
        this.releasesRepository = releasesRepository;
        this.snapshotsRepository = snapshotsRepository;
    }

    @Override
    public String name() {
        return serviceName;
    }

    @Override
    public String description() {
        return serviceDescription;
    }

    @Override
    public Optional<RemoteRepository> targetReleaseRepository() {
        return Optional.ofNullable(targetReleaseRepository);
    }

    @Override
    public Optional<RemoteRepository> targetSnapshotRepository() {
        return Optional.ofNullable(targetSnapshotRepository);
    }

    @Override
    public Optional<RemoteRepository> serviceReleaseRepository() {
        return Optional.ofNullable(releasesRepository);
    }

    @Override
    public Optional<RemoteRepository> serviceSnapshotRepository() {
        return Optional.ofNullable(snapshotsRepository);
    }

    @Override
    public Optional<ArtifactStoreValidator> validator() {
        return Optional.empty();
    }

    @Override
    public void publish(ArtifactStore artifactStore) throws IOException {
        requireNonNull(artifactStore);
        checkClosed();

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
