package io.wispforest.helpers

import groovy.lang.MissingPropertyException
import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import io.wispforest.helpers.Extensions.currentPlatform
import io.wispforest.helpers.Extensions.modId
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.UncheckedIOException
import org.gradle.api.provider.Provider
import java.io.IOException
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter

object ResourceProcessingUtils {
    //-- Fabric FMJ Entry Utils
    const val REMOVE_LINE_TARGET_START = "#REMOVE_LINE_START#"
    const val REMOVE_LINE_TARGET_END = "#REMOVE_LINE_END#"
    const val INDENTATION = "  "

    private fun indentation(level: Int): String {
        return INDENTATION.repeat(level);
    }

    const val ENTRY_SEPERATOR = ",\n"

    fun buildReplacementEntry(isMap: Boolean, indentationLevel: Int, builder: () -> String): String {
        var entry = "$REMOVE_LINE_TARGET_START\"${if (isMap) ": \"\"" else ""}$ENTRY_SEPERATOR"

        entry += builder().prependIndent(indentation(indentationLevel));

        entry += "\"$REMOVE_LINE_TARGET_END"

        return entry;
    }

    /*
     * // BEFORE
     * "contacts": [
     *    "{unpack_key_here}"
     * ]
     *
     * // AFTER
     * "contacts": [
     *    "#REMOVE_LINE_START#",
     *    "test1",
     *    {
     *       "name": ""test2""
     *    }
     *    "#REMOVE_LINE_END#"
     * ]
     */
    fun buildContactListEntry(project: Project, keys: List<String>): String {
        val rootProject = project.rootProject;

        return buildReplacementEntry(false, 2) {
            var entry = ""

            keys.forEachIndexed { i: Int, key: String ->
                if (key.isBlank()) return@forEachIndexed

                val contactKey = key.lowercase().replace(" ", "_");

                if (rootProject.hasProperty(contactKey)) {
                    entry +=
                        """{
  "name": "$key",
  "contact": { 
    "homepage": "${rootProject.property(contactKey)}"
  }
}"""
                } else {
                    entry += "\"$key\"";
                }

                entry += if (i < keys.size - 1) ENTRY_SEPERATOR else "\n";
            }

            return@buildReplacementEntry entry;
        }
    }

    /*
     * // BEFORE
     * "{unpack_key_here}": ""
     *
     * // AFTER
     * "#REMOVE_LINE_START#": ""
     * "details": [
     *     "test1",
     *     "test2"
     * ],
     * "#REMOVE_LINE_END#": ""
     */
    fun buildProjectContactMapEntry(project: Project, vararg keys: String): String {
        val rootProject = project.rootProject

        return buildReplacementEntry(true, 2) {
            var entry = ""

            keys.forEachIndexed { i, key ->
                val propertyValue = rootProject.property("mod_$key")

                entry += if (propertyValue is String && propertyValue.isNotBlank()) "\"$key\": \"$propertyValue\"" else ""

                entry += if (i < keys.size - 1) ENTRY_SEPERATOR else "\n";
            }

            return@buildReplacementEntry entry;
        }
    }

    fun expandIgnoreErrors(properties: MutableMap<String, String>, escapeBackslash: () -> Boolean) : Transformer<String?, String> {
        return object : Transformer<String?, String> {
            override fun transform(original: String): String {
                try {
                    var template: Template

                    val engine = SimpleTemplateEngine()
                    engine.isEscapeBackslash = escapeBackslash()
                    template = engine.createTemplate(original)

                    val writer = StringWriter()
                    // SimpleTemplateEngine expects to be able to mutate the map internally.
                    template.make(LinkedHashMap<String?, String?>(properties)).writeTo(writer)

                    return writer.toString()
                } catch (e: IOException) {
                    throw UncheckedIOException(e)
                } catch (_: MissingPropertyException) { }

                return original;
            }
        }
    }
}