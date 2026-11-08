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
    accessWidenerPath =
        rootProject.file(
            "src/main/resources/aw/${stonecutter.current.version}.accesswidener"
        )

    runs {
        named("client") {
            client()
            ideConfigGenerated(true)

            runDir = "run/"
            configName = "Fabric Client"

            programArgs("--username=Dev")

            vmArgs(
                "-XX:StackShadowPages=32",
                "--add-exports",
                "java.base/jdk.internal.misc=ALL-UNNAMED",
            )
        }

        mods {
            register(prop("mod.id")) {
                sourceSet(sourceSets["main"])

                modFiles.from(
                    rootProject.file("src/generated/${stonecutter.current.version}")
                )
            }
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
        outputDirectory =
            rootProject.file("src/generated/${stonecutter.current.version}")

        // The common build plugin adds generated data to the runtime output.
        addToResources = false
        client = true
        modId = prop("mod.id")
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

    implementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric-api")}")
    implementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
    implementation("maven.modrinth:sway:${prop("deps.sway")}")

    val modMenuVersion = prop("deps.modmenu")

    compileOnly("com.terraformersmc:modmenu:$modMenuVersion")
    compileOnly("maven.modrinth:iris:${prop("deps.iris")}")
    compileOnly("maven.modrinth:sodium:${prop("deps.sodium")}")

    localRuntime("com.terraformersmc:modmenu:$modMenuVersion")
}

tasks.matching { it.name == "javadoc" }.configureEach {
    dependsOn("runDatagen")
}
