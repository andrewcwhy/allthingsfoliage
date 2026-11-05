plugins {
    id("mod-platform")
    id("net.fabricmc.fabric-loom")
}

stonecutter {
    val (minecraftVersion, loader) = current.project.split('-', limit = 2)
    properties.tags(minecraftVersion, loader)
}

platform {
    loader = "fabric"

    dependencies {
        required("minecraft") {
            fabricLikeVersionRange = prop("deps.minecraft")
        }

        required("fabric-api") {
            slug("fabric-api")
            fabricLikeVersionRange = ">=${prop("deps.fabric-api")}"
        }

        required("fabricloader") {
            fabricLikeVersionRange = ">=${prop("deps.fabric-loader")}"
        }

        required("sway") {
            slug("sway")
            fabricLikeVersionRange = "*"
        }

        optional("modmenu") {}
    }
}

loom {
    accessWidenerPath = rootProject.file(
        "src/main/resources/aw/${stonecutter.current.version}.accesswidener"
    )

    runs {
        named("client") {
            client()
            ideConfigGenerated(true)

            runDir = "run/"
            configName = "Fabric Client"

            programArgs("--username=Dev")

            // Preserve the existing Minecraft 26.3 development launch workaround.
            vmArgs(
                "-XX:StackShadowPages=32",
                "--add-exports",
                "java.base/jdk.internal.misc=ALL-UNNAMED",
            )
        }

        named("server") {
            server()
            ideConfigGenerated(true)

            runDir = "run/"
            configName = "Fabric Server"
        }
    }
}

fabricApi {
    configureDataGeneration {
        outputDirectory = rootProject.file(
            "versions/datagen/${stonecutter.current.version}/src/main/generated"
        )
        client = true
    }
}

repositories {
    mavenCentral()

    strictMaven("https://maven.terraformersmc.com/", "com.terraformersmc") {
        name = "TerraformersMC"
    }

    strictMaven("https://api.modrinth.com/maven", "maven.modrinth") {
        name = "Modrinth"
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")

    implementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric-api")}")
    implementation("maven.modrinth:sway:${prop("deps.sway")}")

    // Compile against Mod Menu and load it during development without bundling it.
    val modMenuVersion = prop("deps.modmenu")
    compileOnly("com.terraformersmc:modmenu:$modMenuVersion")
    localRuntime("com.terraformersmc:modmenu:$modMenuVersion")

    // Compile against optional shader integrations without bundling them.
    if (hasProperty("deps.iris")) {
        compileOnly("maven.modrinth:iris:${prop("deps.iris")}")
    }

    compileOnly("maven.modrinth:sodium:${prop("deps.sodium")}")
}
