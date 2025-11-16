import io.wispforest.helpers.Extensions.currentPlatform
import io.wispforest.helpers.Extensions.libs
import io.wispforest.helpers.Extensions.modId
import io.wispforest.helpers.ItemViewerUtils.setupItemViewerDependencies
import io.wispforest.helpers.ResourceProcessingUtils
import io.wispforest.helpers.RunConfigurationUtils.createExtraRunConfigs
import io.wispforest.helpers.UtilsJava

plugins {
    id("dev.architectury.loom")
    id("maven-publish")
    id("base")
    id("java")
    id("java-library")
}

val modid = project.modId

base {
    archivesName = "${modid}${(if(project.name.isEmpty()) "" else "-${project.name.replace("-mojmap", "")}")}"
}

version = "${project.property("mod_version")}+${libs.versions.minecraft.base.get()}${(if(project.name.contains("mojmap")) "-mojmap" else "")}"
group = rootProject.property("mod_group")!!

val projectPlatform: String = currentPlatform;
val enabledTestmodPlatforms = (rootProject.property("enabled_testmod_platforms") as String).split(",");

if (projectPlatform != "common" && enabledTestmodPlatforms.contains(projectPlatform)) {
    sourceSets {
        create("testmod") {
            runtimeClasspath += sourceSets["main"].runtimeClasspath
            compileClasspath += sourceSets["main"].compileClasspath
        }
    }
}

loom {
    silentMojangMappingsLicense()

    if (project.path == ":common") {
        val awPath = "src/main/resources/${modid}.accesswidener"
        val awFile = file(awPath);

        if (!awFile.exists()) {
            throw IllegalStateException("Unable to locate the given AccessWidener File at the given path: $awPath")
        }

        accessWidenerPath = awFile;

        return@loom
    }

    mods {
        try {
            named("main") {
                this@named.sourceSet("main", project)
                this@named.sourceSet("main", project(":common"))
            }
        } catch (e: UnknownDomainObjectException) {
            register("main") {
                this@register.sourceSet("main", project)
                this@register.sourceSet("main", project(":common"))
            }
        }
    }

    accessWidenerPath = project(":common").loom.accessWidenerPath

    runs {
        this.createExtraRunConfigs(project)
    }

    if (currentPlatform == "neoforge") {
        println("Setup for Neoforge")
        neoForge {}
    } else if(currentPlatform == "forge") {
        println("Setup for Forge")
        forge {}
    }
}

repositories {
    // Platform Mavens
    maven("https://maven.parchmentmc.org")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases/")

    // Fabric API Event Lib
    maven("https://maven.su5ed.dev/releases")

    // oωo (owo-lib) and Endec Lib
    maven("https://maven.wispforest.io/releases")

    // REI Item Viewer
    maven("https://maven.shedaniel.me/")
    maven("https://maven.architectury.dev/")

    // EMI Item Viewer
    maven("https://maven.terraformersmc.com/releases")

    // Modrinth Maven
    maven("https://api.modrinth.com/maven")

    // JEI Item Viewer
    maven("https://maven.blamejared.com/")
    maven("https://modmaven.dev") // Backup

    // Trinkets
    maven("https://maven.ladysnake.org/releases")

    // Curios
    maven("https://maven.theillusivec4.top/")

    mavenCentral()
    gradlePluginPortal()
}

//--

dependencies {
    minecraft("com.mojang:minecraft:${libs.versions.minecraft.asProvider().get()}")

    if (name == "common-mojmap") {
        mappings(loom.officialMojangMappings())
    } else {
        mappings (
            loom.layered {
                this.officialMojangMappings()
                this.parchment("org.parchmentmc.data:parchment-${libs.versions.minecraft.asProvider().get()}:${libs.versions.parchment.get()}@zip")
            }
        )
    }

    if (projectPlatform != "common" && enabledTestmodPlatforms.contains(projectPlatform)) {
        "testmodImplementation"(sourceSets.main.get().output)
    }

    // General Libs
    var owolibDependency = if (projectPlatform == "neoforge") libs.owolib.neo else libs.owolib.fabric

    modImplementation(owolibDependency) {
        if (projectPlatform == "common") exclude("net.fabricmc.fabric-api")
    }
    annotationProcessor(owolibDependency) {
        if (projectPlatform == "common") exclude("net.fabricmc.fabric-api")
    }

    implementation(libs.endec)
    implementation(libs.endec.netty)
    implementation(libs.endec.gson)
    implementation(libs.endec.jankson)
    //--

    // Item Viewer Libs
    project.setupItemViewerDependencies(modCompileOnly = this::modCompileOnly, modLocalRuntime = this::modLocalRuntime)
}

tasks.processResources {
    val baseProperties = mutableMapOf(
        "neoforge_mod_id"                    to modid.replace("-", "_"),
        "mod_id"                             to modid,
        "mod_name"                           to rootProject.property("mod_name"),
        "mod_version"                        to rootProject.property("mod_version"),
        "mod_license"                        to rootProject.property("mod_license"),
        "mod_credits"                        to rootProject.property("mod_credits"),
        "mod_authors"                        to rootProject.property("mod_authors"),
        "mod_contributors"                   to rootProject.property("mod_contributors"),
        "mod_group"                          to rootProject.property("mod_group"),
        "mod_description"                    to rootProject.property("mod_description"),
        "mod_issuepage"                      to rootProject.property("mod_issuepage"),
        "mod_sourcepage"                     to rootProject.property("mod_sourcepage"),
        "minecraft_version"                  to libs.versions.minecraft.asProvider().get(),
        "fabric_minecraft_version_range"     to libs.versions.fabric.minecraft.range.get(),
        "fabric_api_version"                 to libs.versions.fabric.api.asProvider().get(),
        "fabric_api_version_range"           to libs.versions.fabric.api.range.get(),
        "fabric_loader_version"              to libs.versions.fabric.loader.asProvider().get(),
        "fabric_loader_version_range"        to libs.versions.fabric.loader.range.get(),
        "owo_fabric_version"                 to libs.versions.owo.fabric.asProvider().get(),
        "owo_fabric_version_range"           to libs.versions.owo.fabric.range.get(),
        "neoforge_minecraft_version_range"   to libs.versions.neoforge.minecraft.range.get(),
        "neoforge_version"                   to libs.versions.neoforge.asProvider().get(),
        "neoforge_version_range"             to libs.versions.neoforge.range.get(),
        "neoforge_loader_version_range"      to libs.versions.neoforge.loader.range.get(),
        "owo_neoforge_version"               to libs.versions.owo.neoforge.asProvider().get(),
        "owo_neoforge_version_range"         to libs.versions.owo.neoforge.range.get(),
        "java_version"                       to libs.versions.java.get()
    )

    filesMatching(listOf("pack.mcmeta", "META-INF/neoforge.mods.toml")) {
        expand(baseProperties)
    }

    // Fabric: More Info about contacts like links
    val fabricExtraProperties = baseProperties.toMutableMap();

    fabricExtraProperties["contact_entry"] = ResourceProcessingUtils.buildProjectContactMapEntry(project, "homepage", "sourcepage", "issuepage")

    fabricExtraProperties["list_of_authors"] = ResourceProcessingUtils.buildContactListEntry(project, (rootProject.property("mod_authors") as String).split(","))
    fabricExtraProperties["list_of_contributors"] = ResourceProcessingUtils.buildContactListEntry(project, (rootProject.property("mod_contributors") as String).split(","))

    filesMatching(listOf("fabric.mod.json")) {
        expand(fabricExtraProperties)

        // Note: Remove various entries that are there due to inability to properly add string data without breaking FMJ before looms reading
        // Note: Stupid Cast to remove error that is wrong
        filter(UtilsJava.removeLineTransformer())
    }
    // --

    inputs.properties(baseProperties)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = Integer.parseInt(libs.versions.java.get())
}

java {
    withSourcesJar()
}