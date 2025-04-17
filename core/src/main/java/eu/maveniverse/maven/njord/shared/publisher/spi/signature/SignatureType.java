/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher.spi.signature;

public interface SignatureType {
    /**
     * Signature algorithm name, like "GPG".
     */
    String name();

    /**
     * Description.
     */
    String description();

    /**
     * Signature extension without leading dot, like "asc".
     */
    String extension();
}
