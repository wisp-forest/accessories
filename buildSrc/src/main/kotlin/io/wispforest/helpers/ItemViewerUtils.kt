package io.wispforest.helpers

import io.wispforest.helpers.Extensions.currentPlatform
import io.wispforest.helpers.Extensions.libs
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project

object ItemViewerUtils {
    fun Project.setupItemViewerDependencies(modCompileOnly: (Any) -> Unit, modLocalRuntime: (Any) -> Unit) {
        val enabledViewers = (this.rootProject.property("enabled_item_viewers") as String).split(",")
        val selectedViewer = (this.rootProject.property("selected_item_viewers") as String)

        val projectPlatform = this.currentPlatform
        val libs = this.libs

        val allowNonAPIForCompile = true;

        fun setupViewerDependency(name: String, api: Any, mod: Any) {
            if (name in enabledViewers) {
                modCompileOnly(api)
                if (allowNonAPIForCompile) modCompileOnly(mod)
                if (selectedViewer == name) modLocalRuntime(mod)
            }
        }

        // Item Viewer Libs
        setupViewerDependency("rei", libs.create("rei.$projectPlatform.api").get(), libs.create("rei.$projectPlatform").get())

        setupViewerDependency("emi", "${libs.create("emi.$projectPlatform").get()}:api", libs.create("emi.$projectPlatform").get())

        val versions = libs.versions

        setupViewerDependency("jei",
            libs.repackWithMinecraft("jei.$projectPlatform.api", "jei_minecraft" to versions.jei.minecraft.get()),
            libs.repackWithMinecraft("jei.$projectPlatform", "jei_minecraft" to versions.jei.minecraft.get())
        )
    }

    fun LibrariesForLibs.repackWithMinecraft(alias: String, minecraftVersionData: Pair<String, String> ): String {
        return "${this.create(alias).get()}".replace("\${${minecraftVersionData.first}}", minecraftVersionData.second)
    }
}