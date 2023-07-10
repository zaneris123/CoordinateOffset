import de.undercouch.gradle.tasks.download.Download

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.palantir.git-version") version "3.0.0"
    id("de.undercouch.download") version "5.4.0"
}

val gitVersion: groovy.lang.Closure<String> by extra

group = "com.jtprince"
version = gitVersion()

val pluginYmlApiVersion: String by project
val spigotApiVersion: String by project
val javaVersion: JavaLanguageVersion = JavaLanguageVersion.of(17)

val localDependencyDir = buildDir.resolve("dependencies")

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.codemc.org/repository/maven-public/") // CommandAPI
    maven("https://hub.jeff-media.com/nexus/repository/jeff-media-public/") // MorePDTs

    // maven("https://repo.dmulloy2.net/repository/public/")  // ProtocolLib
    flatDir {
        dir(localDependencyDir)
    }
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.0")
    compileOnly("org.spigotmc:spigot-api:$spigotApiVersion")
    compileOnly(files(localDependencyDir.resolve("ProtocolLib.jar")))
    implementation("dev.jorel:commandapi-bukkit-shade:9.0.3")
    implementation("com.jeff_media:MorePersistentDataTypes:2.4.0")
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
        placeholders.forEach { (k, v) -> inputs.property(k, v) } // ensure cache is invalidated after version bumps
        filesMatching("plugin.yml") {
            expand(placeholders)
        }
    }

    shadowJar {
        minimize()
        relocate("dev.jorel.commandapi", "$group.lib.commandapi")
        relocate("com.jeff_media.morepersistentdatatypes", "$group.lib.morepersistentdatatypes")
    }

    val copyJarToSnapshot = register<Copy>("copyJarToSnapshot") {
        dependsOn(jar)
        dependsOn(shadowJar)
        from(shadowJar)
        into("build")
        rename { "${rootProject.name}-SNAPSHOT.jar" }
    }

    assemble {
        dependsOn(shadowJar)
        dependsOn(copyJarToSnapshot)
    }

    val downloadProtocolLibDevBuild = register<Download>("downloadProtocolLibDevBuild") {
        /*
         * ProtocolLib is still in dev-builds for 1.20. Since they're not published on Maven yet, download the latest
         * JAR directly from Jenkins.
         */
        doFirst { localDependencyDir.mkdirs() }
        src(arrayOf("https://ci.dmulloy2.net/job/ProtocolLib/lastSuccessfulBuild/artifact/build/libs/ProtocolLib.jar"))
        dest(localDependencyDir.resolve("ProtocolLib.jar"))
        overwrite(false)
    }

    compileJava {
        dependsOn(downloadProtocolLibDevBuild)
    }
}
