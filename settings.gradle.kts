pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    // Same multi-version + cross-obfuscation toolchain as Anvil, so CSW builds against Anvil's
    // Mojmap jars on both versions.
    id("dev.kikugie.stonecutter") version "0.9.6"
    id("dev.kikugie.loom-back-compat") version "0.3"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        versions("1.21.11", "26.1")
        // Current SkyBlock runs on 1.21.11, so that's the working baseline.
        vcsVersion = "1.21.11"
    }
}

// Composite build: CSW consumes Anvil straight from the sibling project — no publish step. Gradle
// auto-substitutes a dependency on `Anvil:<node>` with Anvil's matching Stonecutter subproject.
includeBuild("../anvil")

rootProject.name = "CantStopWinning"
