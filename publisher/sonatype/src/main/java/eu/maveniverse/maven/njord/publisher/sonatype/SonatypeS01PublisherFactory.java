package eu.maveniverse.maven.njord.publisher.sonatype;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactory;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

@Singleton
@Named(SonatypeS01PublisherFactory.NAME)
public class SonatypeS01PublisherFactory implements ArtifactStorePublisherFactory {
    public static final String NAME = "sonatype-s01";

    private final RepositorySystem repositorySystem;

    @Inject
    public SonatypeS01PublisherFactory(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public ArtifactStorePublisher create(RepositorySystemSession session, Config config) throws IOException {
        SonatypeS01PublisherConfig s01Config = SonatypeS01PublisherConfig.with(config);
        RemoteRepository releasesRepository = new RemoteRepository.Builder(
                        s01Config.releaseRepositoryId(), "default", s01Config.releaseRepositoryUrl())
                .build();
        RemoteRepository snapshotsRepository = new RemoteRepository.Builder(
                        s01Config.snapshotRepositoryId(), "default", s01Config.snapshotRepositoryUrl())
                .build();
        return new SonatypeNx2Publisher(repositorySystem, session, releasesRepository, snapshotsRepository);
    }
}
