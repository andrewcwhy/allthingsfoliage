plugins {
	id("mod-platform")
	id("net.neoforged.moddev")
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)
}

platform {
	loader = "neoforge"
	dependencies {
		required("minecraft") {
			forgeLikeVersionRange = prop("deps.minecraft")
		}
		required("neoforge") {
			forgeLikeVersionRange.set("[1,)")
		}
		// After Sway: each mod's model hooks run in load order, and the model wrapper that leaves the GPU renderer's
		// foliage out of the chunk mesh has to go around Sway's. Left to chance, Forge's order changes with the number of
		// mods installed, and the plants were drawn twice.
		required("sway") {
			slug("sway")
			forgeLikeVersionRange.set("[1,)")
			ordering.set("AFTER")
		}
	}
}

neoForge {
	version = prop("deps.neoforge")
	accessTransformers.from(rootProject.file("src/main/resources/aw/${stonecutter.current.version}.cfg"))
	validateAccessTransformers = true

	runs {
		register("client") {
			client()

			gameDirectory = file("run/")
			ideName = "NeoForge Client (${stonecutter.current.version})"

			programArgument("--username=Dev")

			// NeoForge validates every draw in the dev environment only, and the check indexes 16 vertex bindings
			// into the one-element arrays Iris answers for its pipelines, crashing the first frame with a shader pack.
			systemProperty("neoforge.disableGlValidation", "true")
			systemProperty("neoforge.disableBlaze3DValidation", "true")

			// 26.3's version manifest asks for this export, which the plugin leaves out (it passes the rest, among them
			// -XX:StackShadowPages=32, whose absence killed Fabric's dev runs); the launcher passes it.
			jvmArguments.addAll("--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED")
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
	sourceSets["main"].resources.srcDir("${rootDir}/versions/datagen/${sc.current.version.split("-")[0]}/src/main/generated")
}

repositories {
	mavenCentral()

	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") {
	    name = "Modrinth"
	}
}

dependencies {
	implementation("maven.modrinth:sway:${prop("deps.sway")}")

    // Optional shader integration: available for compilation, not bundled.
	if (hasProperty("deps.iris")) {
	    compileOnly("maven.modrinth:iris:${prop("deps.iris")}")
	}

	compileOnly("maven.modrinth:sodium:${prop("deps.sodium")}")
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}
