/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher.signature;

import eu.maveniverse.maven.njord.shared.impl.CloseableSupport;
import eu.maveniverse.maven.njord.shared.publisher.spi.signature.SignatureValidator;
import java.io.IOException;
import java.io.InputStream;

public class GpgSignatureValidator extends CloseableSupport implements SignatureValidator {
    public static final String NAME = "GPG";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "GPG detached and armored signature";
    }

    @Override
    public String extension() {
        return "asc";
    }

    @Override
    public boolean verifySignature(InputStream content, InputStream signature) throws IOException {
        return true;
    }
}
