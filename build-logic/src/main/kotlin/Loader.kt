@file:Suppress("unused")

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.peanuuutz.tomlkt.Toml
import org.gradle.api.NamedDomainObjectContainer
import java.util.*

private val JSON = Json {
    prettyPrint = true;
    encodeDefaults = true
}

private val TOML = Toml { }

sealed class Loader(val id: String) {
	abstract val jarTask: String
	abstract val sourcesJarTask: String
	abstract val modManifestPath: String
	abstract val excludedResources: List<String>

	open val isFabricLike: Boolean = false

	abstract fun generateManifest(ctx: Context): String

	sealed class FabricLike(id: String) : Loader(id) {
		override val isFabricLike = true

		override val excludedResources = listOf(
			"META-INF/mods.toml", "META-INF/neoforge.mods.toml", "aw/*.cfg", ".cache", "pack.mcmeta"
		)

		override fun generateManifest(ctx: Context): String {
			val manifest = FabricManifest(
				id = ctx.modId,
				name = ctx.modName,
				version = ctx.baseVersion,
				authors = ctx.authors,
				contributors = ctx.contributors,
				contact = mapOf(
					"sources" to ctx.sourcesUrl, "issues" to ctx.issuesUrl, "homepage" to ctx.homepageUrl
				),
				custom = buildJsonObject {
					putJsonObject("modmenu") {
						putJsonObject("links") {
							put("modmenu.discord", ctx.discordUrl)
						}
					}
				},
				description = ctx.description,
				icon = "assets/${ctx.modId}/icon.png",
				license = ctx.licenseName,
				accessWidener = "aw/${ctx.currentMcVersion}.accesswidener",
				entrypoints = mapOf(
					"main" to listOf("${ctx.modGroup}.${ctx.modId}.platform.fabric.FabricEntrypoint"),
					"client" to listOf("${ctx.modGroup}.${ctx.modId}.platform.fabric.FabricClientEntrypoint"),
					"fabric-datagen" to listOf("${ctx.modGroup}.${ctx.modId}.platform.fabric.datagen.FabricDataGeneratorEntrypoint"),
					// Only queried when Mod Menu is installed, so it costs nothing when it is not.
					"modmenu" to listOf("${ctx.modGroup}.${ctx.modId}.platform.fabric.FabricModMenuIntegration")
				),
				// The GPU foliage mixins are only declared where the GPU renderer has been ported, so
				// their targets are known to be there.
				mixins = buildList {
					add("${ctx.modId}.mixins.json")
					add("${ctx.modId}.gpu.mixins.json")

					if (ctx.shaderPackSupport) {
						add("${ctx.modId}.iris.mixins.json")
					}

					// The foliage in Polytone's shadow map, which Polytone has from 1.21.1. Its plugin skips it unless Polytone is
					// installed. Not on 26.3 yet either.
					if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.1")
						&& !ctx.stonecutter.eval(ctx.currentMcVersion, ">=26.3")) {
						add("${ctx.modId}.polytone.mixins.json")
					}

					add("${ctx.modId}.pass.mixins.json")
				},
				depends = ctx.extension.dependencies.required.associate { it.modid.get() to it.fabricLikeVersionRange.get() },
				recommends = ctx.extension.dependencies.optional.associate { it.modid.get() to it.fabricLikeVersionRange.get() },
				breaks = ctx.extension.dependencies.incompatible.associate { it.modid.get() to it.fabricLikeVersionRange.get() })
			return JSON.encodeToString(manifest)
		}
	}

	object Fabric : FabricLike("fabric") {
		override val jarTask = "jar"
		override val sourcesJarTask = "sourcesJar"
		override val modManifestPath = "fabric.mod.json"
	}

	sealed class ForgeLike(id: String) : Loader(id) {
		override val jarTask = "jar"
		override val sourcesJarTask = "sourcesJar"
		override val excludedResources = listOf(
			"fabric.mod.json", "aw/*.accesswidener", ".cache"
		)

		override fun generateManifest(ctx: Context): String {
			val forgeDeps = mutableListOf<ForgeDependency>()

			fun addDeps(container: NamedDomainObjectContainer<Dependency>, type: String) {
				container.forEach {
					forgeDeps.add(
						ForgeDependency(
							modId = it.modid.get(),
							side = it.environment.get().uppercase(Locale.getDefault()),
							versionRange = it.forgeLikeVersionRange.get(),
							mandatory = type == "required",
							type = type,
							ordering = it.ordering.get()
						)
					)
				}
			}

			addDeps(ctx.extension.dependencies.required, "required")
			addDeps(ctx.extension.dependencies.optional, "optional")
			addDeps(ctx.extension.dependencies.incompatible, "incompatible")

			// NeoForge 26.2 on shows a square icon in the mod list and a wide banner above the description, each named as
			// an asset, and warns about a mod still naming a logoFile. Before, and on Forge, the one logoFile is all
			// there is. The banner is only named once its picture is among the resources.
			val newPictures = ctx.loader is Loader.NeoForge && ctx.stonecutter.eval(ctx.currentMcVersion, ">=26.2")
			val hasBanner = ctx.project.rootProject.file("src/main/resources/assets/${ctx.modId}/banner.png").exists()
			val manifest = ForgeManifest(
				license = ctx.licenseName, issueTrackerURL = ctx.issuesUrl, mods = listOf(
					ForgeMod(
						modId = ctx.modId,
						displayName = ctx.modName,
						version = ctx.baseVersion,
						displayURL = ctx.homepageUrl,
						modUrl = ctx.homepageUrl,
						logoFile = if (newPictures) "" else "assets/${ctx.modId}/icon.png",
						iconFile = if (newPictures) "${ctx.modId}:icon.png" else "",
						iconBlur = newPictures,
						bannerFile = if (newPictures && hasBanner) "${ctx.modId}:banner.png" else "",
						authors = ctx.authors.joinToString(", "),
						credits = "${ctx.authors.joinToString(", ")} Contributors: ${ctx.contributors.joinToString(", ")}",
						description = ctx.description
					)
				), dependencies = mapOf(ctx.modId to forgeDeps),
				mixins = buildList {
					add(ForgeMixin("${ctx.modId}.mixins.json"))

					// Shader pack support, as on Fabric, where there is an Iris for the loader to write it against.
					if (ctx.shaderPackSupport) {
						add(ForgeMixin("${ctx.modId}.iris.mixins.json"))
					}

					// Polytone's shadow map, as on Fabric: not on 26.3 yet.
					if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.1")
						&& !ctx.stonecutter.eval(ctx.currentMcVersion, ">=26.3")) {
						add(ForgeMixin("${ctx.modId}.polytone.mixins.json"))
					}

					add(ForgeMixin("${ctx.modId}.blockgetter.mixins.json"))
					add(ForgeMixin("${ctx.modId}.pass.mixins.json"))
				},
				// NeoForge 26.3 opens GlRenderPipeline's constructor for the shader pack support, as Fabric's widener does;
				// every other version's transformer is empty and goes unnamed.
				accessTransformers = buildList {
					if (ctx.loader is Loader.NeoForge && ctx.stonecutter.eval(ctx.currentMcVersion, ">=26.3")) {
						add(ForgeAccessTransformer("aw/${ctx.currentMcVersion}.cfg"))
					}
				}
			)

			// An empty list is written out as an empty array, and a picture a version does not name as an empty string or
			// a false, none of which a loader needs to read: left out.
			return TOML.encodeToString(manifest)
				.replace(Regex("""(?m)^accessTransformers = \[\s*]\r?\n"""), "")
				.replace(Regex("""(?m)^(logoFile|iconFile|bannerFile) = ""\r?\n"""), "")
				.replace(Regex("""(?m)^iconBlur = false\r?\n"""), "")
		}
	}

	object NeoForge : ForgeLike("neoforge") {
		override val modManifestPath = "META-INF/neoforge.mods.toml"
		override val excludedResources = (super.excludedResources + "META-INF/mods.toml") + "pack.mcmeta"
	}

	companion object {
		fun of(id: String): Loader = when (id) {
			"fabric" -> Fabric
			"neoforge" -> NeoForge
			else -> error("Unknown loader: '$id'")
		}
	}
}
