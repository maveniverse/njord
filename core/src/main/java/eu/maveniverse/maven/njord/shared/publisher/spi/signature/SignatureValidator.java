/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher.spi.signature;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public interface SignatureValidator extends Closeable {
    /**
     * The type this validator validates.
     */
    SignatureType type();

    /**
     * Verifies received content against received signature. May perform much more, like fetching key and so on.
     * If it returns {@code true}, then and only then is signature accepted as "verified".
     */
    boolean verifySignature(InputStream content, InputStream signature) throws IOException;
}
