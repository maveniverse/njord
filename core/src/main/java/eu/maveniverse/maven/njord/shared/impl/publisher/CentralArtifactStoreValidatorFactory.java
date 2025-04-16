/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreValidator;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreValidatorFactory;
import eu.maveniverse.maven.njord.shared.publisher.spi.ValidatorFactory;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;

@Singleton
@Named(CentralArtifactStoreValidatorFactory.NAME)
public class CentralArtifactStoreValidatorFactory implements ArtifactStoreValidatorFactory {
    public static final String NAME = "central";

    private final List<ValidatorFactory> validatorFactories;

    @Inject
    public CentralArtifactStoreValidatorFactory(List<ValidatorFactory> validatorFactories) {
        this.validatorFactories = requireNonNull(validatorFactories);
    }

    @Override
    public ArtifactStoreValidator create(RepositorySystemSession session, Config config) {
        return new DefaultArtifactStoreValidator(NAME, "Central Validator", session, config, validatorFactories);
    }
}
