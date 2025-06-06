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
 * so today we have many server IDs in use <em>despite almost all of use one of these 3 services to publish</em>.
 * (the Sonatype ones, so 3 and not 4, as ASF projects have all this sorted out by using ASF wide Parent POM).
 * This lead to issues, when one user maintains several "namespaces" (several unrelated groupID publishing to Central),
 * as this user needs to copy-paste all these server entries with <em>same auth</em> to be able to publish.
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
 * This entry tells the "truth", in a way it tells exactly where it publishes (URL), and it is from where the artifacts are available
 * once they are published. This latter is important, as before it was common to have some "service URL" here, that had nothing to
 * do with "published artifacts whereabouts", it was always implied. But, some tools like do like to have
 * this information, they should not guess this. Similarly for snapshot publishing, it is <em>same service</em>, hence
 * same auth is needed for it as well.
 * <p>
 * Moreover, consider if some vendor changes service endpoint (like goes Ver2 from Ver1). Having service endpoint in here
 * is not future-proof and is just confusing: your publishing target (Central) does not change but some technicality requires
 * you to change you parent POM. Using service endpoint does not express where the published artifacts will go.
 * Finally, all you do is <em>publish to Central</em>, so why should POM differ between two projects, possibly using
 * different services? Does it matter HOW you publish, while the WHERE you publish remain same? Is just silly.
 * <p>
 * Njord encourages this setup:
 * <ul>
 *     <li>have only as many properly identified auth-equipped entries in your <code>settings.xml</code> as many you really use (stop copy-paste, no duplicates)</li>
 *     <li>ideally, your POM should tell the "truth", and not use some invented server ID and service/proprietary URL</li>
 *     <li>if not possible, it allows you to create "redirects" in your <code>settings.xml</code> to overcome this chaos</li>
 * </ul>
 * <p>
 * In case you need to publish a project that does not follow these rules, and assuming it uses own invented server ID
 * "the-project-releases", but it migrated to use Central Portal, just add this entry to your user
 * <code>settings.xml</code>:
 * <pre>{@code
 *     <server>
 *       <id>the-project-releases</id>
 *       <configuration>
 *         <njord.serviceRedirect>sonatype-central-portal</njord.serviceRedirect>
 *       </configuration>
 *     </server>
 * }</pre>
 * And that is it, no copy pasta needed: if you cannot change the project distribution management, just redirect it
 * to proper service entry in your own <code>settings.xml</code>.
 *
 * @see eu.maveniverse.maven.njord.shared.SessionConfig#CONFIG_SERVICE_REDIRECT
 * @see eu.maveniverse.maven.njord.shared.SessionConfig#CONFIG_AUTH_REDIRECT
 */
public interface ArtifactPublisherRedirector {
    /**
     * Tells, based on server config, what is the Njord URL to be used for given remote repository. Never returns {@code null},
     * and without any configuration just returns passed in repository URL.
     *
     * @param repository The remote repository we ask config for, never {@code null}.
     */
    String getRepositoryUrl(RemoteRepository repository);

    /**
     * Tells, based on server config, what is the Njord URL to be used for given remote repository. Never returns {@code null},
     * and without any configuration just returns passed in repository URL.
     *
     * @param repository The remote repository we ask config for, never {@code null}.
     * @param repositoryMode The repository mode we ask URL for, never {@code null}.
     */
    String getRepositoryUrl(RemoteRepository repository, RepositoryMode repositoryMode);

    /**
     * Returns the remote repository to source auth from for the passed in remote repository. Never returns {@code null}.
     * The repository will have auth applied, if applicable.
     *
     * @param repository The remote repository we ask Auth for, never {@code null}.
     */
    RemoteRepository getAuthRepositoryId(RemoteRepository repository);

    /**
     * Returns the remote repository to use for publishing. Never returns {@code null}. The repository will have
     * auth applied, if applicable. The auth may come from elsewhere, see {@link #getAuthRepositoryId(RemoteRepository)}.
     *
     * @param repository The remote repository we ask Auth for, never {@code null}.
     * @param expectAuth Whether a warning should be logged if no auth found (is most probably configuration error).
     */
    RemoteRepository getPublishingRepository(RemoteRepository repository, boolean expectAuth);

    /**
     * Returns the name of wanted/configured {@link eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher}.
     * This method returns present string optional ONLY if string found in configuration is name of existing publisher.
     * If there was a string discovered, but string is not a name of existing publisher, this method throws. Otherwise,
     * empty optional is returned. The decision is made based on
     * {@link eu.maveniverse.maven.njord.shared.SessionConfig#CONFIG_PUBLISHER} property sourced from properties
     * (system, user or project), and finally, if project present, the distribution management of it.
     *
     * @throws IllegalStateException If string was found (given or found in config) but name does not correspond to
     *         known publisher.
     */
    Optional<String> getArtifactStorePublisherName();

    /**
     * Returns the name of wanted/configured {@link eu.maveniverse.maven.njord.shared.publisher.ArtifactStorePublisher}
     * based on passed in name. This method returns present string optional ONLY if passed in name is name of existing
     * publisher, or, is a server ID that has publisher configured. If there was a string discovered, but string is
     * not a name of existing publisher, this method throws.
     *
     * @param name A "name" that may be {@code null}. If not null, it is observed as "user input" and will be used
     *             as basis for looking up publisher name. Value may be a publisher name, or a settings server ID that has
     *             Njord config with {@link eu.maveniverse.maven.njord.shared.SessionConfig#CONFIG_PUBLISHER}. If
     *             name is {@code null}, call is passed to {@link #getArtifactStorePublisherName()} method.
     * @throws IllegalStateException If string was found (given or found in config) but name does not correspond to
     *         known publisher.
     */
    Optional<String> getArtifactStorePublisherName(String name);
}
