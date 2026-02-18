/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl;

import static java.util.Objects.requireNonNull;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Njord specific transfer listener: it is silent (as logs to DEBUG) but user can still make output visible
 * by using debug (-X) of Maven. This listener shows message(s) only in some case of problem.
 * <p>
 * If debug logging is enabled, this class will emit visible (DEBUG) messages of successful transfers and warnings
 * about failures will contain full stack traces.
 */
public class NjordTransferListener extends AbstractTransferListener {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final boolean silent;

    public NjordTransferListener(boolean silent) {
        this.silent = silent;
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        String action = (event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded");
        String direction = event.getRequestType() == TransferEvent.RequestType.PUT ? "to" : "from";
        TransferResource resource = event.getResource();
        if (silent) {
            logger.debug(
                    "{} {} {}: {}{}",
                    action,
                    direction,
                    resource.getRepositoryId(),
                    resource.getRepositoryUrl(),
                    resource.getResourceName());
        } else {
            logger.info(
                    "{} {} {}: {}{}",
                    action,
                    direction,
                    resource.getRepositoryId(),
                    resource.getRepositoryUrl(),
                    resource.getResourceName());
        }
    }

    @Override
    public void transferFailed(TransferEvent event) {
        String action = (event.getRequestType() == TransferEvent.RequestType.PUT ? "upload" : "download");
        String direction = event.getRequestType() == TransferEvent.RequestType.PUT ? "to" : "from";
        TransferResource resource = event.getResource();
        // if we are called here without exception = bug in resolver/maven
        Exception exception = requireNonNull(event.getException());
        if (event.getRequestType() != TransferEvent.RequestType.PUT && exception instanceof ArtifactNotFoundException) {
            if (silent) {
                logger.debug(
                        "Failed {} {} {}: {}{}; not found",
                        action,
                        direction,
                        resource.getRepositoryId(),
                        resource.getRepositoryUrl(),
                        resource.getResourceName());
            } else {
                logger.info(
                        "Failed {} {} {}: {}{}; not found",
                        action,
                        direction,
                        resource.getRepositoryId(),
                        resource.getRepositoryUrl(),
                        resource.getResourceName());
            }
        } else {
            logger.warn(
                    "Failed {} {} {}: {}{}; {}",
                    action,
                    direction,
                    resource.getRepositoryId(),
                    resource.getRepositoryUrl(),
                    resource.getResourceName(),
                    exception.getMessage());
            if (logger.isDebugEnabled()) {
                logger.warn("Failure:", exception);
            }
        }
    }
}
