package com.retirement.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {
    const val DEFAULT_JDBC_URL = "jdbc:sqlite:./retirement.db"

    /** Connect to SQLite and create missing tables. Tests pass a temporary database file. */
    fun init(jdbcUrl: String = DEFAULT_JDBC_URL) {
        val database = Database.connect(jdbcUrl, "org.sqlite.JDBC")

        transaction(database) {
            SchemaUtils.create(Simulations)
            addUpdatedAtIfMissing()
        }
    }

    /**
     * Databases saved before spec 005 have no updated_at column: add it, nullable, so existing rows read as never
     * updated (R005-6). Only this one column is added; nothing else in the schema or the rows changes.
     */
    private fun JdbcTransaction.addUpdatedAtIfMissing() {
        val columns = exec("PRAGMA table_info(${Simulations.tableName})") { rs ->
            generateSequence { if (rs.next()) rs.getString("name") else null }.toList()
        }.orEmpty()
        if (Simulations.updatedAt.name !in columns) {
            exec("ALTER TABLE ${Simulations.tableName} ADD COLUMN ${Simulations.updatedAt.name} TEXT NULL")
        }
    }

    // Exposed 1.x: suspendTransaction takes no coroutine context, so the IO dispatcher comes from withContext
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        withContext(Dispatchers.IO) { suspendTransaction { block() } }
}
