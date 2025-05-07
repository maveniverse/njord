/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl.comparator;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.njord.shared.store.ArtifactStoreComparator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comparator support.
 */
public abstract class ArtifactStoreComparatorSupport implements ArtifactStoreComparator {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final SessionConfig sessionConfig;
    protected final String name;
    protected final String description;

    protected ArtifactStoreComparatorSupport(SessionConfig sessionConfig, String name, String description) {
        this.sessionConfig = requireNonNull(sessionConfig);
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

    @Override
    public ComparisonResult compare(ArtifactStore a1, ArtifactStore a2) throws IOException {
        requireNonNull(a1);
        requireNonNull(a2);

        CR cr = new CR(description());

        ComparisonContext template = cr.child("Template comparison");
        if (a1.template().equals(a2.template())) {
            template.addEquality("Stores used same template: " + a1.template().name());
        } else {
            template.addDifference("Stores used different template: "
                    + a1.template().name() + " vs " + a2.template().name());
        }
        ComparisonContext index = cr.child("Store index comparison");
        List<Artifact> a1Artifacts = extractIndex(a1);
        List<Artifact> a2Artifacts = extractIndex(a2);
        if (a1Artifacts.equals(a2Artifacts)) {
            index.addEquality(
                    "Stores contains same artifacts: " + a1.artifacts().size());
            doCompare(cr, a1, a2);
        } else {
            index.addDifference("Storage contains different artifacts: "
                    + a1.artifacts().size() + " vs " + a2.artifacts().size());
        }
        return cr;
    }

    /**
     * Index "extraction": it "dries" out artifacts and sorts them. Basically, they are only coordinates.
     */
    protected List<Artifact> extractIndex(ArtifactStore artifactStore) {
        return artifactStore.artifacts().stream()
                .map(INDEX_COMPARATOR_MAPPER)
                .sorted(INDEX_COMPARATOR)
                .collect(Collectors.toList());
    }

    protected abstract void doCompare(ComparisonContext comparisonContext, ArtifactStore a1, ArtifactStore a2)
            throws IOException;

    protected static final Function<Artifact, Artifact> INDEX_COMPARATOR_MAPPER =
            a -> a.setFile(null).setProperties(null);

    protected static final Comparator<Artifact> INDEX_COMPARATOR = Comparator.comparing(Artifact::getGroupId)
            .thenComparing(Artifact::getArtifactId)
            .thenComparing(Artifact::getVersion)
            .thenComparing(Artifact::getClassifier)
            .thenComparing(Artifact::getExtension);

    private static final class CR implements ComparisonResult, ComparisonContext {
        private final String name;
        private final ArrayList<String> equalities = new ArrayList<>();
        private final ArrayList<String> differences = new ArrayList<>();
        private final LinkedHashMap<String, ArtifactStoreComparatorSupport.CR> children = new LinkedHashMap<>();

        private CR(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        private void dropIfEmpty(ArtifactStoreComparatorSupport.CR child) {
            if (child.equalities.isEmpty() && child.differences.isEmpty() && child.children.isEmpty()) {
                children.remove(child.name());
            }
        }

        @Override
        public Collection<String> equalities() {
            return List.copyOf(equalities);
        }

        @Override
        public Collection<String> differences() {
            return List.copyOf(differences);
        }

        @Override
        public Collection<ComparisonResult> children() {
            return List.copyOf(children.values());
        }

        @Override
        public ArtifactStoreComparatorSupport.CR addEquality(String msg) {
            equalities.add(msg);
            return this;
        }

        @Override
        public ArtifactStoreComparatorSupport.CR addDifference(String msg) {
            differences.add(msg);
            return this;
        }

        @Override
        public ArtifactStoreComparatorSupport.CR child(String name) {
            requireNonNull(name);
            ArtifactStoreComparatorSupport.CR child = new ArtifactStoreComparatorSupport.CR(name);
            children.put(name, child);
            return child;
        }
    }
}
