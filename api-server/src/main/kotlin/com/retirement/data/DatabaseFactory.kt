package com.retirement.data

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    const val DEFAULT_JDBC_URL = "jdbc:sqlite:./retirement.db"

    /** Connect to SQLite and create missing tables. Tests pass a temporary database file. */
    fun init(jdbcUrl: String = DEFAULT_JDBC_URL) {
        val database = Database.connect(jdbcUrl, "org.sqlite.JDBC")

        transaction(database) {
            SchemaUtils.create(Simulations)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
