# Njörðr

[![Maven Central](https://img.shields.io/maven-central/v/eu.maveniverse.maven.njord/extension.svg?label=Maven%20Central)](https://search.maven.org/artifact/eu.maveniverse.maven.njord/extension)

Documentation available: https://maveniverse.eu/docs/njord/

Requirements:
* Java 8+
* Maven 3.9+ (tested with 3.9.9 and 4.0.0-rc-3)

Note: this code is Proof of Concept, with a lot of To-Be-Done parts and intentionally simple as possible.

Goal: Provides non-intrusive Artifact publishing and local staging functionality.

Concept: Assume you have a Maven project and it works very well. Suddenly, the _publishing requirement_ change, and 
changes wildly... and then you are in trouble. But, if you think about it, **where** and **how** you publish your 
artifacts will inevitably change from time to time, maybe even involving some proprietary solutions and/or protocols.
If you are within some organization, you are probably using some Maven Repository Manager, and you use Maven as "usual":
deploying artifacts into designated hosted repository. But, if you are in open (as in "open source"), or you want to 
publish your artifacts globally (make them globally reachable), then you are at the mercy of organizations providing 
these services, usually requiring some "special" (given there is no standard in this area) steps to publish artifacts.
Moreover, these steps most often comes with some required changes to your project, like adding profiles, and who knows
what, plus you are forced to use their (possibly closed source) plugins in your own build to make it work.
This tool aim is to help you manage your build artifacts by publishing them to these repositories, but without the
hassle of requiring to mutilate your own build (POMs).

With Njord you can have benefits:
* have only as many server entries in your `settings.xml` as many publishing services you use, as opposed to current
  status, where each project uses "own" server ID for distribution management, that again causes that users working
  on several projects (ie releasing them) must have copies of auth for each server of each project. Currently, there
  are only 4 services publishing to Maven Central, so all you need is at most 4 server entries with auth in your `settings.xml`.
* support publishing that is not "natively" supported by Maven, without hoops and looks and any change needed in
  your project. Moreover, publishing comes with local staging as well in non-intrusive way.

## Setting it up

With Maven 3 install Njord project-wide `.mvn/extensions.xml`, or with Maven 4+ install it user-wide `~/.m2/extensions.xml` like this:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
    <extension>
        <groupId>eu.maveniverse.maven.njord</groupId>
        <artifactId>extension</artifactId>
        <version>${currentVersion}</version>
    </extension>
</extensions>
```

It is recommended (but not mandatory) to add this stanza to your `settings.xml` as well if you don't have it already 
(to not type whole G of plugin):
```xml
  <pluginGroups>
    <pluginGroup>eu.maveniverse.maven.plugins</pluginGroup>
  </pluginGroups>
```

Next, set up authentication for service or even more services you plan to use. Different publishers require different 
server, for example `sonatype-cp` publisher needs following stanza in your `settings.xml`:

```xml
    <server>
      <id>sonatype-cp</id>
      <username>USER_TOKEN_PT1</username>
      <password>USER_TOKEN_PT2</password>
    </server>
```

Supported publishers and corresponding `server.id`s are:

| Publisher (publisher ID)                                       | server.id               | What is needed                                                                                                                               |
|----------------------------------------------------------------|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| Sonatype Central Portal (`sonatype-cp`)                        | `sonatype-cp`           | Obtain tokens for publishing by following [this documentation](https://central.sonatype.org/publish/generate-portal-token/).                 |
| Sonatype OSS on https://oss.sonatype.org/ (`sonatype-oss`)     | `sonatype-oss`          | Obtain tokens for publishing by following [this documentation](https://central.sonatype.org/publish/generate-token/) and using OSS instance. |
| Sonatype S01 on https://s01.oss.sonatype.org/ (`sonatype-s01`) | `sonatype-s01`          | As above but using s01 instance.                                                                                                             |
| Apache RAO on https://repository.apache.org/ (`apache-rao`)    | `apache.releases.https` | As above but using RAO instance.                                                                                                             |

Make sure your `settings.xml` contains token associated with proper `server.id` corresponding to you publishing service you want to use.
The publisher id is determined (or inferred) from the plugin parameter `publisher` (of goal `publish`) or from user
or project properties, or from `server` indirection (in `settings.xml` you can add `configuration` element with Njord
configurations that will be obeyed to "redirect" to other server, useful when you does not want to or cannot change
the distributionManagement server IDs.

If the project POM cannot be changed (`project/distributionManagement/repository`) or you don't want to change it, 
you need one more thing: set up server indirection. Assuming your POM contains this:

```xml
  <distributionManagement>
    <repository>
      <id>project-releases</id>
      <name>Some Release Repository</name>
      <url>https://some.service/deploy</url>
    </repository>
    <snapshotRepository>
      <id>project-snapshots</id>
      <name>Some Snapshots Repository</name>
      <url>https://some.service/snapshots</url>
    </snapshotRepository>
  </distributionManagement>
```

Then you need to set up indirection for servers `project-releases` and `project-snapshots` in your `settings.xml`, like this:

```xml
    <server>
      <id>project-releases</id>
      <configuration>
        <!-- Using Sonatype Central Portal publisher -->
        <njord.publisher>sonatype-cp</njord.publisher>
        <!-- Releases are staged locally (if omitted, would go directly to URL as per POM) -->
        <njord.releaseUrl>njord:template:release-sca</njord.releaseUrl>
      </configuration>
    </server>
    <server>
      <id>project-snapshots</id>
      <configuration>
         <!-- Using Sonatype Central Portal publisher -->
         <njord.publisher>sonatype-cp</njord.publisher>
         <!-- Snapshots are staged locally (if omitted, would go directly to URL as per POM) -->
         <njord.snapshotUrl>njord:template:snapshot-sca</njord.snapshotUrl>
      </configuration>
    </server>
```

This provides Njord following information:
* indirection of publishing `project-release` -> `sonatype-cp` and `project-snapshots` -> `sonatype-cp` (for snapshot support Central Portal must have snapshot support enabled)
* templates for local staging of releases and snapshots
* any indirection may be omitted, then Maven will use distribution repositories from POM. Same for URL, if omitted, URL from POM will be used.

That's all! No project change needed at all. 

## Using it

Next, let's see an example of Apache Maven project (I used `maven-gpg-plugin`):

1. For example’s sake, I took last release of plugin (hence am simulating release deploy): `git checkout maven-gpg-plugin-3.2.7`
2. Deploy it (locally stage): `mvn -P apache-release deploy -DaltDeploymentRepository=id::njord:` (The `id` is really unused, is there just to fulfil deploy plugin syntax requirement. The URL `njord:` will use "default" store template that is RELEASE. You can target other templates by using, and is equivalent of this `njord:release`. You can stage locally snapshots as well with URL `njord:snapshot`. Finally, you can target existing store with `njord:store:storename-xxx`).
3. Check staged store names: `mvn njord:list`
4. Optionally, check locally staged content: `mvn njord:list-content -Dstore=release-xxx` (use store name from above)
5. Optionally, validate locally staged content: `mvn njord:validate -Ddetails -Dstore=release-xxx` (use store name from above)
6. Publish it to ASF: `mvn njord:publish -Dstore=release-xxx -Dpublisher=apache-rao` (use store name from above), if operation successful, store is dropped.
7. From now on, the repository is staged on RAO, so you can close it, vote, and all the usual fluff as before.
8. To drop locally staged stores use: `mvn njord:drop -Dstore=release-xxx` (use store name from above)

Build requirements:
* Java 21
* Maven 3.9.9+

## High level design

TBD
