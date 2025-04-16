/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher.basic;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.impl.ModelProvider;
import eu.maveniverse.maven.njord.shared.impl.publisher.ValidatorSupport;
import eu.maveniverse.maven.njord.shared.publisher.spi.ValidationContext;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

/**
 *  Verifies that any found POM name, description, project URL, SCM and license is filled in.
 */
public class PomProjectValidatorFactory extends ValidatorSupport {
    private final RepositorySystemSession session;
    private final List<RemoteRepository> remoteRepositories;
    private final ModelProvider modelProvider;

    public PomProjectValidatorFactory(
            String name,
            RepositorySystemSession session,
            List<RemoteRepository> repositories,
            ModelProvider modelProvider) {
        super(name);
        this.session = requireNonNull(session);
        this.remoteRepositories = requireNonNull(repositories);
        this.modelProvider = requireNonNull(modelProvider);
    }

    @Override
    public void validate(ArtifactStore artifactStore, Artifact artifact, ValidationContext collector)
            throws IOException {
        if (artifact.getClassifier().isEmpty() && "pom".equals(artifact.getExtension())) {
            ArrayList<RemoteRepository> remoteRepositories = new ArrayList<>(this.remoteRepositories);
            remoteRepositories.add(artifactStore.storeRemoteRepository());
            Optional<Model> mo = modelProvider.readEffectiveModel(session, artifact, remoteRepositories);
            if (mo.isPresent()) {
                Model m = mo.orElseThrow();
                if (m.getName() != null && !m.getName().trim().isEmpty()) {
                    collector.addInfo("POM project/name OK");
                } else {
                    collector.addError("POM project/name MISSING");
                }
                if (m.getDescription() != null && !m.getDescription().trim().isEmpty()) {
                    collector.addInfo("POM project/description OK");
                } else {
                    collector.addError("POM project/description MISSING");
                }
                if (m.getUrl() != null && !m.getUrl().trim().isEmpty()) {
                    collector.addInfo("POM project/url OK");
                } else {
                    collector.addError("POM project/url MISSING");
                }
                if (m.getLicenses().isEmpty()) {
                    collector.addError("POM project/licenses MISSING");
                } else {
                    boolean ok = true;
                    for (License license : m.getLicenses()) {
                        if ((license.getName() == null
                                        || license.getName().trim().isEmpty())
                                || (license.getUrl() == null
                                        || license.getUrl().trim().isEmpty())) {
                            ok = false;
                            collector.addError("POM project/licenses/license MISSING (incomplete)");
                        }
                    }
                    if (ok) {
                        collector.addInfo("POM project/licenses OK");
                    }
                }
                if (m.getDevelopers().isEmpty()) {
                    collector.addError("POM project/developers MISSING");
                } else {
                    boolean ok = true;
                    for (Developer developer : m.getDevelopers()) {
                        if ((developer.getName() == null
                                        || developer.getName().trim().isEmpty())
                                || (developer.getEmail() == null
                                        || developer.getEmail().trim().isEmpty())) {
                            ok = false;
                            collector.addError("POM project/developers/developer MISSING (incomplete)");
                        }
                    }
                    if (ok) {
                        collector.addInfo("POM project/licenses OK");
                    }
                }
                if (m.getScm() == null) {
                    collector.addError("POM project/scm MISSING");
                } else {
                    Scm scm = m.getScm();
                    if ((scm.getUrl() == null || scm.getUrl().trim().isEmpty())
                            || (scm.getConnection() == null
                                    || scm.getConnection().trim().isEmpty())
                            || (scm.getDeveloperConnection() == null
                                    || scm.getDeveloperConnection().trim().isEmpty())) {
                        collector.addError("POM project/scm MISSING (incomplete)");
                    }
                }
            } else {
                collector.addError("Could not get effective model");
            }
        }
    }
}
