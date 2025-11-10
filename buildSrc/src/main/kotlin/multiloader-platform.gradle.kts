import io.wispforest.helpers.Extensions.currentPlatform
import io.wispforest.helpers.Extensions.currentPlatformDisplayName
import io.wispforest.helpers.Utils
import org.gradle.kotlin.dsl.get

plugins {
    id("multiloader-base")
    id("com.github.johnrengelman.shadow")
}

val common by configurations.creating
val shadowCommon by configurations.creating

configurations {
    common
    shadowCommon // Don't use shadow from the shadow plugin since it *excludes* files.
    "compileClasspath" { extendsFrom(common) }
    "runtimeClasspath" { extendsFrom(common) }
}

// Setup platforms Shadow Configs
dependencies {
    "common"(project(":common", "namedElements")) { this.isTransitive = false }
    "shadowCommon"(project(":common", "namedElements")) { this.isTransitive = false }
}

//-- Data Generation Setup Section

var generatedResources = file("src/generated/resources")

sourceSets {
    main {
        resources {
            srcDir(generatedResources)
            exclude(".cache/**")
        }
    }
}

val modid = Utils.modId(rootProject)

if (currentPlatform == "fabric") {
    // Use Fabric API to setup data generation
    fabricApi {
        configureDataGeneration {
            modId.set(modid)
            outputDirectory = generatedResources
            client = true
        }
    }
} else {
    // Create custom run that allows for Data Generation to work for Neoforge
    loom {
        runs {
            create("data-generation"){
                clientData()

                name("Data Generation")

                programArgs.addAll(
                    mutableListOf(
                        "--all", "--mod", modid, "--output", generatedResources.absolutePath
                    )
                )
            }
        }
    }
}

//-- Jar Handling Section

// Setup shadow to take files from common module
tasks.shadowJar {
    if (currentPlatform != "fabric") {
        exclude("fabric.mod.json")
    }
    exclude("architectury.common.json")

    configurations = mutableListOf<FileCollection>(project.configurations["shadowCommon"]);
    archiveClassifier.set("dev-shadow")
    destinationDirectory.set(destinationDirectory.get().dir("shadow"))
}

// Remap the shadow jar to the proper platform mapping
tasks.remapJar {
    inputFile.set(tasks.shadowJar.get().archiveFile)
    dependsOn(tasks.shadowJar)
    archiveClassifier.set("")

    if (currentPlatform == "fabric") {
        injectAccessWidener = true
    } else {
        //atAccessWideners.add("${modid}.accesswidener")
    }
}

// Add Common files to Source
tasks.getByName("sourcesJar", AbstractArchiveTask::class) {
    val commonSources = project(":common").tasks.getByName("sourcesJar", AbstractArchiveTask::class)
    dependsOn(commonSources)
    from(commonSources.archiveFile.map { zipTree(it) })
}

with(components["java"] as AdhocComponentWithVariants) {
    withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) { skip() }
}