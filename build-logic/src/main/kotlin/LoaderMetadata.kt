import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class FabricManifest(
	val schemaVersion: Int = 1,
	val id: String,
	val name: String,
	val version: String,
	val authors: List<String>,
	val contributors: List<String>,
	val contact: Map<String, String>,
	val custom: JsonObject,
	val description: String,
	val icon: String,
	val license: String,
	val environment: String = "*",
	val accessWidener: String,
	val entrypoints: Map<String, List<String>>,
	val mixins: List<String>,
	val depends: Map<String, String> = emptyMap(),
	val recommends: Map<String, String> = emptyMap(),
	val breaks: Map<String, String> = emptyMap()
)

@Serializable
data class ForgeManifest(
	val modLoader: String = "javafml",
	val loaderVersion: String = "[2,)",
	val license: String,
	val issueTrackerURL: String,
	val mods: List<ForgeMod>,
	val dependencies: Map<String, List<ForgeDependency>> = emptyMap(),
	val mixins: List<ForgeMixin> = emptyList(),
	/** Access transformers kept outside META-INF, which the loader only reads where the manifest names them. */
	val accessTransformers: List<ForgeAccessTransformer> = emptyList()
)

@Serializable
data class ForgeMod(
	val modId: String,
	val displayName: String,
	val version: String,
	val displayURL: String,
	val modUrl: String,
	/** The mod list's picture before NeoForge 26.2, which reads the two below instead and warns about this one. */
	val logoFile: String = "",
	/** NeoForge 26.2 on: the square picture beside the mod's name in the list, and whether it is smoothed when scaled. */
	val iconFile: String = "",
	val iconBlur: Boolean = false,
	/** NeoForge 26.2 on: the wide picture above the mod's description, fitted to 50 pixels of height. */
	val bannerFile: String = "",
	val authors: String,
	val logoBlur: Boolean = false,
	val credits: String,
	val description: String
)

@Serializable
data class ForgeDependency(
	val modId: String,
	val side: String,
	val versionRange: String,
	val mandatory: Boolean,
	val type: String,
	val ordering: String
)

@Serializable
data class ForgeMixin(val config: String)

@Serializable
data class ForgeAccessTransformer(val file: String)
