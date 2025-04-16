package eu.maveniverse.maven.njord.shared.impl;

import java.util.List;
import java.util.Optional;
import org.apache.maven.model.Model;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

public interface ModelProvider {
    Optional<Model> readEffectiveModel(
            RepositorySystemSession session, Artifact artifact, List<RemoteRepository> remoteRepositories);
}
