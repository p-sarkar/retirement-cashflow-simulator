package com.retirement.data

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Simulations : UUIDTable() {
    val name = varchar("name", 255)
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val configuration = text("configuration") // JSON blob of SimulationConfig
    val result = text("result") // JSON blob of SimulationResult
}
