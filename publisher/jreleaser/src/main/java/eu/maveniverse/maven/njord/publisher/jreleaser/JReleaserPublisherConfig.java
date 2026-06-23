/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.jreleaser;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.impl.J8Utils;
import eu.maveniverse.maven.njord.shared.publisher.PublisherConfigSupport;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.eclipse.aether.util.ConfigUtils;
import org.jreleaser.config.JReleaserConfigLoader;
import org.jreleaser.engine.context.ContextCreator;
import org.jreleaser.logging.JReleaserLogger;
import org.jreleaser.model.JReleaserException;
import org.jreleaser.model.JReleaserVersion;
import org.jreleaser.model.api.JReleaserCommand;
import org.jreleaser.model.internal.JReleaserContext;
import org.jreleaser.model.internal.JReleaserModel;
import org.jreleaser.util.PlatformUtils;
import org.slf4j.Logger;

/**
 * JReleaser publisher config. Right now it forces to have JReleaser config file in basedir.
 */
public final class JReleaserPublisherConfig extends PublisherConfigSupport {
    private final Logger logger;

    private final Path configFile;
    private final boolean yolo;
    private final boolean dryRun;
    private final boolean gitRootSearch;
    private final boolean strict;
    private final boolean reproducible;

    private final JReleaserCommand command;
    private final Path outputDirectory;

    public JReleaserPublisherConfig(SessionConfig sessionConfig, Logger logger) {
        super(JReleaserPublisherFactory.NAME, sessionConfig);
        this.logger = requireNonNull(logger);
        this.configFile = FileUtils.discoverUserCurrentWorkingDirectory().resolve("jreleaser.yml");

        this.yolo = ConfigUtils.getBoolean(sessionConfig.effectiveProperties(), false, keyNames("yolo"));
        this.dryRun = ConfigUtils.getBoolean(sessionConfig.effectiveProperties(), false, keyNames("dryRun"));
        this.gitRootSearch =
                ConfigUtils.getBoolean(sessionConfig.effectiveProperties(), false, keyNames("gitRootSearch"));
        this.strict = ConfigUtils.getBoolean(sessionConfig.effectiveProperties(), false, keyNames("strict"));
        this.reproducible =
                ConfigUtils.getBoolean(sessionConfig.effectiveProperties(), false, keyNames("reproducible"));
        this.command = JReleaserCommand.valueOf(ConfigUtils.getString(
                sessionConfig.effectiveProperties(), JReleaserCommand.FULL_RELEASE.name(), keyNames("command")));

        if (sessionConfig.currentProject().isPresent()) {
            this.outputDirectory = sessionConfig
                    .currentProject()
                    .orElseThrow(J8Utils.OET)
                    .buildDirectory()
                    .resolve("staging-deploy");
        } else {
            this.outputDirectory = FileUtils.discoverUserCurrentWorkingDirectory()
                    .resolve("target")
                    .resolve("staging-deploy");
        }
    }

    public Path outputDirectory() {
        return outputDirectory;
    }

    public JReleaserContext createContext() throws IOException {
        if (!Files.isRegularFile(configFile)) {
            throw new JReleaserException("Missing JReleaser configuration: " + configFile);
        }

        SessionConfig.CurrentProject currentProject =
                sessionConfig.currentProject().orElse(null);
        Path outputDirectory;
        if (currentProject != null) {
            outputDirectory = currentProject.buildDirectory();
        } else {
            outputDirectory = sessionConfig.basedir().resolve("target");
        }

        JReleaserLogger logger = getLogger(outputDirectory);
        PlatformUtils.resolveCurrentPlatform(logger);
        Path basedir = sessionConfig.basedir();

        logger.info("JReleaser {}", JReleaserVersion.getPlainVersion());
        logger.info("Configuring with {}", configFile);
        logger.increaseIndent();
        logger.info("- basedir set to {}", basedir);
        logger.info("- outputdir set to {}", outputDirectory);
        logger.decreaseIndent();

        JReleaserModel model = JReleaserConfigLoader.loadConfig(configFile);

        if (currentProject != null) {
            model.getProject()
                    .getLanguages()
                    .getJava()
                    .setGroupId(currentProject.artifact().getGroupId());
            model.getProject()
                    .getLanguages()
                    .getJava()
                    .setArtifactId(currentProject.artifact().getArtifactId());
            model.getProject()
                    .getLanguages()
                    .getJava()
                    .setVersion(currentProject.artifact().getVersion());
            model.getProject().setVersion(currentProject.artifact().getVersion());
        }

        return ContextCreator.create(
                getLogger(outputDirectory),
                JReleaserContext.Configurer.CLI_YAML,
                org.jreleaser.model.api.JReleaserContext.Mode.FULL,
                command,
                model,
                FileUtils.discoverUserCurrentWorkingDirectory(),
                null,
                outputDirectory,
                yolo,
                dryRun,
                gitRootSearch,
                strict,
                reproducible,
                Collections.emptyList(),
                Collections.emptyList());
    }

    private JReleaserLoggerAdapter getLogger(Path outputDirectory) throws IOException {
        java.nio.file.Files.createDirectories(outputDirectory);
        return new JReleaserLoggerAdapter(
                new PrintWriter(
                        new BufferedWriter(new OutputStreamWriter(
                                Files.newOutputStream(outputDirectory.resolve("trace.log")), StandardCharsets.UTF_8)),
                        true),
                logger);
    }
}
