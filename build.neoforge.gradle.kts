plugins {
    id("mod-platform")
    id("net.neoforged.moddev")
}

stonecutter {
    val (minecraftVersion, loader) = current.project.split('-', limit = 2)
    properties.tags(minecraftVersion, loader)
}

val fabricDatagenTaskPath =
    ":${stonecutter.current.version}-fabric:runDatagen"

val localRuntimeConfiguration = configurations.maybeCreate("localRuntime")

configurations.named("runtimeClasspath") {
    extendsFrom(localRuntimeConfiguration)
}

platform {
    loader = "neoforge"

    dependencies {
        required("minecraft") {
            forgeLikeVersionRange = "[${prop("deps.minecraft")}]"
        }

        required("neoforge") {
            forgeLikeVersionRange.set("[${prop("deps.neoforge")},)")
        }

        // Install Sway's model wrapper before applying foliage compatibility.
        required("sway") {
            slug("sway")
            forgeLikeVersionRange.set("[1,)")
            ordering.set("AFTER")
        }
    }
}

repositories {
    mavenCentral()

    strictMaven("https://api.modrinth.com/maven", "maven.modrinth") {
        name = "Modrinth"
    }
}

dependencies {
    compileOnly("maven.modrinth:iris:${prop("deps.iris")}")
    compileOnly("maven.modrinth:sodium:${prop("deps.sodium")}")

    implementation("maven.modrinth:sway:${prop("deps.sway")}")

    add("localRuntime", "maven.modrinth:lithium:${prop("deps.lithium")}")
    add("localRuntime", "maven.modrinth:sodium:${prop("deps.sodium")}")
}

neoForge {
    version = prop("deps.neoforge")

    accessTransformers.from(
        rootProject.file("src/main/resources/aw/${stonecutter.current.version}.cfg")
    )
    validateAccessTransformers = true

    mods {
        register(prop("mod.id")) {
            sourceSet(sourceSets["main"])
        }
    }

    runs {
        register("client") {
            client()

            gameDirectory = file("run/")
            ideName = "NeoForge Client (${stonecutter.current.version})"
            programArgument("--username=Dev")

            // Iris uses fewer vertex bindings than the development validator expects.
            systemProperty("neoforge.disableBlaze3DValidation", "true")
            systemProperty("neoforge.disableGlValidation", "true")

            jvmArguments.addAll(
                "--add-exports",
                "java.base/jdk.internal.misc=ALL-UNNAMED",
            )
        }

        register("server") {
            server()

            gameDirectory = file("run/")
            ideName = "NeoForge Server (${stonecutter.current.version})"
        }
    }
}

// Generated data is supplied by the common build plugin.
sourceSets["main"].java {
    exclude("**/platform/fabric/**")
}

tasks.named("createMinecraftArtifacts") {
    dependsOn(tasks.named("stonecutterGenerate"))
}

tasks.matching { it.name == "javadoc" }.configureEach {
    dependsOn(fabricDatagenTaskPath)
}
