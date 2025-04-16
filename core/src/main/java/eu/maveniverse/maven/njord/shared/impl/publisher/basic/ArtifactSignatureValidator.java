/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher.basic;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Config;
import eu.maveniverse.maven.njord.shared.publisher.spi.Validator;
import eu.maveniverse.maven.njord.shared.publisher.spi.ValidatorFactory;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.artifact.SubArtifact;

/**
 * Pluggable signature validator.
 */
@Singleton
@Named(ArtifactSignatureValidator.NAME)
public class ArtifactSignatureValidator extends ValidatorSupport implements ValidatorFactory {
    public static final String NAME = "artifact-signature";

    public interface SignatureValidator extends Closeable {
        String algorithm();

        String extension();

        void verifySignature(InputStream content, InputStream signature, ValidationResultCollector collector)
                throws IOException;
    }

    private final List<SignatureValidator> signatureValidators;

    @Inject
    protected ArtifactSignatureValidator(List<SignatureValidator> signatureValidators) {
        super(NAME, "Artifact Signature Validator");
        this.signatureValidators = requireNonNull(signatureValidators);
    }

    @Override
    public Validator create(RepositorySystemSession session, Config config) {
        return this;
    }

    @Override
    public void validate(ArtifactStore artifactStore, ValidationResultCollector collector) throws IOException {
        for (Artifact artifact : artifactStore.artifacts()) {
            if (artifactStore.omitChecksumsForExtensions().stream()
                    .noneMatch(e -> artifact.getExtension().endsWith(e))) {
                ValidationResultCollector chkCollector = collector.child(ArtifactIdUtils.toId(artifact));
                for (SignatureValidator signatureValidator : signatureValidators) {
                    Artifact signature =
                            new SubArtifact(artifact, "*", artifact.getExtension() + signatureValidator.extension());
                    Optional<InputStream> so = artifactStore.artifactContent(signature);
                    if (so.isPresent()) {
                        final InputStream signatureContent;
                        try (InputStream in = so.orElseThrow()) {
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            in.transferTo(bos);
                            signatureContent = new ByteArrayInputStream(bos.toByteArray());
                        }
                        signatureValidator.verifySignature(
                                artifactStore.artifactContent(artifact).orElseThrow(), signatureContent, collector);
                    } else {
                        chkCollector.addError("Missing " + signatureValidator.algorithm() + " signature");
                    }
                }
            }
        }
    }

    @Override
    protected void doClose() throws IOException {
        ArrayList<IOException> exceptions = new ArrayList<>();
        try {
            for (SignatureValidator signatureValidator : signatureValidators) {
                signatureValidator.close();
            }
        } catch (IOException e) {
            exceptions.add(e);
        }
        if (!exceptions.isEmpty()) {
            IOException e = new IOException("Failed to close validators");
            exceptions.forEach(e::addSuppressed);
            throw e;
        }
    }
}
