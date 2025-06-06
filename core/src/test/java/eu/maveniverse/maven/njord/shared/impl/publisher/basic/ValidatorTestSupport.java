/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.publisher.basic;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.impl.publisher.PublisherTestSupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreValidator;
import eu.maveniverse.maven.njord.shared.publisher.spi.ValidationContext;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

public class ValidatorTestSupport extends PublisherTestSupport {
    protected final Path withClasses = Paths.get("src/test/binaries/validators/withClasses.jar");
    protected final Path withoutClasses = Paths.get("src/test/binaries/validators/withoutClasses.jar");
    protected final Path withSources = Paths.get("src/test/binaries/validators/withSources.jar");
    protected final Path withoutSources = Paths.get("src/test/binaries/validators/withoutSources.jar");

    public static class TestValidationContext implements ArtifactStoreValidator.ValidationResult, ValidationContext {
        private final String name;
        private final ArrayList<String> info = new ArrayList<>();
        private final ArrayList<String> warnings = new ArrayList<>();
        private final ArrayList<String> errors = new ArrayList<>();
        private final LinkedHashMap<String, TestValidationContext> children = new LinkedHashMap<>();

        public TestValidationContext(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Collection<String> info() {
            return J8Utils.copyOf(info);
        }

        @Override
        public Collection<String> warning() {
            return J8Utils.copyOf(warnings);
        }

        @Override
        public Collection<String> error() {
            return J8Utils.copyOf(errors);
        }

        @Override
        public Collection<ArtifactStoreValidator.ValidationResult> children() {
            return J8Utils.copyOf(children.values());
        }

        @Override
        public TestValidationContext addInfo(String msg) {
            info.add(msg);
            return this;
        }

        @Override
        public TestValidationContext addWarning(String msg) {
            warnings.add(msg);
            return this;
        }

        @Override
        public TestValidationContext addError(String msg) {
            errors.add(msg);
            return this;
        }

        @Override
        public TestValidationContext child(String name) {
            requireNonNull(name);
            TestValidationContext child = new TestValidationContext(name);
            children.put(name, child);
            return child;
        }
    }
}
