# Njörðr

[![Maven Central](https://img.shields.io/maven-central/v/eu.maveniverse.maven.njord/extension.svg?label=Maven%20Central)](https://search.maven.org/artifact/eu.maveniverse.maven.njord/extension)

Requirements:
* Java 17+
* Maven 3.9+

Note: this code is Proof of Concept, with a lot of To-Be-Done parts and intentionally simple as possible. For now
only "central" repository released artifacts are supported.

Goal: A universal publishing and local staging extension.

Concept:
Where you may publish your artifacts may change from time to time, involving even some proprietary solutions and protocols.
If you are within some organization, you most probably use some Maven Repository Manager, and you use Maven as "usual"
with deploying artifacts into designated hosted repository. But, if you are in open (as in "open source"), then you are
mercy of some organizations providing services, but requiring some "special" (given there is no standard in this area)
steps to publish artifacts. This tool aims to help you manage your build artifacts by publishing them to repositories,
but not requiring from you to mutilate your own build (POMs) if it happens that you change HOW or WHERE you publish.

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

And just build with Maven...

Build requirements:
* Java 21
* Maven 3.9.9+

## High level design

TBD
