# Njörðr

[![Maven Central](https://img.shields.io/maven-central/v/eu.maveniverse.maven.njord/extension.svg?label=Maven%20Central)](https://search.maven.org/artifact/eu.maveniverse.maven.njord/extension)

Requirements:
* Java 17+
* Maven 3.9+

Note: this code is Proof of Concept, with a lot of To-Be-Done parts and intentionally simple as possible. For now
only "central" repository released artifacts are supported.

Goal: A universal publishing and repository management extension.

Concept:
* TBD

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
