@file:OptIn(dev.kikugie.stonecutter.StonecutterExperimentalAPI::class)

plugins {
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.dotenv)
    alias(libs.plugins.fabric.loom) apply false
    alias(libs.plugins.neoforged.moddev) apply false
    alias(libs.plugins.jsonlang.postprocess) apply false
    alias(libs.plugins.mod.publish.plugin) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.devtools.ksp) apply false
    alias(libs.plugins.fletching.table) apply false
}

stonecutter active "26.3-fabric"

tasks.register("runActiveClient") {
    group = "stonecutter"
    description = "Run the client for the active Stonecutter target"
    dependsOn("${stonecutter.current!!.project}:runClient")
}

tasks.register("runActiveServer") {
    group = "stonecutter"
    description = "Run the server for the active Stonecutter target"
    dependsOn("${stonecutter.current!!.project}:runServer")
}

stonecutter parameters
    {
        constants.match(
            current.project.substringAfterLast('-'),
            "fabric",
            "neoforge",
        )

        swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
        swaps["mod_id"] = "\"${properties.get<String>("mod.id")}\";"
        swaps["mod_name"] = "\"${properties.get<String>("mod.name")}\";"
        swaps["mod_group"] = "\"${properties.get<String>("mod.group")}\";"
        swaps["minecraft"] = "\"${current.version}\";"

        constants["release"] = properties.get<String>("mod.id") != "modtemplate"
        constants["iris"] = true
    }

for (minecraftVersion in stonecutter.versions.map { it.version }.distinct()) {
    tasks.register("publish$minecraftVersion") {
        group = "publishing"
        description = "Publish configured loaders for Minecraft $minecraftVersion"
        dependsOn(
            stonecutter.tasks.named("publishMods") {
                metadata.version == minecraftVersion
            }
        )
    }
}
