import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace
import net.fabricmc.loom.task.AbstractRemapJarTask
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask

plugins {
    id("multiloader-base")
    id("multiloader-publishing")
}

val mojmapJar = tasks.register<RemapJarTask>("mojmapJar") {
    setupTask(this, "remapJar", "")
}

val mojmapSourcesJar = tasks.register<RemapSourcesJarTask>("mojmapSourcesJar") {
    setupTask(this, "remapSourcesJar", "sources")
}

fun setupTask(targetTask: AbstractRemapJarTask, taskName: String, archiveClassifier: String) {
    targetTask.classpath.from((project(":common").loom as LoomGradleExtension).getMinecraftJarsCollection(MappingsNamespace.INTERMEDIARY))

    val baseJarTask = project(":common").tasks.named<AbstractRemapJarTask>(taskName)

    targetTask.dependsOn(baseJarTask)

    if (archiveClassifier.isNotEmpty()) targetTask.archiveClassifier = archiveClassifier

    targetTask.inputFile.convention(baseJarTask.get().inputFile)

    targetTask.sourceNamespace = "named"
    targetTask.targetNamespace = "named"

    //targetTask.remapperIsolation = true

    targetTask.mustRunAfter(
        tasks.named("generateMetadataFileForMavenCommonPublication"),
        tasks.named("generateMetadataFileForMavenMojmapPublication"),
        tasks.named("publishMavenCommonPublicationToMavenLocal"),
        tasks.named("publishMavenCommonPublicationToMavenRepository"),
        //tasks.named("publishMavenMojmapPublicationToMavenLocal")
    )
}

tasks.named("build").configure {
    dependsOn(mojmapJar, mojmapSourcesJar)
}