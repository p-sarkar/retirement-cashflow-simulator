package com.retirement.data

import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

object Simulations : UUIDTable() {
    val name = varchar("name", 255)
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val updatedAt = datetime("updated_at").nullable() // When inputs and results were last replaced (FR005-007)
    val configuration = text("configuration") // JSON blob of SimulationConfig
    val result = text("result") // JSON blob of SimulationResult
}
