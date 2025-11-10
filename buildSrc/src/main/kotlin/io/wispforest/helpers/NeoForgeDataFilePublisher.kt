/*
 * Copyright (C) 2025 Neoforged
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package io.wispforest.helpers

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

fun Project.neoForgeDataFile(action: Action<NeoForgeDataFilePublisher>) {
    NeoForgeDataFilePublisher.create(this, action)
}

/**
 * Code is based on ModDevGradle [DataFileCollections](https://github.com/neoforged/ModDevGradle/blob/48c9191456a41b52ac8b3e1446b5824cadfd701d/src/main/java/net/neoforged/moddevgradle/internal/DataFileCollections.java#L25)
 * class and has been modified to only be used for publishing.
 */
class NeoForgeDataFilePublisher(
    private val project: Project,
    private val accessTransformersPublishArtifactCallback: PublishCallback,
    private val interfaceInjectionDataPublishArtifactCallback: PublishCallback
) {

    fun publishAccessTransformersFile(filePath: String) {
        publishAccessTransformers(project.file(filePath))
    }

    fun publishInterfaceInjectionsFile(filePath: String) {
        publishInterfaceInjections(project.file(filePath))
    }

    fun publishAccessTransformers(artifactNotation: Any) {
        if (artifactNotation is String) {
            throw IllegalArgumentException("Can not publish a direct string as an AT, use 'publishAccessTransformersFile' instead or pass a direct file!")
        }

        accessTransformersPublishArtifactCallback.addFile(artifactNotation)
    }

    fun publishInterfaceInjections(artifactNotation: Any) {
        if (artifactNotation is String) {
            throw IllegalArgumentException("Can not publish a direct string as an InterfaceInjections, use 'publishInterfaceInjectionsFile' instead or pass a direct file!")
        }

        interfaceInjectionDataPublishArtifactCallback.addFile(artifactNotation)
    }

    companion object {
        const val CONFIGURATION_ACCESS_TRANSFORMERS: String = "accessTransformers"
        const val CONFIGURATION_INTERFACE_INJECTION_DATA: String = "interfaceInjectionData"

        private val dataFilePublishers: Map<String, NeoForgeDataFilePublisher> = mutableMapOf();

        fun create(project: Project, action: Action<NeoForgeDataFilePublisher>) {
            dataFilePublishers.getOrElse(project.path) {
                NeoForgeDataFilePublisher(
                    project,
                    PublishCallback(project, CONFIGURATION_ACCESS_TRANSFORMERS, "accesstransformer"),
                    PublishCallback(project, CONFIGURATION_INTERFACE_INJECTION_DATA, "interfaceinjection")
                )
            }.also(action::execute)
        }
    }
}

/**
 * Code is based on ModDevGradle [DataFileCollections.createCollection](https://github.com/neoforged/ModDevGradle/blob/48c9191456a41b52ac8b3e1446b5824cadfd701d/src/main/java/net/neoforged/moddevgradle/internal/DataFileCollections.java#L36)
 * method and has been modified just to handle the publishing of files to maven.
 */
class PublishCallback {
    private var project: Project
    private var name: String
    private var category: String

    private var firstArtifact: ConfigurablePublishArtifact? = null
    private var artifactCount: Int = 0

    private var elementsConfiguration: Configuration

    private var copyTaskName: String
    private var copyTask: TaskProvider<CopyDataFile>

    constructor(project: Project, name: String, category: String) {
        this.project = project;
        this.name = name;
        this.category = category;

        this.elementsConfiguration = project.configurations.create(name + "Elements") {
            this.description = "Published data files for $name"
            this.isCanBeConsumed = true
            this.isCanBeResolved = false
            this.attributes {
                this.attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    project.objects.named(Category.CATEGORY_ATTRIBUTE.getType(), category)
                )
            }
        }

        // Set up the variant publishing conditionally
        (project.components.getByName("java") as AdhocComponentWithVariants)
            .addVariantsFromConfiguration(elementsConfiguration){
                // This should be invoked lazily, so checking if the artifacts are empty is fine:
                // "The details object used to determine what to do with a configuration variant **when publishing**."
                if (this.configurationVariant.artifacts.isEmpty()) this.skip();
            }

        this.copyTaskName = "copy" + name.uppercaseFirstChar() + "Publications";
        this.copyTask = project.tasks.register(copyTaskName, CopyDataFile::class.java)
    }

    fun addFile(artifactNotation: Any) {
        // Create a temporary artifact to resolve file and task dependencies.
        val dummyArtifact = project.artifacts.add(elementsConfiguration.name, artifactNotation);

        val artifactFile = dummyArtifact.file;
        val artifactDependencies = dummyArtifact.buildDependencies;
        elementsConfiguration.artifacts.remove(dummyArtifact);

        val copyOutput = project.layout.buildDirectory.file(copyTaskName + "/" + artifactCount + "-" + artifactFile.getName());
        copyTask.configure {
            this.dependsOn(artifactDependencies);
            this.inputFiles.add(project.layout.file(project.provider { artifactFile }));
            this.outputFiles.add(copyOutput);
        }

        project.artifacts.add(elementsConfiguration.name, copyOutput) {
            this.builtBy(copyTask)

            if (firstArtifact == null) firstArtifact = this

            if (artifactCount == 1) {
                firstArtifact!!.classifier = category + artifactCount
            }

            artifactCount++

            this.classifier = category + (if(artifactCount > 1) artifactCount else "")
        }
    }
}

/**
 * 1:1 conversion of ModDevGradles [CopyDataFile.java](https://github.com/neoforged/ModDevGradle/blob/48c9191456a41b52ac8b3e1446b5824cadfd701d/src/main/java/net/neoforged/moddevgradle/tasks/CopyDataFile.java#L13)
 * with all credit and attribution going to such.
 */
abstract class CopyDataFile : DefaultTask() {
    @get:InputFiles
    abstract val inputFiles: ListProperty<RegularFile?>

    @get:OutputFiles
    abstract val outputFiles: ListProperty<RegularFile?>

    @TaskAction
    @Throws(IOException::class)
    fun doCopy() {
        val inputs = this.inputFiles.get()
        val outputs = this.outputFiles.get()
        if (inputs.size != outputs.size) throw RuntimeException("Lists length do not match.")

        for (i in 0 until inputs.size) {
            val outputFile = outputs[i]!!.asFile.toPath()

            Files.createDirectories(outputFile.parent)
            Files.copy(inputs[i]!!.asFile.toPath(), outputFile, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}