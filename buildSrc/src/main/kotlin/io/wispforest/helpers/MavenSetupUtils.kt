package io.wispforest.helpers

import io.wispforest.helpers.Extensions.modId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import java.net.URI

object MavenSetupUtils {
    /**
     * The given function is designed to pull maven credentials from environment variables with the
     * given key pattern of `${id}_maven_credentials` and accepting two types of JSON data format:
     *
     * - Parent: Allows for deferring credentials to another set of credentials. Code: `"parent_credentials":""`
     * - Base: Declaring the `url`, `user`, and `password` in standard JSON format allows for individual projects
     *   to declare credentials. Code: `"url":"","user":"","password":""`
     */
    fun setupMavenRepo(project: Project, repoHandler: RepositoryHandler) {
        setupMavenRepo(project.modId, repoHandler)
    }

    fun setupMavenRepo(id: String, repoHandler: RepositoryHandler) {
        val env = System.getenv();
        val mavenCredentials = env["${id}_maven_credentials"] ?: env["${id}-maven-credentials"] ?: return
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