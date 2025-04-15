/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher.spi;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.publisher.spi.Validator;
import eu.maveniverse.maven.njord.shared.publisher.spi.ValidatorFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.artifact.SubArtifact;

/**
 * Verifies presence of source and javadoc for every main artifact.
 */
@Singleton
@Named(SourceAndJavadocValidatorFactory.NAME)
public class SourceAndJavadocValidatorFactory extends ValidatorSupport implements ValidatorFactory {
    public static final String NAME = "source-javadoc";

    public SourceAndJavadocValidatorFactory() {
        super(NAME, "Source and Javadoc presence");
    }

    @Override
    public Validator create(RepositorySystemSession session, Config config) {
        return this;
    }

    @Override
    public void validate(ArtifactStore artifactStore, ValidationResultCollector collector) throws IOException {
        for (Artifact artifact : artifactStore.artifacts()) {
            if (artifact.getClassifier().isEmpty() && "jar".equals(artifact.getExtension())) {
                ValidationResultCollector chkCollector = collector.child(ArtifactIdUtils.toId(artifact));
                HashSet<String> ok = new HashSet<>();
                HashSet<String> missing = new HashSet<>();
                Optional<InputStream> c = artifactStore.artifactContent(new SubArtifact(artifact, "sources", "jar"));
                if (c.isPresent()) {
                    ok.add("sources");
                    c.orElseThrow().close();
                } else {
                    missing.add("sources");
                }
                c = artifactStore.artifactContent(new SubArtifact(artifact, "javadoc", "jar"));
                if (c.isPresent()) {
                    ok.add("javadoc");
                    c.orElseThrow().close();
                } else {
                    missing.add("javadoc");
                }
                if (!ok.isEmpty()) {
                    chkCollector.addInfo("OK: " + String.join(", ", ok));
                }
                if (!missing.isEmpty()) {
                    chkCollector.addError("MISSING: " + String.join(", ", missing));
                }
            }
        }
    }
}
