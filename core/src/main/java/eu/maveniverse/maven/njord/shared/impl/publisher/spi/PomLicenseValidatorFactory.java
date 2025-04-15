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
import org.eclipse.aether.RepositorySystemSession;

/**
 * Verifies that any found POM license is filled in.
 */
public class PomLicenseValidatorFactory implements ValidatorFactory {
    @Override
    public Validator create(RepositorySystemSession session, Config config) {
        return null;
    }
}
