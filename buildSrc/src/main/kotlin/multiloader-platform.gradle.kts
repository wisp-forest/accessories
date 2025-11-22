import io.wispforest.helpers.Extensions.currentPlatform
import io.wispforest.helpers.Extensions.modId
import net.fabricmc.loom.task.service.MixinRefmapService

plugins {
    id("multiloader-base")
    id("com.gradleup.shadow")
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

val modid = rootProject.modId

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

shadow {
    addShadowVariantIntoJavaComponent = false;
}

// Setup shadow to take files from common module
tasks.shadowJar {
    configurations.set(mutableSetOf<Configuration>(project.configurations["shadowCommon"]));
    archiveClassifier.set("dev-shadow")
    destinationDirectory.set(destinationDirectory.get().dir("shadow"))

    exclude("architectury.common.json")

//    if (currentPlatform == "neoforge") { // Neoforge does not require the refmap, we remove the file and the line within the mixin json
//        exclude("${modId}-common-common-refmap.json")
//
//        filesMatching("${modId}-common.mixins.json") {
//            filter(LineContains::class, "negate" to true, "contains" to listOf("refmap"))
//        }
//    } else if (currentPlatform == "fabric") { // Fabric requires the refmap but only in prod so we have the field get filled in here
//        val commonExtraProperties = mutableMapOf("mod_id" to modid)
//
//        filesMatching(listOf("${modId}-common.mixins.json")) {
//            filter(ResourceProcessingUtils.expandIgnoreErrors(commonExtraProperties) { true })
//        }
//    }
}

// Remap the shadow jar to the proper platform mapping
tasks.remapJar {
    inputFile.set(tasks.shadowJar.get().archiveFile)
    dependsOn(tasks.shadowJar)
    archiveClassifier.set("")
    if (currentPlatform == "fabric") injectAccessWidener = true

    var commonProject = project(":common");

    var commonRemapJarTask = commonProject.tasks.remapJar.get();

    this.mixinRefmapServiceOptions.addAll(MixinRefmapService.createOptions(commonRemapJarTask))
}

// Add Common files to Source
tasks.getByName("sourcesJar", AbstractArchiveTask::class) {
    val commonSources = project(":common").tasks.getByName("sourcesJar", AbstractArchiveTask::class)
    dependsOn(commonSources)
    from(commonSources.archiveFile.map { zipTree(it) })
}