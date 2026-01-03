/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher.signature;

import eu.maveniverse.maven.njord.shared.publisher.spi.ValidationContext;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;

public class SigstoreSignatureValidator extends SignatureValidatorSupport {
    public SigstoreSignatureValidator() {
        super(new SigstoreSignatureType());
    }

    @Override
    public Outcome verifySignature(
            ArtifactStore artifactStore,
            Artifact artifact,
            Artifact signatureArtifact,
            IOSupplier<Optional<InputStream>> artifactContent,
            IOSupplier<Optional<InputStream>> signatureContent,
            ValidationContext collector)
            throws IOException {
        return Outcome.SKIPPED; // TODO
    }
}
