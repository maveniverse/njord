/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.jreleaser;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherSupport;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStoreRequirements;
import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.jreleaser.model.api.JReleaserCommand;
import org.jreleaser.model.internal.JReleaserContext;
import org.jreleaser.workflow.Workflow;
import org.jreleaser.workflow.Workflows;

public class JReleaserPublisher extends ArtifactStorePublisherSupport {
    private final JReleaserPublisherConfig config;

    public JReleaserPublisher(
            Session session,
            RepositorySystem repositorySystem,
            JReleaserPublisherConfig config,
            ArtifactStoreRequirements artifactStoreRequirements) {
        super(
                session,
                repositorySystem,
                JReleaserPublisherFactory.NAME,
                "Invokes using JReleaser over given ArtifactStore",
                config.targetReleaseRepository(),
                config.targetSnapshotRepository(),
                config.targetReleaseRepository(),
                config.targetSnapshotRepository(),
                artifactStoreRequirements);
        this.config = requireNonNull(config);
    }

    @Override
    protected void doPublish(ArtifactStore artifactStore) throws IOException {
        RemoteRepository repository = selectServiceRemoteRepositoryFor(artifactStore);
        if (session.config().dryRun()) {
            logger.info(
                    "Dry run; not publishing '{}' to '{}' service at {}",
                    artifactStore.name(),
                    name,
                    repository.getUrl());
            return;
        }

        // write it out for JReleaser; remove leftovers of previous runs
        if (Files.exists(config.outputDirectory())) {
            FileUtils.deleteRecursively(config.outputDirectory());
        }
        session.artifactStoreWriter().writeAsDirectory(artifactStore, config.outputDirectory());

        JReleaserContext context = config.createContext();
        try {
            workflow(config.command(), context).execute();
        } finally {
            context.getLogger().close();
        }
    }

    private static Workflow workflow(JReleaserCommand command, JReleaserContext context) {
        switch (command) {
            case DOWNLOAD:
                return Workflows.download(context);
            case ASSEMBLE:
                return Workflows.assemble(context);
            case CHANGELOG:
                return Workflows.changelog(context);
            case CHECKSUM:
                return Workflows.checksum(context);
            case CATALOG:
                return Workflows.catalog(context);
            case SIGN:
                return Workflows.sign(context);
            case DEPLOY:
                return Workflows.deploy(context);
            case UPLOAD:
                return Workflows.upload(context);
            case RELEASE:
                return Workflows.release(context);
            case PREPARE:
                return Workflows.prepare(context);
            case PACKAGE:
                return Workflows.packageRelease(context);
            case PUBLISH:
                return Workflows.publish(context);
            case ANNOUNCE:
                return Workflows.announce(context);
            case FULL_RELEASE:
                return Workflows.fullRelease(context);
            default:
                throw new IllegalArgumentException("Unsupported JReleaser command: " + command);
        }
    }
}
