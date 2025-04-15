/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl;

import static java.util.Objects.requireNonNull;

public abstract class CloseableConfigSupport<C> extends CloseableSupport {
    protected final C config;

    protected CloseableConfigSupport(C config) {
        this.config = requireNonNull(config);
    }
}
