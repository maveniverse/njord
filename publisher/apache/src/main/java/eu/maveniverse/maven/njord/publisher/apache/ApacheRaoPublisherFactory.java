/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.apache;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.njord.publisher.sonatype.central.SonatypeCentralRequirementsFactory;
import eu.maveniverse.maven.njord.publisher.sonatype.nx2.SonatypeNx2Publisher;
import eu.maveniverse.maven.njord.shared.Session;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher;
import eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisherFactorySupport;
import eu.maveniverse.maven.njord.shared.publisher.MavenCentralPublisherFactory;
import java.util.Collections;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;

@Singleton
@Named(ApacheRaoPublisherFactory.NAME)
public class ApacheRaoPublisherFactory extends ArtifactStorePublisherFactorySupport
        implements MavenCentralPublisherFactory {
    public static final String NAME = "apache-rao";

    private final SonatypeCentralRequirementsFactory centralRequirementsFactory;

    @Inject
    public ApacheRaoPublisherFactory(
            RepositorySystem repositorySystem, SonatypeCentralRequirementsFactory centralRequirementsFactory) {
        super(
                repositorySystem,
                Collections.singletonMap(SonatypeCentralRequirementsFactory.NAME, centralRequirementsFactory));
        this.centralRequirementsFactory = requireNonNull(centralRequirementsFactory);
    }

    @Override
    protected ArtifactStorePublisher doCreate(Session session) {
        ApacheRaoPublisherConfig config = new ApacheRaoPublisherConfig(session.config());
        return new SonatypeNx2Publisher(
                session,
                repositorySystem,
                NAME,
                "Publishes to ASF RAO",
                CENTRAL,
                config.targetSnapshotRepository(),
                config.serviceReleaseRepository(),
                config.serviceSnapshotRepository(),
                centralRequirementsFactory.create(session),
                config.listenerMode());
    }
}
