package com.retirement.logic

import com.retirement.model.InputsFile
import com.retirement.model.SimulationConfig
import com.retirement.util.ServerInfo
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** What an imported file turned out to be (FR005-014 to FR005-016). */
sealed interface InputsImport {
    data class Valid(val config: SimulationConfig) : InputsImport
    data class Invalid(val errors: List<ValidationError>) : InputsImport
    data class NotAnInputsFile(val reason: String) : InputsImport
}

/** Writes and reads the inputs file (US005-3, research R005-9). */
object InputsFiles {
    const val FORMAT = "retirement-simulator-inputs"
    const val FORMAT_VERSION = 1

    // As for saved simulations: inputs added since the file was written take their defaults (FR005-017)
    private val json = Json { ignoreUnknownKeys = true }

    fun export(config: SimulationConfig) = InputsFile(FORMAT, FORMAT_VERSION, ServerInfo.version, config)

    /** Reads a file's text: its inputs when they are valid, every invalid field otherwise, or why it isn't a file. */
    fun read(text: String): InputsImport {
        val file = try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (e: SerializationException) {
            null
        } ?: return InputsImport.NotAnInputsFile("The file isn't JSON")

        val format = runCatching { file["format"]?.jsonPrimitive?.content }.getOrNull()
        if (format != FORMAT) return InputsImport.NotAnInputsFile("The file isn't a simulation inputs file")
        val version = runCatching { file["formatVersion"]?.jsonPrimitive?.intOrNull }.getOrNull()
            ?: return InputsImport.NotAnInputsFile("The file has no format version")
        if (version > FORMAT_VERSION) {
            return InputsImport.NotAnInputsFile("The file was written by a newer version of the app (format $version)")
        }

        val configJson = file["config"] ?: return InputsImport.NotAnInputsFile("The file has no inputs")
        val config = try {
            json.decodeFromJsonElement(SimulationConfig.serializer(), configJson)
        } catch (e: IllegalArgumentException) { // SerializationException is one: missing or mistyped inputs
            return InputsImport.NotAnInputsFile("The file's inputs can't be read: ${e.message?.lineSequence()?.first()}")
        }

        val errors = ConfigValidator.validate(config).errors
        return if (errors.isEmpty()) InputsImport.Valid(config) else InputsImport.Invalid(errors)
    }
}
