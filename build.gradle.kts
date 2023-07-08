buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    java
    id("com.palantir.git-version") version("3.0.0")
}

val gitVersion: groovy.lang.Closure<String> by extra

group = "com.jtprince"
version = gitVersion()

val pluginYmlApiVersion: String by project
val spigotApiVersion: String by project
val javaVersion: JavaLanguageVersion = JavaLanguageVersion.of(17)

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:$spigotApiVersion")
}

java {
    toolchain {
        sourceCompatibility = JavaVersion.toVersion(javaVersion)
        targetCompatibility = JavaVersion.toVersion(javaVersion)
        languageVersion.set(javaVersion)
    }
}

tasks {
    processResources {
        val placeholders = mapOf(
            "version" to version,
            "apiVersion" to pluginYmlApiVersion,
        )
        filesMatching("plugin.yml") {
            expand(placeholders)
        }
    }
}
