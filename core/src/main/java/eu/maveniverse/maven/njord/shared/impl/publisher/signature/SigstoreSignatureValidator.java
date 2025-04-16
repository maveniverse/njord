/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher.signature;

import java.io.IOException;
import java.io.InputStream;

public class SigstoreSignatureValidator extends SignatureValidatorSupport {
    public SigstoreSignatureValidator() {
        super(new SigstoreSignatureType());
    }

    @Override
    public boolean verifySignature(InputStream content, InputStream signature) throws IOException {
        return false;
    }
}
