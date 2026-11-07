@file:Suppress("unused")

import java.util.Locale
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.peanuuutz.tomlkt.Toml
import org.gradle.api.NamedDomainObjectContainer

private val JSON = Json {
    prettyPrint = true
    encodeDefaults = true
}

private val TOML = Toml {}

sealed class Loader(val id: String) {
    abstract val jarTask: String
    abstract val sourcesJarTask: String
    abstract val modManifestPath: String
    abstract val excludedResources: List<String>

    open val isFabricLike: Boolean = false

    abstract fun generateManifest(ctx: Context): String

    sealed class FabricLike(id: String) : Loader(id) {
        override val isFabricLike = true
        override val excludedResources =
            listOf(
                "META-INF/mods.toml",
                "META-INF/neoforge.mods.toml",
                "aw/*.cfg",
                ".cache",
                "pack.mcmeta",
            )

        override fun generateManifest(ctx: Context): String {
            val manifest =
                FabricManifest(
                    id = ctx.modId,
                    name = ctx.modName,
                    version = ctx.baseVersion,
                    authors = ctx.authors,
                    contributors = ctx.contributors,
                    contact =
                        mapOf(
                            "sources" to ctx.sourcesUrl,
                            "issues" to ctx.issuesUrl,
                            "homepage" to ctx.homepageUrl,
                        ),
                    custom =
                        buildJsonObject {
                            if (ctx.discordUrl.isNotBlank()) {
                                putJsonObject("modmenu") {
                                    putJsonObject("links") {
                                        put("modmenu.discord", ctx.discordUrl)
                                    }
                                }
                            }
                        },
                    description = ctx.description,
                    icon = "assets/${ctx.modId}/icon.png",
                    license = ctx.licenseName,
                    accessWidener = "aw/${ctx.currentMcVersion}.accesswidener",
                    entrypoints =
                        mapOf(
                            "main" to
                                listOf(
                                    "${ctx.modGroup}.${ctx.modId}.platform.fabric.FabricEntrypoint"
                                ),
                            "client" to
                                listOf(
                                    "${ctx.modGroup}.${ctx.modId}.platform.fabric.FabricClientEntrypoint"
                                ),
                            "fabric-datagen" to
                                listOf(
                                    "${ctx.modGroup}.${ctx.modId}.platform.fabric.datagen.FabricDataGeneratorEntrypoint"
                                ),
                            "modmenu" to
                                listOf(
                                    "${ctx.modGroup}.${ctx.modId}.platform.fabric.FabricModMenuIntegration"
                                ),
                        ),
                    mixins =
                        buildList {
                            add("${ctx.modId}.gpu.mixins.json")
                            if (ctx.shaderPackSupport) {
                                add("${ctx.modId}.iris.mixins.json")
                            }
                            add("${ctx.modId}.render.mixins.json")
                            add("${ctx.modId}.targeting.mixins.json")
                        },
                    depends =
                        ctx.extension.dependencies.required.associate {
                            it.modid.get() to it.fabricLikeVersionRange.get()
                        },
                    recommends =
                        ctx.extension.dependencies.optional.associate {
                            it.modid.get() to it.fabricLikeVersionRange.get()
                        },
                    breaks =
                        ctx.extension.dependencies.incompatible.associate {
                            it.modid.get() to it.fabricLikeVersionRange.get()
                        },
                )
            return JSON.encodeToString(manifest)
        }
    }

    object Fabric : FabricLike("fabric") {
        override val jarTask = "jar"
        override val sourcesJarTask = "sourcesJar"
        override val modManifestPath = "fabric.mod.json"
    }

    object NeoForge : Loader("neoforge") {
        override val jarTask = "jar"
        override val sourcesJarTask = "sourcesJar"
        override val modManifestPath = "META-INF/neoforge.mods.toml"
        override val excludedResources =
            listOf(
                "fabric.mod.json",
                "META-INF/mods.toml",
                "aw/*.accesswidener",
                ".cache",
                "pack.mcmeta",
            )

        override fun generateManifest(ctx: Context): String {
            val dependencies = buildList {
                fun addDependencies(
                    container: NamedDomainObjectContainer<Dependency>,
                    type: String,
                ) {
                    container.forEach { dependency ->
                        add(
                            ForgeDependency(
                                modId = dependency.modid.get(),
                                side = dependency.environment.get().uppercase(Locale.ROOT),
                                versionRange = dependency.forgeLikeVersionRange.get(),
                                type = type,
                                ordering = dependency.ordering.get(),
                            )
                        )
                    }
                }

                addDependencies(ctx.extension.dependencies.required, "required")
                addDependencies(ctx.extension.dependencies.optional, "optional")
                addDependencies(ctx.extension.dependencies.incompatible, "incompatible")
            }

            val hasBanner =
                ctx.project.rootProject
                    .file("src/main/resources/assets/${ctx.modId}/banner.png")
                    .exists()

            val manifest =
                ForgeManifest(
                    license = ctx.licenseName,
                    issueTrackerURL = ctx.issuesUrl,
                    mods =
                        listOf(
                            ForgeMod(
                                modId = ctx.modId,
                                displayName = ctx.modName,
                                version = ctx.baseVersion,
                                displayURL = ctx.homepageUrl,
                                modUrl = ctx.homepageUrl,
                                iconFile = "${ctx.modId}:icon.png",
                                iconBlur = true,
                                bannerFile = if (hasBanner) "${ctx.modId}:banner.png" else "",
                                authors = ctx.authors.joinToString(", "),
                                credits =
                                    (ctx.authors + ctx.contributors).distinct().joinToString(", "),
                                description = ctx.description,
                            )
                        ),
                    dependencies = mapOf(ctx.modId to dependencies),
                    mixins =
                        buildList {
                            add(ForgeMixin("${ctx.modId}.gpu.mixins.json"))
                            if (ctx.shaderPackSupport) {
                                add(ForgeMixin("${ctx.modId}.iris.mixins.json"))
                            }
                            add(ForgeMixin("${ctx.modId}.blockgetter.mixins.json"))
                            add(ForgeMixin("${ctx.modId}.render.mixins.json"))
                            add(ForgeMixin("${ctx.modId}.targeting.mixins.json"))
                        },
                    accessTransformers =
                        listOf(ForgeAccessTransformer("aw/${ctx.currentMcVersion}.cfg")),
                )

            return TOML.encodeToString(manifest).replace(Regex("""(?m)^bannerFile = ""\r?\n"""), "")
        }
    }

    companion object {
        fun of(id: String): Loader =
            when (id) {
                "fabric" -> Fabric
                "neoforge" -> NeoForge
                else -> error("Unknown loader: '$id'")
            }
    }
}
