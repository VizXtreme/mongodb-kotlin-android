package com.vizx.mongodbclient.data

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val uri: String) : ConnectionState
    data class Connected(
        val uri: String,
        val serverVersion: String,
        val databases: List<String>,
        val pingMs: Long
    ) : ConnectionState
    data class Error(val message: String, val details: String? = null) : ConnectionState
}

enum class MongoOperation(val label: String) {
    FIND("Find / Query"),
    INSERT("Insert"),
    UPDATE("Update"),
    DELETE("Delete")
}

enum class LogLevel {
    INFO, SUCCESS, WARN, ERROR
}

data class LogEntry(
    val timestamp: String,
    val message: String,
    val level: LogLevel = LogLevel.INFO
)

data class QueryResult(
    val documents: List<String> = emptyList(),
    val totalCount: Long = 0,
    val executionTimeMs: Long = 0,
    val message: String = ""
)
