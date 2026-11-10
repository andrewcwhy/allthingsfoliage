// All Things Foliage
import org.gradle.jvm.tasks.Jar

plugins {
    id("mod-platform")
    id("net.fabricmc.fabric-loom")
}

stonecutter {
    val loader = current.project.substringAfterLast('-')

    properties.tags(current.version, loader)
}

val minecraftVersion = stonecutter.current.version
val modId = prop("mod.id")
val modVersion = prop("mod.version")

val accessWidenerFile =
    rootProject.file("src/main/resources/aw/$minecraftVersion.accesswidener")

val generatedResourcesDirectory =
    rootProject.file("src/generated/$minecraftVersion")

platform {
    loader = "fabric"

    dependencies {
        required("fabric-api") {
            slug("fabric-api")
            fabricLikeVersionRange = ">=${prop("deps.fabric-api")}"
        }

        required("fabricloader") {
            fabricLikeVersionRange = ">=${prop("deps.fabric-loader")}"
        }

        required("minecraft") {
            fabricLikeVersionRange = prop("deps.minecraft")
        }

        required("sway") {
            slug("sway")
            fabricLikeVersionRange = "*"
        }

        optional("modmenu") {}
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")

    compileOnly("maven.modrinth:iris:${prop("deps.iris")}")
    compileOnly("com.terraformersmc:modmenu:${prop("deps.modmenu")}")
    compileOnly("maven.modrinth:sodium:${prop("deps.sodium")}")

    implementation("maven.modrinth:sway:${prop("deps.sway")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric-api")}")
    implementation("net.fabricmc:fabric-language-kotlin:${prop("deps.fabric-kotlin")}")
    implementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")

    localRuntime("maven.modrinth:lithium:${prop("deps.lithium")}")
    localRuntime("com.terraformersmc:modmenu:${prop("deps.modmenu")}")
    localRuntime("maven.modrinth:sodium:${prop("deps.sodium")}")
}

loom {
    accessWidenerPath = accessWidenerFile

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
            modFiles.from(generatedResourcesDirectory)
        }
    }

    runs {
        named("client") {
            client()

            configName = "Fabric Client"
            ideConfigGenerated(true)
            programArgs("--username=Dev")
            runDir = "run/"

            vmArgs(
                "-XX:StackShadowPages=32",
                "--add-exports",
                "java.base/jdk.internal.misc=ALL-UNNAMED",
            )
        }

        named("server") {
            server()

            configName = "Fabric Server"
            ideConfigGenerated(true)
            runDir = "run/"
        }
    }
}


repositories {
    mavenCentral()

    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"

        content {
            includeGroup("maven.modrinth")
        }
    }

    maven("https://maven.terraformersmc.com/") {
        name = "TerraformersMC"

        content {
            includeGroup("com.terraformersmc")
        }
    }
}

fabricApi {
    configureDataGeneration {
        // mod-platform adds generated resources to the runtime output.
        addToResources = false
        client = true
        modId = prop("mod.id")
        outputDirectory = generatedResourcesDirectory
    }
}

tasks.matching { task -> task.name == "javadoc" }.configureEach {
    dependsOn(tasks.named("runDatagen"))
}

tasks.named("build") {
    val modArchive = tasks.named<Jar>("jar").flatMap { it.archiveFile }

    doLast {
        val archive = modArchive.get().asFile
        val bytes = archive.length()

        logger.lifecycle(
            "${archive.name}: ${bytes / 1024} KiB ($bytes bytes)"
        )
    }
}
