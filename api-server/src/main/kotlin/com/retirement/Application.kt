package com.retirement

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.retirement.plugins.*

fun main() {
    embeddedServer(Netty, port = 8090, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    com.retirement.data.DatabaseFactory.init()
    com.retirement.data.HistoricalDataService.loadData()
    configureHTTP()
    configureSerialization()
    configureRouting()
}
