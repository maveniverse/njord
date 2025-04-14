package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.NjordUtils;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManager;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreManagerFactory;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NjordMojoSupport extends AbstractMojo {
    protected final Logger logger = LoggerFactory.getLogger(NjordMojoSupport.class);

    @Inject
    protected MavenSession mavenSession;

    @Inject
    protected RepositorySystem repositorySystem;

    @Inject
    protected ArtifactStoreManagerFactory artifactStoreManagerFactory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        RepositorySystemSession session = mavenSession.getRepositorySession();
        NjordUtils.lazyInitConfig(
                session,
                Config.defaults()
                        .userProperties(session.getUserProperties())
                        .systemProperties(session.getSystemProperties())
                        .build(),
                artifactStoreManagerFactory::create);
        Optional<ArtifactStoreManager> artifactStoreManager = NjordUtils.mayGetArtifactStoreManager(session);
        if (artifactStoreManager.isEmpty()) {
            logger.warn("Not configured or explicitly disabled");
            return;
        }
        try (ArtifactStoreManager storeManager = artifactStoreManager.orElseThrow()) {
            doExecute(storeManager);
        } catch (IOException e) {
            throw new MojoFailureException(e);
        }
    }

    protected abstract void doExecute(ArtifactStoreManager artifactStoreManager)
            throws IOException, MojoExecutionException, MojoFailureException;
}
