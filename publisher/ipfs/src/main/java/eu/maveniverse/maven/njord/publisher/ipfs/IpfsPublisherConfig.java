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
import java.util.regex.Pattern;
import org.eclipse.aether.util.ConfigUtils;

/**
 * IPFS publisher config.
 */
public final class IpfsPublisherConfig extends PublisherConfigSupport {
    private static final Pattern NS_PATTERN = Pattern.compile("^[a-z]+[a-z0-9._\\-]*[a-z]+$");

    private final String multiaddr;
    private final String namespace;
    private final String filesPrefix;
    private final String namespacePrefix;
    private final boolean publishIPNS;
    private final String publishIPNSKeyName;
    private final boolean publishIPNSKeyCreate;

    public IpfsPublisherConfig(SessionConfig sessionConfig) {
        super(IpfsPublisherFactory.NAME, sessionConfig);

        this.multiaddr = ConfigUtils.getString(
                sessionConfig.effectiveProperties(), "/ip4/127.0.0.1/tcp/5001", keyNames("multiaddr"));
        String namespaceInput =
                ConfigUtils.getString(sessionConfig.effectiveProperties(), "self", keyNames("namespace"));
        if (!NS_PATTERN.matcher(namespaceInput).matches()) {
            throw new IllegalArgumentException("namespace must comply to regexp " + NS_PATTERN);
        }
        this.namespace = namespaceInput;
        String filesPrefixInput = ConfigUtils.getString(
                sessionConfig.effectiveProperties(), "publish/" + this.namespace, keyNames("filesPrefix"));
        if (!filesPrefixInput.endsWith("/")) {
            filesPrefixInput += "/";
        }
        this.filesPrefix = filesPrefixInput;
        String namespacePrefixInput =
                ConfigUtils.getString(sessionConfig.effectiveProperties(), "repository", keyNames("namespacePrefix"));
        if (!namespacePrefixInput.endsWith("/")) {
            namespacePrefixInput += "/";
        }
        this.namespacePrefix = namespacePrefixInput;

        this.publishIPNS = ConfigUtils.getBoolean(sessionConfig.effectiveProperties(), true, keyNames("publishIPNS"));
        this.publishIPNSKeyName = ConfigUtils.getString(
                sessionConfig.effectiveProperties(), this.namespace, keyNames("publishIPNSKeyName"));
        this.publishIPNSKeyCreate =
                ConfigUtils.getBoolean(sessionConfig.effectiveProperties(), true, keyNames("publishIPNSKeyCreate"));
    }

    public String multiaddr() {
        return multiaddr;
    }

    public String namespace() {
        return namespace;
    }

    public String filesPrefix() {
        return filesPrefix;
    }

    public String namespacePrefix() {
        return namespacePrefix;
    }

    public boolean isPublishIPNS() {
        return publishIPNS;
    }

    public String publishIPNSKeyName() {
        return publishIPNSKeyName;
    }

    public boolean isPublishIPNSKeyCreate() {
        return publishIPNSKeyCreate;
    }
}
