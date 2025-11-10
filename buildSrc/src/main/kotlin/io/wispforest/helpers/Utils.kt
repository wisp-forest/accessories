package io.wispforest.helpers

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.fabricmc.loom.configuration.ide.RunConfigSettings
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import java.net.URI

object Utils {
    private val Project.sourceSets: SourceSetContainer get() =
        this.extensions.getByName("sourceSets") as SourceSetContainer

    private val NamedDomainObjectContainer<Configuration>.compileClasspath: NamedDomainObjectProvider<Configuration> get() =
        named<Configuration>("compileClasspath")

    fun getSetupRunsAction(project: Project): Action<NamedDomainObjectContainer<RunConfigSettings>> {
        return Action {
            val currentPlatform: String = ((project.properties["loom.platform"] as String?) ?: "common")
            val rootProject = project.rootProject;

            val enabledMixinDebuggingPlatforms = (rootProject.property("enabled_mixin_debugging_platforms") as String).split(",")
            val enabledRenderDocPlatforms = (rootProject.property("enabled_renderdoc_debugging_platforms") as String).split(",");
            val enabledTestmodPlatforms = (rootProject.property("enabled_testmod_platforms") as String).split(",");

            val renderDocPath = System.getenv("renderDocPath");

            val addMixinDebugginRuns = enabledMixinDebuggingPlatforms.contains(currentPlatform)
            val addTestModRuns = enabledTestmodPlatforms.contains(currentPlatform)

            fun setupTestMod(settings: RunConfigSettings) {
                project.afterEvaluate {
                    settings.source(project.sourceSets["testmod"])
                }

                if (currentPlatform != "neoforge") return

                settings.mods {
                    create("${rootProject.property("test_mod_id")}") { sourceSet(project.sourceSets["testmod"]) }
                    create("${rootProject.property("mod_id")}") { sourceSet(project.sourceSets["main"]) }
                }
            }

            if (addTestModRuns) {
                create("testmodClient") {
                    client()
                    ideConfigGenerated(true)
                    name("Testmod Client")
                    setupTestMod(this)
                }
                create("testmodServer") {
                    server()
                    ideConfigGenerated(true)
                    name("Testmod Server")
                    setupTestMod(this)
                }
            }

            if (addMixinDebugginRuns) {
                fun addMixinAsJavaAgent(settings: RunConfigSettings) {
                    // TODO: OUTSOURCE TO METHOD!
                    try {
                        project.afterEvaluate {
                            val mixin = this.configurations.compileClasspath.get()
                                .allDependencies
                                .asIterable()
                                .firstOrNull { it.name == "sponge-mixin" }

                            if (mixin != null) {
                                val file = this.configurations.compileClasspath.get().incoming.artifactView {
                                    componentFilter { id ->
                                        if (id is ModuleComponentIdentifier) {
                                            return@componentFilter id.moduleIdentifier.group == mixin.group
                                                    && id.moduleIdentifier.name == mixin.name
                                                    && id.version == mixin.version
                                        }

                                        return@componentFilter false
                                    }
                                }.files.first()

                                settings.vmArg("-javaagent:\"${file.path}\"")
                                println("[Info]: Mixin Hotswap Run should be working")
                            } else {
                                println("[Warning]: Unable to locate file path for Mixin Jar, HotSwap Run will not work!!!")
                            }
                        }
                    } catch (e: Exception) {
                        println("[Error]: MixinHotswap Run had a issue!")
                        e.printStackTrace()
                    }
                }

                create("clientMixinDebug") {
                    client()
                    ideConfigGenerated(true)
                    name("Minecraft Client - (Mixin Debug)")
                    vmArg("-Dfabric.dli.config=${project.file(".gradle/loom-cache/launch.cfg")}")
                    vmArg("-Dfabric.dli.env=client")
                    vmArg("-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient")

                    addMixinAsJavaAgent(this);

                    vmArg("-Dlog4j.configurationFile=${project.file(".gradle/loom-cache/log4j.xml")}")
                    vmArg("-Dfabric.log.disableAnsi=false")
                    vmArg("-Dmixin.debug.export=true")
                }

                if (addTestModRuns) {
                    create("testmodClientMixinDebug") {
                        client()
                        ideConfigGenerated(true)
                        name("Testmod Client - (Mixin Debug)")
                        vmArg("-Dfabric.dli.config=${project.file(".gradle/loom-cache/launch.cfg")}")
                        vmArg("-Dfabric.dli.env=client")
                        vmArg("-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient")

                        addMixinAsJavaAgent(this);

                        vmArg("-Dlog4j.configurationFile=${project.file(".gradle/loom-cache/log4j.xml")}")
                        vmArg("-Dfabric.log.disableAnsi=false")
                        vmArg("-Dmixin.debug.export=true")

                        setupTestMod(this)
                    }
                }
            } else {
                println("Mixin Debugging for $currentPlatform is disabled just a FYI, adjust the gradle.properties 'enabled_mixin_debugging_platforms' field to contain such if you want the runs!")
            }

            if (enabledRenderDocPlatforms.contains(currentPlatform)) {
                if (renderDocPath != null) {
                    create("owoClientRenderDoc") {
                        client()
                        ideConfigGenerated(true)
                        name("Minecraft Client - (RenderDoc)")
                        source(project.sourceSets["main"])
                        vmArg("-Dowo.renderdocPath=$renderDocPath")
                    }
                    if (addTestModRuns) {
                        create("testmodOwoClientRenderDoc") {
                            client()
                            ideConfigGenerated(true)
                            name("Testmod Client - (RenderDoc)")
                            source(project.sourceSets["testmod"])
                            vmArg("-Dowo.renderdocPath=$renderDocPath")

                            setupTestMod(this)
                        }
                    }
                } else {
                    println("Unable to create Render Doc runs due to the renderDocPath variable is not found! Please add such and regenerate runs to get access if desired!")
                }
            } else {
                println("Render Doc Debugging for $currentPlatform is disabled just a FYI, adjust the gradle.properties 'enabled_mixin_debugging_platforms' field to contain such if you want the runs!")
            }
        }
    }

    //-- Fabric FMJ Entry Utils
    const val removeLineTarget = "#REMOVE_LINE#"
    const val baseIndentation = "  "

    fun indentation (level: Int): String {
        return baseIndentation.repeat(level);
    }

    const val separator = ",\n"

    fun buildListEntry (project: Project, keys: List<String>): String {
        val rootProject = project.rootProject;
        var entries = "";

        keys.forEachIndexed { i: Int, key: String ->
            if (key.isBlank()) return@forEachIndexed

            val contactKey = key.lowercase().replace(" ", "_");
            val hasContact = rootProject.hasProperty(contactKey)

            if (hasContact) {
                if (i == 0) entries += "$removeLineTarget\"$separator"

                entries +=
                    """{
  "name": "$key",
  "contact": { 
    "homepage": "${rootProject.property(contactKey)}"
  }
}""".prependIndent(indentation(2))
            } else {
                if (i != 0) entries += "\"";

                entries += "$key\"";
            }
            entries += if (i < keys.size - 1) separator else "\n";
        }

        return entries + "${indentation(2)}\"$removeLineTarget";
    }

    fun buildMapEntry(project: Project, vararg keys: String): String {
        val rootProject = project.rootProject;
        var fullEntry = "$removeLineTarget\": \"\"$separator";

        keys.forEachIndexed { i, key ->
            fullEntry += run {
                val propertyValue = rootProject.property("mod_$key") as String
                "${indentation(2)}${(if (propertyValue.isNotBlank()) "\"$key\": \"$propertyValue\"" else "")}"
            }
            fullEntry += if (i < keys.size - 1) separator else "\n";
        }

        fullEntry += "${indentation(2)}\"$removeLineTarget"

        return fullEntry;
    }

    fun currentPlatform(project: Project): String {
        return ((project.properties["loom.platform"] as String?) ?: "common");
    }

    fun modId(project: Project): String {
        return project.property("mod_id") as String
    }

    /**
     * The given function is designed to pull maven credentials from environment variables with the
     * given key pattern of `${id}_maven_credentials` and accepting two types of JSON data format:
     *
     * - Parent: Allows for deferring credentials to another set of credentials. Code: `"parent_credentials":""`
     * - Base: Declaring the `url`, `user`, and `password` in standard JSON format allows for individual projects
     *   to declare credentials. Code: `"url":"","user":"","password":""`
     */
    fun setupMavenRepo(project: Project, repoHandler: RepositoryHandler) {
        setupMavenRepo(modId(project), repoHandler)
    }

    fun setupMavenRepo(id: String, repoHandler: RepositoryHandler) {
        val mavenCredentials = System.getenv()["${id}_maven_credentials"] ?: return
        val json = Json.decodeFromString<JsonObject>("{${mavenCredentials}}")

        fun getContent(obj: JsonObject, key: String): String? {
            val element = obj[key] ?: return null
            return (element as? JsonPrimitive ?: throw IllegalStateException("'$id' maven credentials entry has the incorrect type! '$key' is not a JsonPrimitive: $element")).content
        }

        fun getRequired(obj: JsonObject, key: String): String {
            return getContent(obj, key) ?: throw IllegalStateException("'${id}' maven credentials was missing '${key}' as its required!")
        }

        val parent = getContent(json, "parent_credentials")

        // Attempt to use parent project credentials instead of trying to use unique credentials for the project
        if (parent != null) return setupMavenRepo(parent, repoHandler)

        repoHandler.maven {
            url = URI.create(getRequired(json, "url"))

            credentials {
                username = getRequired(json, "user")
                password = getRequired(json, "password")
            }
        }
    }
}