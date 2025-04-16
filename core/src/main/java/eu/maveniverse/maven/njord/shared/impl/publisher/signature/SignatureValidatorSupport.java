/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher.signature;

import eu.maveniverse.maven.njord.shared.impl.CloseableSupport;
import eu.maveniverse.maven.njord.shared.publisher.spi.signature.SignatureType;
import eu.maveniverse.maven.njord.shared.publisher.spi.signature.SignatureValidator;

public abstract class SignatureValidatorSupport extends CloseableSupport implements SignatureValidator {
    private final SignatureType type;

    protected SignatureValidatorSupport(SignatureType type) {
        this.type = type;
    }

    @Override
    public SignatureType type() {
        return type;
    }
}
