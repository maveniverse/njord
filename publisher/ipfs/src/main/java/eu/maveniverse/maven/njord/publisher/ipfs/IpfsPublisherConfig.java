/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.ipfs;

import eu.maveniverse.maven.njord.shared.SessionConfig;
import eu.maveniverse.maven.njord.shared.publisher.PublisherConfigSupport;
import org.eclipse.aether.util.ConfigUtils;

/**
 * IPFS publisher config.
 */
public final class IpfsPublisherConfig extends PublisherConfigSupport {
    private final String multiaddr;
    private final String prefix;
    private final boolean publish;
    private final String publishKeyName;
    private final boolean publishKeyCreate;

    public IpfsPublisherConfig(SessionConfig sessionConfig) {
        super(IpfsPublisherFactory.NAME, sessionConfig);

        this.multiaddr = ConfigUtils.getString(
                sessionConfig.effectiveProperties(), "/ip4/127.0.0.1/tcp/5001", keyNames("multiaddr"));
        this.prefix = ConfigUtils.getString(
                sessionConfig.effectiveProperties(), "/publish/eu.maveniverse/", keyNames("prefix"));
        this.publish = ConfigUtils.getBoolean(sessionConfig.effectiveProperties(), true, keyNames("publish"));
        this.publishKeyName =
                ConfigUtils.getString(sessionConfig.effectiveProperties(), "self", keyNames("publishKeyName"));
        this.publishKeyCreate =
                ConfigUtils.getBoolean(sessionConfig.effectiveProperties(), true, keyNames("publishKeyCreate"));
    }

    public String multiaddr() {
        return multiaddr;
    }

    public String prefix() {
        return prefix;
    }

    public boolean isPublish() {
        return publish;
    }

    public String publishKeyName() {
        return publishKeyName;
    }

    public boolean isPublishKeyCreate() {
        return publishKeyCreate;
    }
}
