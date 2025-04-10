/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DirectoryLocker {
    public static DirectoryLocker INSTANCE = new DirectoryLocker();

    private final HashMap<Path, FileChannel> fileChannels = new HashMap<>();
    private final HashMap<Path, FileLock> fileLocks = new HashMap<>();
    private final HashMap<Path, ArrayDeque<Boolean>> references = new HashMap<>();

    public synchronized void lockDirectory(Path directory, boolean exclusiveAccess) throws IOException {
        requireNonNull(directory, "directory");

        Path lockFile = directory.resolve(".lock");
        if (!Files.isRegularFile(lockFile)) {
            try {
                Files.createFile(lockFile);
            } catch (FileAlreadyExistsException e) {
                // ignore
            }
        }
        if (exclusiveAccess && fileChannels.containsKey(directory)) {
            throw new IOException("Failed to gain exclusive access to storage: " + directory);
        } else {
            try {
                AtomicBoolean acted = new AtomicBoolean(false);
                FileChannel channel = fileChannels.computeIfAbsent(directory, p -> {
                    try {
                        FileChannel c = FileChannel.open(lockFile, StandardOpenOption.READ, StandardOpenOption.WRITE);
                        acted.set(true);
                        return c;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                FileLock lock = fileLocks.computeIfAbsent(directory, p -> {
                    try {
                        return channel.tryLock(0, Long.MAX_VALUE, !exclusiveAccess);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                if (lock.isShared() == exclusiveAccess) {
                    throw new IOException("Failed to gain " + (exclusiveAccess ? "exclusive" : "shared")
                            + " access to storage: " + directory);
                }
                references.computeIfAbsent(directory, p -> new ArrayDeque<>()).push(acted.get());
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }
    }

    public synchronized void unlockDirectory(Path directory) throws IOException {
        requireNonNull(directory, "directory");
        ArrayDeque<Boolean> refs = references.get(directory);
        if (refs == null) {
            throw new IOException("Directory was not locked: " + directory);
        }
        refs.pop();
        if (refs.isEmpty()) {
            try {
                fileLocks.remove(directory).close();
            } catch (IOException e) {
                // ignore
            }
            try {
                fileChannels.remove(directory).close();
            } catch (IOException e) {
                // ignore
            }
            references.remove(directory);
        }
    }
}
