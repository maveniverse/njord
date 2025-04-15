/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CloseableSupport implements Closeable {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final AtomicBoolean closed;

    protected CloseableSupport() {
        this.closed = new AtomicBoolean(false);
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            doClose();
        }
    }

    protected void doClose() throws IOException {
        // nothing; override if needed
    }

    protected void checkClosed() {
        if (closed.get()) {
            throw new IllegalStateException(getClass().getSimpleName() + " is closed");
        }
    }
}
