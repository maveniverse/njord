package eu.maveniverse.maven.njord.shared.impl;

import static java.util.Objects.requireNonNull;

public abstract class CloseableConfigSupport<C> extends CloseableSupport {
    protected final C config;

    protected CloseableConfigSupport(C config) {
        this.config = requireNonNull(config);
    }
}
