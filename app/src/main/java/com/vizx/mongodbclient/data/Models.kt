package com.vizx.mongodbclient.data

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val uri: String) : ConnectionState
    data class Connected(
        val uri: String,
        val serverVersion: String,
        val databases: List<String>,
        val pingMs: Long,
        val clusterInfo: ClusterInfo? = null
    ) : ConnectionState
    data class Error(val message: String, val details: String? = null) : ConnectionState
}

data class NetworkConfig(
    val connectTimeoutSeconds: Int = 15,
    val readTimeoutSeconds: Int = 20,
    val serverSelectionTimeoutSeconds: Int = 15,
    val maxPoolSize: Int = 20,
    val minPoolSize: Int = 0,
    val allowInvalidHostnames: Boolean = false
)

data class ServerStatusMetrics(
    val currentConnections: Long = 0,
    val availableConnections: Long = 0,
    val totalCreatedConnections: Long = 0,
    val uptimeSeconds: Long = 0,
    val residentMemoryMb: Long = 0,
    val virtualMemoryMb: Long = 0,
    val opcountersInsert: Long = 0,
    val opcountersQuery: Long = 0,
    val opcountersUpdate: Long = 0,
    val opcountersDelete: Long = 0,
    val opcountersCommand: Long = 0,
    val networkBytesInFormatted: String = "0 B",
    val networkBytesOutFormatted: String = "0 B"
)

data class ActiveOperation(
    val opId: Long = 0,
    val ns: String = "",
    val op: String = "",
    val secsRunning: Long = 0,
    val queryJson: String = "{}"
)

data class ClusterInfo(
    val serverVersion: String,
    val clusterType: String,
    val pingMs: Long,
    val hosts: List<String> = emptyList(),
    val connectionMode: String = "ReplicaSet",
    val maxBsonObjectSizeMb: Double = 16.0
)

data class DatabaseStats(
    val dbName: String,
    val collectionsCount: Int = 0,
    val objectsCount: Long = 0,
    val avgObjSize: Double = 0.0,
    val dataSizeFormatted: String = "0 B",
    val storageSizeFormatted: String = "0 B",
    val indexesCount: Int = 0,
    val indexSizeFormatted: String = "0 B"
)

data class CollectionSummary(
    val name: String,
    val documentCount: Long = 0
)

data class SavedConnection(
    val id: String,
    val name: String,
    val uri: String,
    val lastConnected: Long = System.currentTimeMillis()
)

data class IndexSummary(
    val name: String,
    val keys: String,
    val isUnique: Boolean = false
)

enum class MongoOperation(val label: String) {
    FIND("Find / Query"),
    INSERT("Insert"),
    UPDATE("Update"),
    DELETE("Delete"),
    AGGREGATE("Aggregate Pipeline"),
    COUNT("Count Documents")
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

enum class AppScreen {
    LOGIN,
    HOME
}
