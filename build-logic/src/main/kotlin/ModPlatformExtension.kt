@file:Suppress("unused")

import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

abstract class ModPlatformExtension {

	abstract val requiredJava: Property<JavaVersion>
	abstract val loader: Property<String>
	abstract val jarTask: Property<String>
	abstract val sourcesJarTask: Property<String>

	@get:Nested
	abstract val dependencies: DependenciesConfig

	init {
		requiredJava.convention(JavaVersion.VERSION_21)
	}

	fun dependencies(action: Action<DependenciesConfig>) {
		action.execute(dependencies)
	}
}

abstract class DependenciesConfig @Inject constructor(val objects: ObjectFactory) {

	private fun container() = objects.domainObjectContainer(Dependency::class.java)

	val required: NamedDomainObjectContainer<Dependency> = container()
	val optional: NamedDomainObjectContainer<Dependency> = container()
	val incompatible: NamedDomainObjectContainer<Dependency> = container()
	val embeds: NamedDomainObjectContainer<Dependency> = container()
	/**
	 * Required on Modrinth and CurseForge, so launchers install it alongside the mod, but left out of the jar's own
	 * metadata: the game still starts without it, and a player can remove it through their launcher.
	 */
	val publishRequired: NamedDomainObjectContainer<Dependency> = container()

	fun required(modid: String, action: Action<Dependency>): Dependency? = required.create(modid, action)
	fun publishRequired(modid: String, action: Action<Dependency>): Dependency? = publishRequired.create(modid, action)
	fun optional(modid: String, action: Action<Dependency>): Dependency? = optional.create(modid, action)
	fun incompatible(modid: String, action: Action<Dependency>): Dependency? = incompatible.create(modid, action)
	fun embeds(modid: String, action: Action<Dependency>): Dependency? = embeds.create(modid, action)
}

abstract class Dependency @Inject constructor(val name: String) {

	abstract val modid: Property<String>
	abstract val modrinth: Property<String>
	abstract val curseforge: Property<String>
	abstract val fabricLikeVersionRange: Property<String>
	abstract val forgeLikeVersionRange: Property<String>
	abstract val environment: Property<String>
	/** Forge and NeoForge: whether this mod loads BEFORE or AFTER the dependency, or NONE. */
	abstract val ordering: Property<String>

	init {
		modid.convention(name)
		fabricLikeVersionRange.convention("*")
		forgeLikeVersionRange.convention("(,]")
		environment.convention("both")
		ordering.convention("NONE")
	}

	fun slug(slug: String) {
		modrinth.set(slug)
		curseforge.set(slug)
	}

	fun slug(modrinthSlug: String? = null, curseforgeSlug: String? = null) {
		if (modrinthSlug != null) {
			modrinth.set(modrinthSlug)
		}
		if (curseforgeSlug != null) {
			curseforge.set(curseforgeSlug)
		}
	}
}
