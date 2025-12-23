/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.plugin3;

import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.store.WriteMode;
import java.io.IOException;
import java.util.Locale;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Sets store write mode (by default to read-only).
 */
@Mojo(name = "set-write-mode", threadSafe = true, requiresProject = false, aggregator = true)
public class SetWriteModeMojo extends NjordMojoSupport {
    /**
     * The name of the store to set read-only.
     */
    @Parameter(required = true, property = SessionConfig.KEY_PREFIX + "store")
    private String store;

    /**
     * The write mode of the store to apply. Accepted values are:
     * <ul>
     *     <li>{@code r}, {@code ro}, {@code readonly}, {@code read-only}, {@code read_only} (case-insensitive for {@link WriteMode#READ_ONLY})</li>
     *     <li>{@code w1}, {@code wo}, {@code writeonce}, {@code write-once}, {@code write_once} (case-insensitive for {@link WriteMode#WRITE_ONCE})</li>
     *     <li>{@code w}, {@code wm}, {@code writemany}, {@code write-many}, {@code write_many} (case-insensitive for {@link WriteMode#WRITE_MANY})</li>
     * </ul>
     *
     * @see WriteMode
     */
    @Parameter(required = true, property = SessionConfig.KEY_PREFIX + "writeMode", defaultValue = "r")
    private String writeMode;

    @Override
    protected void doWithSession(Session ns) throws MojoFailureException, IOException {
        WriteMode wm = parseWriteMode();
        if (ns.artifactStoreManager().updateWriteModeArtifactStore(store, wm)) {
            logger.info("ArtifactStore {} write mode set to {}", store, wm);
        } else {
            logger.warn("ArtifactStore with given name not found");
        }
    }

    private WriteMode parseWriteMode() throws MojoFailureException {
        switch (writeMode.toLowerCase(Locale.ROOT)) {
            case "r":
            case "ro":
            case "readonly":
            case "read-only":
            case "read_only":
                return WriteMode.READ_ONLY;
            case "w1":
            case "wo":
            case "writeonce":
            case "write-once":
            case "write_once":
                return WriteMode.WRITE_ONCE;
            case "w":
            case "wm":
            case "writemany":
            case "write-many":
            case "write_many":
                return WriteMode.WRITE_MANY;
        }
        throw new MojoFailureException("Unknown value for 'writeMode' mojo parameter");
    }
}
