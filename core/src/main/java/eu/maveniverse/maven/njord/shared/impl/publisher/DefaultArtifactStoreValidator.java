/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreValidator;
import eu.maveniverse.maven.njord.shared.publisher.spi.Validator;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultArtifactStoreValidator implements ArtifactStoreValidator {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final String name;
    private final String description;
    private final Collection<Validator> validators;

    public DefaultArtifactStoreValidator(String name, String description, Collection<Validator> validators) {
        this.name = requireNonNull(name);
        this.description = requireNonNull(description);
        this.validators = requireNonNull(validators);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public ValidationResult validate(ArtifactStore artifactStore) throws IOException {
        VR vr = new VR(description());
        for (Validator validator : validators) {
            validator.validate(artifactStore, vr.child(validator.description()));
        }
        return vr;
    }

    private static final class VR implements ValidationResult, Validator.ValidationResultCollector {
        private final String name;
        private final CopyOnWriteArrayList<String> info = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<String> warnings = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<String> errors = new CopyOnWriteArrayList<>();
        private final ConcurrentHashMap<String, VR> children = new ConcurrentHashMap<>();

        private VR(String name) {
            this.name = requireNonNull(name);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Collection<String> info() {
            return List.copyOf(info);
        }

        @Override
        public Collection<String> warning() {
            return List.copyOf(warnings);
        }

        @Override
        public Collection<String> error() {
            return List.copyOf(errors);
        }

        @Override
        public Collection<ValidationResult> children() {
            return List.copyOf(children.values());
        }

        @Override
        public Validator.ValidationResultCollector addInfo(String msg) {
            info.add(msg);
            return this;
        }

        @Override
        public Validator.ValidationResultCollector addWarning(String msg) {
            warnings.add(msg);
            return this;
        }

        @Override
        public Validator.ValidationResultCollector addError(String msg) {
            errors.add(msg);
            return this;
        }

        @Override
        public Validator.ValidationResultCollector child(String name) {
            VR child = new VR(name);
            children.put(name, child);
            return child;
        }
    }
}
