/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.publisher;

import eu.maveniverse.maven.njord.shared.store.RepositoryMode;
import java.util.Optional;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Component responsible for config, remote repo URL and remote repo Auth extraction, while it follows various kinds
 * of "redirections".
 * <p>
 * User can set up proper environment by editing own, user wide <code>settings.xml</code>. Ideally (and minimally), user
 * should have as many auth-equipped server entries as much real publishing services user uses (for Central that is
 * today 3 + 1: Sonatype OSS, Sonatype S01, Sonatype Central Portal and ASF). Out of these four 2 are being phased
 * out (OSS and S01). Aside of auth, these entries should also contain Njord configuration such as
 * {@link eu.maveniverse.maven.njord.shared.SessionConfig#CONFIG_PUBLISHER},
 * {@link eu.maveniverse.maven.njord.shared.SessionConfig#CONFIG_RELEASE_URL} and optionally
 * {@link eu.maveniverse.maven.njord.shared.SessionConfig#CONFIG_SNAPSHOT_URL} if snapshot staging is desired. Once
 * user has these entries in own <code>settings.xml</code>, publishing is set up.
 * <p>
 * Historically, each OSS project "invented" their own Server ID in the <code>project/distributionManagement</code>,
 * so today we have many server IDs in use <em>despite almost all of use one of these 4 services to publish</em>.
 * This lead to issues, when one user maintains several "namespaces" (several unrelated groupID publishing to Central),
 * as this user needs to copy-paste all these server entries with <em>same auth</em> to be able to publish. Moreover,
 * is just chaotic, as people "invented" various names for these services.
 * <p>
 * Take for example Sonatype Central Portal setup:
 * The recommended entry in user <code>settings.xml</code> is following (replace {@code $TOKEN1} and {@code $TOKEN2} with Central Portal generated tokens):
 * <pre>{@code
 *     <server>
 *       <id>sonatype-central-portal</id>
 *       <username>$TOKEN1</username>
 *       <password>$TOKEN2</password>
 *       <configuration>
 *         <njord.publisher>sonatype-cp</njord.publisher>
 *         <njord.releaseUrl>njord:template:release-sca</njord.releaseUrl>
 *       </configuration>
 *     </server>
 * }</pre>
 * <p>
 * The recommended POM distribution management entry for Central Portal w/ snapshot publishing enabled is this:
 * <pre>{@code
 *   <distributionManagement>
 *     <repository>
 *       <id>sonatype-central-portal</id>
 *       <name>Sonatype Central Portal</name>
 *       <url>https://repo.maven.apache.org/maven2/</url>
 *     </repository>
 *     <snapshotRepository>
 *       <id>sonatype-central-portal</id>
 *       <name>Sonatype Central Portal</name>
 *       <url>https://central.sonatype.com/repository/maven-snapshots/</url>
 *     </snapshotRepository>
 *   </distributionManagement>
 * }</pre>
 * This entry tells the "truth", in a way it tells exactly where it publishes, and from where the artifacts are available
 * once they are published. This latter is important, as before it was common to have some "service URL" here, that had nothing to
 * do with "published artifacts whereabouts", it was always implied. But, some tools like SBOM managers do like to have
 * this information, instead to guess them. Similarly for snapshot publishing, it is <em>same service</em>, hence
 * same auth is needed for it as well.
 * <p>
 * Moreover, consider if some vendor changes service endpoint (like goes V2 from V1). Having service endpoint in here
 * is not future-proof. Also, it does not express where the published artifacts go. Finally, all you do is "just publish
 * to Central", so why should POM differ from project that use service A from service B, while they both publish to
 * Central? Does it matter HOW you publish, while the WHERE you publish remain same?  Is silly.
 * <p>
 * Njord encourages this setup:
 * <ul>
 *     <li>have only as many properly identified auth-equipped entries in your <code>settings.xml</code> as many you really use (stop copy-paste, no duplicates)</li>
 *     <li>ideally, your POM should tell the "truth", and not use some invented server ID and service/proprietary URL</li>
 *     <li>if not possible, it allows you to create "redirects" in your <code>settings.xml</code> to overcome this chaos</li>
 * </ul>
 * <p>
 * In case you need to publish a project that does not follow these rules, and assume it uses some invented server IDs,
 * like "the-project-releases", but in reality it wants to use Central Portal, just add this entry to your user
 * <code>settings.xml</code>:
 * <pre>{@code
 *     <server>
 *       <id>the-project-releases</id>
 *       <configuration>
 *         <njord.redirectService>sonatype-central-portal</njord.redirectService>
 *       </configuration>
 *     </server>
 * }</pre>
 * And that is it: no copy pasta needed: if you cannot change the project distribution management, just reidrect it
 * to proper service.
 *
 * @see eu.maveniverse.maven.njord.shared.SessionConfig#CONFIG_SERVICE_REDIRECT
 * @see eu.maveniverse.maven.njord.shared.SessionConfig#CONFIG_AUTH_REDIRECT
 */
public interface ArtifactPublisherRedirector {
    /**
     * Tells, based on Njord config, what is the real URL used for given remote repository. Never returns {@code null},
     * and without any configuration just returns passed in repository URL.
     */
    String getRepositoryUrl(RemoteRepository repository);

    /**
     * Tells, based on Njord config, what is the real URL used for given remote repository. Never returns {@code null},
     * and without any configuration just returns passed in repository URL.
     */
    String getRepositoryUrl(RemoteRepository repository, RepositoryMode repositoryMode);

    /**
     * Returns the remote repository to source auth from for the passed in remote repository. Never returns {@code null}.
     */
    RemoteRepository getAuthRepositoryId(RemoteRepository repository, boolean followAuthRedirection);

    /**
     * Returns the remote repository to use for publishing. Never returns {@code null}. The repository will have
     * auth and any auth-redirection, if applicable, applied.
     *
     * @see #getAuthRepositoryId(RemoteRepository, boolean)
     */
    RemoteRepository getPublishingRepository(
            RemoteRepository repository, boolean expectAuth, boolean followAuthRedirection);

    /**
     * Returns the name of wanted/configured {@link eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher}.
     */
    Optional<String> getArtifactStorePublisherName();
}
