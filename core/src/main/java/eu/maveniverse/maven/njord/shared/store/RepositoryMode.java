/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.store;

import static java.util.Objects.requireNonNull;

import org.eclipse.aether.repository.RemoteRepository;

public enum RepositoryMode {
    RELEASE,
    SNAPSHOT;

    public static RepositoryMode fromString(String mode) {
        requireNonNull(mode);
        if (mode.equalsIgnoreCase("SNAPSHOT")) {
            return SNAPSHOT;
        } else if (mode.equalsIgnoreCase("RELEASE")) {
            return RELEASE;
        } else {
            throw new IllegalArgumentException("Unknown repository mode: " + mode);
        }
    }

    public static RepositoryMode fromRemoteRepository(RemoteRepository remoteRepository) {
        requireNonNull(remoteRepository);
        boolean release = remoteRepository.getPolicy(false).isEnabled();
        boolean snapshot = remoteRepository.getPolicy(true).isEnabled();
        if (release && snapshot) {
            throw new IllegalArgumentException("Mixed mode not supported; repository is mixed: " + remoteRepository);
        }
        if (release) {
            return RELEASE;
        } else if (snapshot) {
            return SNAPSHOT;
        } else {
            throw new IllegalArgumentException("Unknown repository mode: " + remoteRepository);
        }
    }
}
