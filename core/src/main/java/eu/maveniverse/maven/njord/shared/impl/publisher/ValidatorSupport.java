/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.impl.CloseableSupport;
import eu.maveniverse.maven.njord.shared.publisher.spi.Validator;

/**
 * Verifies checksum for every artifact.
 */
public abstract class ValidatorSupport extends CloseableSupport implements Validator {
    private final String name;
    private final String description;

    public ValidatorSupport(String name, String description) {
        this.name = requireNonNull(name);
        this.description = requireNonNull(description);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }
}
