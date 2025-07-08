/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl;

import static java.util.Objects.requireNonNull;

import org.eclipse.aether.repository.RemoteRepository;

/**
 * Utilities to handle Resolver.
 */
public final class ResolverUtils {
    private ResolverUtils() {}

    /**
     * Parses "alt remote repository string" and returns {@link RemoteRepository}. The string must not be
     * {@code null}. If string invalid, {@link IllegalArgumentException} is thrown.
     * <p>
     * The returned repository has not set any policy, this method just sets {@code id} and {@code url}. So to say,
     * this method return "raw" or "bare" repositories.
     */
    public static RemoteRepository parseRemoteRepositoryString(String string) {
        requireNonNull(string);
        String[] split = string.split("::");
        String id;
        String layout = "default";
        String url;
        if (split.length == 2) {
            id = split[0];
            url = split[1];
        } else if (split.length == 3) {
            id = split[0];
            layout = split[1];
            url = split[2];
        } else {
            throw new IllegalArgumentException(
                    "Invalid alt deployment repository syntax (supported is id::url or legacy id::layout::url): "
                            + string);
        }
        return new RemoteRepository.Builder(id, layout, url).build();
    }
}
