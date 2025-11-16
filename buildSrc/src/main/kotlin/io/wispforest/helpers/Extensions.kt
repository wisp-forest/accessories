package io.wispforest.helpers

import net.fabricmc.loom.api.fabricapi.FabricApiExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.accessors.runtime.addDependencyTo
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.the
import java.util.function.BiConsumer

object Extensions {

    val Project.modId: String get(){
        return this.rootProject.property("mod_id") as String
    }

    val Project.currentPlatform: String get() {
        return currentPlatformDisplayName.lowercase()
    }

    val Project.currentPlatformDisplayName: String get() {
        return ((project.properties["loom.platform"] as String?) ?: "Common");
    }

    val Project.libs: LibrariesForLibs get() {
        return the<LibrariesForLibs>();
    }

    private val Project.`fabricApi`: FabricApiExtension get() =
        this.extensions.getByName("fabricApi") as FabricApiExtension

    fun Project.fabricModule(dependencyMethod: BiConsumer<Dependency, Action<Dependency>>, vararg moduleNames: String, action: Action<Dependency>? = null) {
        for (moduleName in moduleNames) {
            dependencyMethod.accept(fabricApi.module(moduleName, libs.versions.fabric.api.asProvider().get())){
                (this as ModuleDependency).exclude(group = "fabric-api", module = "")

                action?.execute(this)
            }
        }
    }

    private fun DependencyHandler.addDependency(notation: String, configurationType: String, configuration: Action<ExternalModuleDependency>) =
        addDependencyTo(this, configurationType, notation, configuration)

    fun DependencyHandler.modrinthLocalRuntime(vararg projects: Pair<String, String>, action: Action<Entry>? = null) {
        modrinth(
            { notation, configuration -> addDependency(notation, "modLocalRuntime", configuration) },
            *projects,
            action = action
        )
    }

    fun DependencyHandler.modrinthCompileOnly(vararg projects: Pair<String, String>, action: Action<Entry>? = null) {
        modrinth(
            { notation, configuration -> addDependency(notation, "modCompileOnly", configuration) },
            *projects,
            action = action
        )
    }

    fun DependencyHandler.modrinthImplementation(vararg projects: Pair<String, String>, action: Action<Entry>? = null) {
        modrinth(
            { notation, configuration -> addDependency(notation, "modImplementation", configuration) },
            *projects,
            action = action
        )
    }

    fun DependencyHandler.modrinth(dependencyMethod: BiConsumer<String, Action<ExternalModuleDependency>>, vararg projects: Pair<String, String>, action: Action<Entry>? = null) {
        projects.forEach { (projectId, version) ->
            dependencyMethod.accept("maven.modrinth:${projectId}:${version}"){
                action?.execute(Entry(this, projectId))
            }
        }
    }

    data class Entry(val dependency: ExternalModuleDependency, val projectId: String)
}