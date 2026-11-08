@file:Suppress("unused", "DuplicatedCode")

import dev.kikugie.fletching_table.extension.FletchingTableExtension
import dev.kikugie.stonecutter.StonecutterExperimentalAPI
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import javax.inject.Inject
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.api.fabricapi.FabricApiExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

val Project.sc: StonecutterBuildExtension
    get() = extensions.getByType<StonecutterBuildExtension>()

@OptIn(StonecutterExperimentalAPI::class)
fun Project.prop(name: String): String = sc.properties.get<String>(name)

fun Project.env(variable: String): String? = providers.environmentVariable(variable).orNull

fun Project.envTrue(variable: String): Boolean =
    env(variable)?.equals("true", ignoreCase = true) == true

fun RepositoryHandler.strictMaven(
    url: String,
    vararg groups: String,
    configure: MavenArtifactRepository.() -> Unit = {},
) = exclusiveContent {
    forRepository {
        maven(url) {
            configure()
        }
    }

    filter {
        groups.forEach(::includeGroup)
    }
}

abstract class GenerateModManifestTask : DefaultTask() {
    @get:Input abstract val content: Property<String>

    @get:OutputFile abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(content.get())
    }
}

abstract class ModPlatformPlugin @Inject constructor() : Plugin<Project> {
    override fun apply(project: Project) =
        with(project) {
            val loaderId = buildFile.name.removePrefix("build.").removeSuffix(".gradle.kts")

            val inferredLoader = Loader.of(loaderId)

            val extension =
                extensions
                    .create(
                        "platform",
                        ModPlatformExtension::class.java,
                    )
                    .apply {
                        loader.convention(inferredLoader.id)
                        jarTask.convention(inferredLoader.jarTask)
                        sourcesJarTask.convention(inferredLoader.sourcesJarTask)
                    }

            listOf(
                    "org.jetbrains.kotlin.jvm",
                    "com.google.devtools.ksp",
                    "dev.kikugie.fletching-table",
                )
                .forEach { pluginId ->
                    apply(plugin = pluginId)
                }

            // Set toolchains before Loom or ModDevGradle configures its compile tasks.
            extensions.configure<JavaPluginExtension> {
                toolchain.languageVersion.set(JavaLanguageVersion.of(25))
            }
            extensions.configure<KotlinJvmProjectExtension> {
                jvmToolchain(25)
            }

            afterEvaluate {
                val ctx =
                    Context(
                        project = this,
                        extension = extension,
                        loader = Loader.of(extension.loader.get()),
                        stonecutter = sc,
                    )

                configureProject(ctx)
            }
        }

    private fun Project.configureProject(ctx: Context) {
        listOf(
                "java",
                "me.modmuss50.mod-publish-plugin",
                "idea",
            )
            .forEach { pluginId ->
                apply(plugin = pluginId)
            }

        version = ctx.fullVersion
        ctx.extension.requiredJava.set(ctx.javaVersion)

        if (ctx.loader.isFabricLike) {
            ctx.extension.dependencies {
                required("java") {
                    fabricLikeVersionRange = ">=${ctx.javaVersion.majorVersion}"
                }
            }
        }

        configureKotlinRuntime(ctx)
        configurePassabilityDataGeneration(ctx)
        configureFletchingTable()
        registerGenerateManifestTask(ctx)
        configureJarTask(ctx)
        configureIdea()
        configureProcessResources(ctx)
        configureJava(ctx)
        registerBuildAndCollectTask(ctx)
        configureModPublishing(ctx)

        if (envTrue("PUB_MAVEN_ENABLE")) {
            apply(plugin = "com.vanniktech.maven.publish")
            configureMavenPublishing(ctx)
        }
    }

    /** Generates shared runtime data once through Fabric's native datagen runner. */
    private fun Project.configurePassabilityDataGeneration(ctx: Context) {
        val generated =
            rootProject.layout.projectDirectory.dir("src/generated/${ctx.currentMcVersion}")
        val generatorPath = ":${ctx.currentMcVersion}-fabric:runDatagen"
        val main = the<JavaPluginExtension>().sourceSets.named("main")

        if (ctx.loader.isFabricLike) {
            val api = extensions.getByType<FabricApiExtension>()
            val loom = extensions.getByType<LoomGradleExtensionAPI>()
            if (loom.runConfigs.findByName("datagen") == null) {
                api.configureDataGeneration {
                    outputDirectory.set(generated.asFile)
                    addToResources.set(false)
                    client.set(false)
                    modId.set(ctx.modId)
                }
            } else {
                loom.runConfigs.named("datagen") {
                    property("fabric-api.datagen.output-dir", generated.asFile.absolutePath)
                    property("fabric-api.datagen.modid", ctx.modId)
                }
            }
            tasks.named("runDatagen") {
                outputs.dir(generated)
            }
        }

        // Do not make processResources depend on datagen: the runner needs compiled code/resources.
        // Additional output joins the runtime classpath and the JAR after generation completes.
        main.configure {
            output.dir(generated.asFile)
        }
        tasks.withType<Jar>().configureEach {
            if (
                name == ctx.extension.jarTask.get() ||
                    name == ctx.extension.sourcesJarTask.get()
            ) {
                dependsOn(generatorPath)
                exclude(".cache/**")
            }
        }
        tasks
            .matching { it.name == "runClient" || it.name == "runServer" }
            .configureEach {
                dependsOn(generatorPath)
            }
    }

    private fun Project.configureKotlinRuntime(ctx: Context) {
        val requirements = ctx.extension.dependencies.required
        requirements.maybeCreate("minecraft").apply {
            fabricLikeVersionRange.set(">=26.3")
            forgeLikeVersionRange.set("[26.3,)")
        }
        if (ctx.loader.isFabricLike) {
            dependencies.add(
                "implementation",
                "net.fabricmc:fabric-language-kotlin:1.14.2+kotlin.2.4.21",
            )
            requirements.maybeCreate("fabric-language-kotlin").apply {
                fabricLikeVersionRange.set(">=1.14.2+kotlin.2.4.21")
            }
        } else {
            repositories.maven("https://thedarkcolour.github.io/KotlinForForge/") {
                name = "Kotlin for Forge"
            }
            dependencies.add("implementation", "thedarkcolour:kotlinforforge-neoforge:6.4.0")
            requirements.maybeCreate("kotlinforforge").apply {
                forgeLikeVersionRange.set("[6.4.0,)")
            }
        }
    }

    private fun Project.configureJava(ctx: Context) {
        extensions.configure<JavaPluginExtension>("java") {
            withSourcesJar()
            withJavadocJar()

            sourceCompatibility = ctx.javaVersion
            targetCompatibility = ctx.javaVersion
        }
    }

    private fun Project.registerGenerateManifestTask(ctx: Context) {
        val manifestOutputDirectory = layout.buildDirectory.dir("generated/modManifest")

        val generateManifest =
            tasks.register<GenerateModManifestTask>("generateModManifest") {
                content.set(ctx.loader.generateManifest(ctx))

                outputFile.set(
                    manifestOutputDirectory.map { directory ->
                        directory.file(ctx.loader.modManifestPath)
                    }
                )
            }

        the<JavaPluginExtension>().sourceSets.named("main") {
            resources.srcDir(manifestOutputDirectory)
        }

        tasks.named<ProcessResources>("processResources") {
            dependsOn(generateManifest)
        }
    }

    private fun Project.configureProcessResources(ctx: Context) {
        tasks.named<ProcessResources>("processResources") {
            dependsOn(
                tasks.named("stonecutterGenerate"),
                "kspKotlin",
            )

            filesMatching("*.mixins.json") {
                expand("java" to "JAVA_${ctx.javaVersion.majorVersion}")
            }

            exclude(ctx.loader.excludedResources)

            // Ignore obsolete configurations if they remain in the resource directory.
            exclude(
                "${ctx.modId}.mixins.json",
                "*.polytone.mixins.json",
                "*.snowrealmagic.mixins.json",
                "*.snowrealmagic_neoforge.mixins.json",
            )

            // Include Iris mixins only when shader integration is configured.
            if (!ctx.shaderPackSupport) {
                exclude("*.iris.mixins.json")
            }

            // The block-getter integration applies only to NeoForge.
            if (ctx.loader !is Loader.NeoForge) {
                exclude("*.blockgetter.mixins.json")
            }
        }
    }

    private fun Project.configureJarTask(ctx: Context) {
        val generateManifest = tasks.named("generateModManifest")

        tasks.withType<Jar>().configureEach {
            archiveBaseName.set(ctx.modId)
            dependsOn(generateManifest)
        }
    }

    private fun Project.configureIdea() {
        extensions.configure<IdeaModel>("idea") {
            module {
                isDownloadJavadoc = true
                isDownloadSources = true
            }
        }
    }

    private fun Project.configureFletchingTable() {
        extensions.configure<FletchingTableExtension> {
            // Mixin configurations maintain their own lists and plugins.
            j52j.register("main") {
                extension("json", "**/*.json5")
            }
        }
    }

    private fun Project.registerBuildAndCollectTask(ctx: Context) {
        tasks.register<Copy>("buildAndCollect") {
            from(
                tasks.named(ctx.extension.jarTask.get()),
                tasks.named(ctx.extension.sourcesJarTask.get()),
                tasks.named("javadocJar"),
            )

            into(rootProject.layout.buildDirectory.dir("libs/${ctx.basicVersion}"))

            dependsOn("build")
        }
    }
}
