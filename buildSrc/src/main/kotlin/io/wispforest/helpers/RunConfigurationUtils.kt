package io.wispforest.helpers

import io.wispforest.helpers.Extensions.currentPlatform
import net.fabricmc.loom.configuration.ide.RunConfigSettings
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named

object RunConfigurationUtils {
    private val Project.sourceSets: SourceSetContainer get() =
        this.extensions.getByName("sourceSets") as SourceSetContainer

    private val NamedDomainObjectContainer<Configuration>.compileClasspath: NamedDomainObjectProvider<Configuration> get() =
        named<Configuration>("compileClasspath")

    // Attempts to add either testmod run, mixin hotswapping run, and render doc run using owolib
    fun NamedDomainObjectContainer<RunConfigSettings>.createExtraRunConfigs(project: Project) {
        val currentPlatform = project.currentPlatform
        val rootProject = project.rootProject

        val enabledMixinDebuggingPlatforms = (rootProject.property("enabled_mixin_debugging_platforms") as String).split(",")
        val enabledRenderDocPlatforms = (rootProject.property("enabled_renderdoc_debugging_platforms") as String).split(",")
        val enabledTestmodPlatforms = (rootProject.property("enabled_testmod_platforms") as String).split(",")

        val doseTestModExist = currentPlatform in enabledTestmodPlatforms;

        if (doseTestModExist) {
            create("testmodClient") {
                client()
                ideConfigGenerated(true)
                name("Testmod Client")
                project.setupTestMod(this)
            }
            create("testmodServer") {
                server()
                ideConfigGenerated(true)
                name("Testmod Server")
                project.setupTestMod(this)
            }
        }

        if (currentPlatform in enabledMixinDebuggingPlatforms) {
            create("clientMixinDebug") {
                client()
                ideConfigGenerated(true)
                name("Minecraft Client - (Mixin Debug)")
                project.addMixinAsJavaAgent(this)
                project.setupRunWithArgs(this)
            }
            if (doseTestModExist) {
                create("testmodClientMixinDebug") {
                    client()
                    ideConfigGenerated(true)
                    name("Testmod Client - (Mixin Debug)")
                    project.addMixinAsJavaAgent(this)
                    project.setupRunWithArgs(this)
                    project.setupTestMod(this)
                }
            }
        } else {
            println("Mixin Debugging for $currentPlatform is disabled just a FYI, adjust the gradle.properties 'enabled_mixin_debugging_platforms' field to contain such if you want the runs!")
        }

        if (currentPlatform in enabledRenderDocPlatforms) {
            val renderDocPath = System.getenv("renderDocPath")

            if (renderDocPath != null) {
                create("owoClientRenderDoc") {
                    client()
                    ideConfigGenerated(true)
                    name("Minecraft Client - (RenderDoc)")
                    source(project.sourceSets["main"])
                    vmArg("-Dowo.renderdocPath=$renderDocPath")
                }
                if (doseTestModExist) {
                    create("testmodOwoClientRenderDoc") {
                        client()
                        ideConfigGenerated(true)
                        name("Testmod Client - (RenderDoc)")
                        source(project.sourceSets["testmod"])
                        vmArg("-Dowo.renderdocPath=$renderDocPath")
                        project.setupTestMod(this)
                    }
                }
            } else {
                println("Unable to create Render Doc runs due to the renderDocPath variable is not found! Please add such and regenerate runs to get access if desired!")
            }
        } else {
            println("Render Doc Debugging for $currentPlatform is disabled just a FYI, adjust the gradle.properties 'enabled_mixin_debugging_platforms' field to contain such if you want the runs!")
        }
    }

    // Used to setup the modid info for testmod instance and add the source set to the run config
    private fun Project.setupTestMod(settings: RunConfigSettings) {
        this.afterEvaluate {
            settings.source(this.sourceSets["testmod"])
        }

        if (this.currentPlatform != "neoforge") return

        settings.mods {
            create("${rootProject.property("test_mod_id")}".replace("-", "_")) { sourceSet(this@setupTestMod.sourceSets["testmod"]) }
            create("${rootProject.property("mod_id")}".replace("-", "_")) { sourceSet(this@setupTestMod.sourceSets["main"]) }
        }
    }

    // Used to setup some of the log stuff for fabric loader as for some reason it has a possibility to disappear... idk
    private fun Project.setupRunWithArgs(runConfig: RunConfigSettings) {
        runConfig.vmArg("-Dfabric.dli.config=${this.file(".gradle/loom-cache/launch.cfg")}")
        runConfig.vmArg("-Dfabric.dli.env=client")
        runConfig.vmArg("-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient")
        runConfig.vmArg("-Dlog4j.configurationFile=${this.file(".gradle/loom-cache/log4j.xml")}")
        runConfig.vmArg("-Dfabric.log.disableAnsi=false")
        runConfig.vmArg("-Dmixin.debug.export=true")
    }

    // Searches the projects `compileClasspath` to attempt to find any dependency of mixin either by checking all
    // dependencies or from the incoming artifact view
    fun Project.gatherMixinFiles(): FileCollection? {
        val compileClasspath = this.configurations.compileClasspath.get();

        val mixin = compileClasspath
            .allDependencies
            .asIterable()
            .firstOrNull { it.name == "sponge-mixin" }

        if (mixin == null) return null

        return compileClasspath.incoming.artifactView {
            componentFilter { id ->
                if (id is ModuleComponentIdentifier) {
                    return@componentFilter id.moduleIdentifier.group == mixin.group
                            && id.moduleIdentifier.name == mixin.name
                            && id.version == mixin.version
                }

                return@componentFilter false
            }
        }.files
    }

    // Attempts to add the first Mixin dependency found from the classpath to the given passed run config
    private fun Project.addMixinAsJavaAgent(settings: RunConfigSettings) {
        try {
            project.afterEvaluate {
                val mixinFiles = this.gatherMixinFiles();

                if (mixinFiles == null || mixinFiles.isEmpty) {
                    println("[Error]: Was unable to locate any mixin files on the compileClasspath, meaning Mixin run was unable to add any as a java agent.")

                    return@afterEvaluate;
                }

                if (mixinFiles.files.size > 1) {
                    println("[Warning]: Multiple mixin instances found when trying to add such as a java agent, using the first instance and hoping it works.")
                }

                settings.vmArg("-javaagent:\"${mixinFiles.first().path}\"")

                println("[Info]: Was able to setup a given Mixin Hotswap run with the mixin instance as a java agent")
            }
        } catch (e: Exception) {
            println("[Error]: MixinHotswap Run had a issue!")
            e.printStackTrace()
        }
    }
}