/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.sonatype;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.impl.ModelProvider;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirementsFactory;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;

@Singleton
@Named(SonatypeCentralRequirementsFactory.NAME)
public class SonatypeCentralRequirementsFactory implements ArtifactStoreRequirementsFactory {
    public static final String NAME = "central";

    private final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;
    private final ModelProvider modelProvider;

    @Inject
    public SonatypeCentralRequirementsFactory(
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector, ModelProvider modelProvider) {
        this.checksumAlgorithmFactorySelector = requireNonNull(checksumAlgorithmFactorySelector);
        this.modelProvider = requireNonNull(modelProvider);
    }

    @Override
    public ArtifactStoreRequirements create(Session session) {
        return new SonatypeCentralRequirements(session, checksumAlgorithmFactorySelector, modelProvider);
    }
}
