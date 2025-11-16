import io.wispforest.helpers.Extensions.libs
import io.wispforest.helpers.Extensions.modId
import io.wispforest.helpers.MavenSetupUtils

plugins {
    id("java-library")
    id("maven-publish")
}

/**
 * Handles the ability to publish either the common, neoforge, or fabric module. Common also publishes one in Mojang Mappings
 * and sets up the maven credentials within [MavenSetupUtils.setupMavenRepo]
 */
publishing {
    var modid = rootProject.modId

    publications {
        create<MavenPublication>("mavenCommon") {
            val name = project.name
            artifactId = "${modid}${(if(name.isEmpty()) "" else "-${name.replace("-mojmap", "")}")}"
            afterEvaluate {
                this@create.from(components["java"])
            }
        }

        if (project.name == "common") {
            create<MavenPublication>("mavenMojmap") {
                val name = project.name

                version = "${rootProject.property("mod_version")}+${libs.versions.minecraft.asProvider().get()}-mojmap"
                artifactId = "${modid}-${name}"

                afterEvaluate {
                    this@create.from(components["java"])

                    this@create.setArtifacts(emptyList<Any>())

                    val mojmapJarTask = project.tasks.named("mojmapJar");
                    artifact(mojmapJarTask) {
                        builtBy(mojmapJarTask)
                        classifier = ""
                    }
                    val mojmapSourcesJarTask = project.tasks.named("mojmapSourcesJar");
                    artifact(mojmapSourcesJarTask) {
                        builtBy(mojmapSourcesJarTask)
                        classifier = "sources"
                    }
                }
            }
        }
    }

    MavenSetupUtils.setupMavenRepo(rootProject, repositories)
}