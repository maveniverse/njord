# Njörðr

[![Maven Central](https://img.shields.io/maven-central/v/eu.maveniverse.maven.njord/extension.svg?label=Maven%20Central)](https://search.maven.org/artifact/eu.maveniverse.maven.njord/extension)

Requirements:
* Java 17+
* Maven 3.9+

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

## To use it

With Maven 3 create project-wide, or with Maven 4-rc-3+ create user-wide `~/.m2/extensions.xml` like this:
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

That's all! No project change needed at all. Next, let's see an example of Apache Maven project (I used `maven-gpg-plugin`):

1. For example’s sake, I took last release of plugin: `git checkout maven-gpg-plugin-3.2.7`
2. Deploy it (locally stage): `mvn -P apache-release deploy -DaltDeploymentRepository=id::njord:` (The `id` is really unused, is there just to fulfil deploy plugin syntax requirement. The URL `njord:` will use "default" store template that is RELEASE. You can target other templates by using, and is equivalent of this `njord:release`. You can stage locally snapshots as well with URL `njord:snapshot`. Finally, you can target existing store with `njord:store:storename-xxx`).
3. Check staged store names: `mvn njord:list`
4. Check locally staged content: `mvn njord:list-content -Dstore=release-xxx` (use store name from above)
5. Publish it to `repository.apache.org`: `mvn njord:publish -Dstore=release-xxx -Dtarget=apache-rao` (use store name from above)
6. From now on, the repository is staged on RAO, so you can close it, vote, and all the usual fluff

Build requirements:
* Java 21
* Maven 3.9.9+

## High level design

TBD
