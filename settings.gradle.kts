pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()

        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }

        maven("https://maven.neoforged.net/releases/") {
            name = "NeoForged"
        }

        maven("https://maven.kikugie.dev/snapshots") {
            name = "KikuGie Snapshots"
        }

        maven("https://maven.kikugie.dev/releases") {
            name = "KikuGie Releases"
        }

        maven("https://maven.parchmentmc.org") {
            name = "ParchmentMC"
        }
    }

    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.2"
}

stonecutter {
	create(rootProject) {
	    fun registerVersion(minecraftVersion: String, vararg loaders: String) {
            loaders.forEach { loader ->
                version("$minecraftVersion-$loader", minecraftVersion)
                    .buildscript = "build.$loader.gradle.kts"
            }
        }

		registerVersion("26.3", "fabric", "neoforge")

		vcsVersion = "26.3-fabric"
	}
}
