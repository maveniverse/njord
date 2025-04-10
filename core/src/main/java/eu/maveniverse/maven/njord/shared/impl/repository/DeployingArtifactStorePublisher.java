package eu.maveniverse.maven.njord.shared.impl.repository;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.repository.ArtifactStore;
import eu.maveniverse.maven.njord.shared.repository.ArtifactStorePublisher;
import java.io.IOException;
import java.util.stream.Collectors;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;

public class DeployingArtifactStorePublisher implements ArtifactStorePublisher {
    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession repositorySystemSession;
    private final RemoteRepository repository;

    public DeployingArtifactStorePublisher(
            RepositorySystem repositorySystem,
            RepositorySystemSession repositorySystemSession,
            RemoteRepository repository) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.repositorySystemSession = requireNonNull(repositorySystemSession);
        this.repository = requireNonNull(repository);
    }

    @Override
    public void publish(ArtifactStore artifactStore) throws IOException {
        DeployRequest deployRequest = new DeployRequest();
        deployRequest.setArtifacts(artifactStore.artifacts().stream()
                .map(a -> a.setVersion(a.getBaseVersion()))
                .collect(Collectors.toList()));
        deployRequest.setRepository(repository);
        deployRequest.setTrace(new RequestTrace(artifactStore));
        try {
            repositorySystem.deploy(repositorySystemSession, deployRequest);
        } catch (DeploymentException e) {
            throw new IOException(e);
        }
    }
}
