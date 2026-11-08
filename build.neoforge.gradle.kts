plugins {
    id("mod-platform")
    id("net.neoforged.moddev")
}

stonecutter {
    val (minecraftVersion, loader) = current.project.split('-', limit = 2)
    properties.tags(minecraftVersion, loader)
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

neoForge {
    version = prop("deps.neoforge")

    accessTransformers.from(
        rootProject.file("src/main/resources/aw/${stonecutter.current.version}.cfg")
    )
    validateAccessTransformers = true

    runs {
        register("client") {
            client()

            gameDirectory = file("run/")
            ideName = "NeoForge Client (${stonecutter.current.version})"

            programArgument("--username=Dev")

            // Iris uses fewer vertex bindings than the development validator expects.
            systemProperty("neoforge.disableGlValidation", "true")
            systemProperty("neoforge.disableBlaze3DValidation", "true")

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

    mods {
        register(prop("mod.id")) {
            sourceSet(sourceSets["main"])
        }
    }
}

// Generated data is supplied by the common build plugin.
sourceSets["main"].java {
    exclude("**/platform/fabric/**")
}

repositories {
    mavenCentral()

    strictMaven("https://api.modrinth.com/maven", "maven.modrinth") {
        name = "Modrinth"
    }
}

dependencies {
    implementation("maven.modrinth:sway:${prop("deps.sway")}")

    compileOnly("maven.modrinth:iris:${prop("deps.iris")}")
    compileOnly("maven.modrinth:sodium:${prop("deps.sodium")}")
}

tasks.named("createMinecraftArtifacts") {
    dependsOn(tasks.named("stonecutterGenerate"))
}

val fabricDatagenTaskPath =
    ":${stonecutter.current.version}-fabric:runDatagen"

tasks.matching { it.name == "javadoc" }.configureEach {
    dependsOn(fabricDatagenTaskPath)
}
