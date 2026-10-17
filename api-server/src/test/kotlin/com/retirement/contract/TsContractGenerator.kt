package com.retirement.contract

import com.retirement.api.BreakdownRequest
import com.retirement.api.ErrorResponse
import com.retirement.api.ValidationErrorResponse
import com.retirement.model.YearBreakdown
import com.retirement.model.MarketPreset
import com.retirement.model.MonteCarloRequest
import com.retirement.model.MonteCarloResult
import com.retirement.model.MonteCarloRunDetail
import com.retirement.model.MonteCarloRunBreakdownRequest
import com.retirement.model.MonteCarloRunRequest
import com.retirement.model.SaveSimulationRequest
import com.retirement.model.SavedSimulation
import com.retirement.model.SavedSimulationSummary
import com.retirement.model.SimulationConfig
import com.retirement.model.SimulationResult
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.serializer
import java.io.File
import kotlin.reflect.full.createType

/**
 * Generates the TypeScript API contract (frontend + BFF) from the Kotlin @Serializable models,
 * so the three tiers cannot drift (Constitution III).
 *
 * Wire-format rules (match Ktor's default Json: encodeDefaults = true, explicit nulls, "type" discriminator):
 * - Nullable Kotlin field -> `T | null`
 * - Field with a Kotlin default -> optional (`?`) only in types reachable from a request, since the server
 *   accepts it missing; responses always include every field, so response-only types keep them required
 * - Sealed hierarchy -> union of interfaces, each with its `type` discriminator literal
 *
 * Run `./gradlew generateTsContract` after changing a model; ContractTypesTest fails until you do.
 */
@OptIn(ExperimentalSerializationApi::class)
object TsContractGenerator {

    private val requestRoots = listOf(
        SimulationConfig.serializer().descriptor,
        BreakdownRequest.serializer().descriptor,
        MonteCarloRequest.serializer().descriptor,
        MonteCarloRunRequest.serializer().descriptor,
        MonteCarloRunBreakdownRequest.serializer().descriptor
    )

    private val responseRoots = listOf(
        SimulationResult.serializer().descriptor,
        YearBreakdown.serializer().descriptor,
        ValidationErrorResponse.serializer().descriptor,
        ErrorResponse.serializer().descriptor,
        MarketPreset.serializer().descriptor,
        MonteCarloResult.serializer().descriptor,
        MonteCarloRunDetail.serializer().descriptor,
        SavedSimulationSummary.serializer().descriptor,
        SavedSimulation.serializer().descriptor,
        // Sent in a request, but it carries a SimulationResult exactly as the server returned it,
        // so its types keep every field required
        SaveSimulationRequest.serializer().descriptor
    )

    /** Output paths, relative to the api-server project directory. */
    val outputFiles = listOf(
        File("../frontend/src/types/contract.generated.ts"),
        File("../backend/src/contract.generated.ts")
    )

    const val REGENERATE_COMMAND = "cd api-server && ./gradlew generateTsContract"

    fun generate(): String {
        val names = mutableMapOf<String, String>()          // serialName -> TS name (sealed subclasses)
        val discriminators = mutableMapOf<String, String>() // serialName -> discriminator value

        val requestTypes = LinkedHashMap<String, SerialDescriptor>()
        requestRoots.forEach { collect(it, requestTypes, names, discriminators) }

        val allTypes = LinkedHashMap<String, SerialDescriptor>()
        (requestRoots + responseRoots).forEach { collect(it, allTypes, names, discriminators) }

        val body = allTypes.values.joinToString("\n\n") { d ->
            render(d, d.baseName() in requestTypes, names, discriminators)
        }

        return buildString {
            appendLine("// GENERATED FILE - DO NOT EDIT.")
            appendLine("// Source: Kotlin @Serializable models in api-server (see TsContractGenerator).")
            appendLine("// Regenerate: $REGENERATE_COMMAND")
            appendLine("// Optional (?) fields have server-side defaults and may be omitted in requests.")
            appendLine()
            appendLine(body)
        }
    }

    private fun SerialDescriptor.baseName() = serialName.removeSuffix("?")

    private fun tsName(d: SerialDescriptor, names: Map<String, String>) =
        names[d.baseName()] ?: d.baseName().substringAfterLast('.')

    private fun collect(
        d: SerialDescriptor,
        into: LinkedHashMap<String, SerialDescriptor>,
        names: MutableMap<String, String>,
        discriminators: MutableMap<String, String>
    ) {
        when (d.kind) {
            StructureKind.CLASS, StructureKind.OBJECT -> {
                if (into.putIfAbsent(d.baseName(), d) != null) return
                d.elementDescriptors.forEach { collect(it, into, names, discriminators) }
            }
            PolymorphicKind.SEALED -> {
                if (into.putIfAbsent(d.baseName(), d) != null) return
                registerSealedSubclasses(d, names, discriminators)
                sealedSubclasses(d).forEach { collect(it, into, names, discriminators) }
            }
            StructureKind.LIST -> collect(d.getElementDescriptor(0), into, names, discriminators)
            StructureKind.MAP -> collect(d.getElementDescriptor(1), into, names, discriminators)
            SerialKind.ENUM -> into.putIfAbsent(d.baseName(), d)
            else -> Unit
        }
    }

    private fun sealedSubclasses(d: SerialDescriptor): List<SerialDescriptor> =
        d.getElementDescriptor(1).elementDescriptors.toList()

    /** Subclasses use @SerialName (e.g. "CASH"), so map them back to class names via reflection. */
    private fun registerSealedSubclasses(
        d: SerialDescriptor,
        names: MutableMap<String, String>,
        discriminators: MutableMap<String, String>
    ) {
        val sealedClass = Class.forName(d.baseName()).kotlin
        sealedClass.sealedSubclasses.forEach { sub ->
            val serialName = serializer(sub.createType()).descriptor.serialName
            val qualified = sub.qualifiedName ?: error("Anonymous sealed subclass of ${d.serialName}")
            names[serialName] = qualified.removePrefix(sub.java.packageName + ".").replace(".", "")
            discriminators[serialName] = serialName
        }
    }

    private fun render(
        d: SerialDescriptor,
        isRequestType: Boolean,
        names: Map<String, String>,
        discriminators: Map<String, String>
    ): String {
        val name = tsName(d, names)
        return when (d.kind) {
            SerialKind.ENUM ->
                "export type $name = ${(0 until d.elementsCount).joinToString(" | ") { "'${d.getElementName(it)}'" }};"
            PolymorphicKind.SEALED ->
                "export type $name = ${sealedSubclasses(d).joinToString(" | ") { tsName(it, names) }};"
            else -> buildString {
                appendLine("export interface $name {")
                discriminators[d.baseName()]?.let { appendLine("  type: '$it';") }
                for (i in 0 until d.elementsCount) {
                    val optional = if (isRequestType && d.isElementOptional(i)) "?" else ""
                    appendLine("  ${d.getElementName(i)}$optional: ${tsType(d.getElementDescriptor(i), names)};")
                }
                append("}")
            }
        }
    }

    private fun tsType(d: SerialDescriptor, names: Map<String, String>): String {
        val base = when (d.kind) {
            PrimitiveKind.STRING, PrimitiveKind.CHAR -> "string"
            PrimitiveKind.BOOLEAN -> "boolean"
            PrimitiveKind.INT, PrimitiveKind.LONG, PrimitiveKind.SHORT, PrimitiveKind.BYTE,
            PrimitiveKind.DOUBLE, PrimitiveKind.FLOAT -> "number"
            StructureKind.LIST -> tsType(d.getElementDescriptor(0), names).let { if (' ' in it) "($it)[]" else "$it[]" }
            StructureKind.MAP -> "Record<${tsType(d.getElementDescriptor(0), names)}, ${tsType(d.getElementDescriptor(1), names)}>"
            else -> tsName(d, names)
        }
        return if (d.isNullable) "$base | null" else base
    }
}

/** Entry point for `./gradlew generateTsContract`. */
fun main() {
    val content = TsContractGenerator.generate()
    TsContractGenerator.outputFiles.forEach { file ->
        file.writeText(content)
        println("Wrote ${file.canonicalPath}")
    }
}
