/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.store;

/**
 * Write mode: read only, write once (no updates/redeploys), write many (may update/redeploey).
 */
public enum WriteMode {
    READ_ONLY(false, false),
    WRITE_ONCE(true, false),
    WRITE_MANY(true, true);

    private final boolean allowWrite;
    private final boolean allowUpdate;

    WriteMode(boolean allowWrite, boolean allowUpdate) {
        this.allowWrite = allowWrite;
        this.allowUpdate = allowUpdate;
    }

    public boolean allowWrite() {
        return allowWrite;
    }

    public boolean allowUpdate() {
        return allowUpdate;
    }
}
