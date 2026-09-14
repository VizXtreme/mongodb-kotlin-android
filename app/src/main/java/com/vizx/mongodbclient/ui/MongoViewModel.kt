package com.vizx.mongodbclient.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vizx.mongodbclient.data.ConnectionState
import com.vizx.mongodbclient.data.LogEntry
import com.vizx.mongodbclient.data.LogLevel
import com.vizx.mongodbclient.data.MongoManager
import com.vizx.mongodbclient.data.MongoOperation
import com.vizx.mongodbclient.data.QueryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MongoUiState(
    val uri: String = "mongodb+srv://username:password@cluster0.ywgy3ll.mongodb.net/?appName=Cluster0",
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val databases: List<String> = emptyList(),
    val selectedDatabase: String = "",
    val collections: List<String> = emptyList(),
    val selectedCollection: String = "",
    val activeOperation: MongoOperation = MongoOperation.FIND,
    val filterJson: String = "{}",
    val insertJson: String = "{\n  \"name\": \"Sample Document\",\n  \"status\": \"active\",\n  \"createdAt\": \"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\"\n}",
    val updateJson: String = "{\n  \"status\": \"updated\",\n  \"version\": 2\n}",
    val isMultiple: Boolean = false,
    val queryResult: QueryResult = QueryResult(),
    val isLoading: Boolean = false,
    val logs: List<LogEntry> = emptyList()
)

class MongoViewModel(
    private val mongoManager: MongoManager = MongoManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MongoUiState())
    val uiState: StateFlow<MongoUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    init {
        log("App initialized. Ready to connect to MongoDB.", LogLevel.INFO)
    }

    fun onUriChange(newUri: String) {
        _uiState.update { it.copy(uri = newUri) }
    }

    fun onDatabaseSelected(dbName: String) {
        _uiState.update { it.copy(selectedDatabase = dbName, selectedCollection = "", collections = emptyList()) }
        loadCollections(dbName)
    }

    fun onCollectionSelected(collName: String) {
        _uiState.update { it.copy(selectedCollection = collName) }
        log("Selected collection: '${_uiState.value.selectedDatabase}.$collName'", LogLevel.INFO)
    }

    fun onOperationSelected(op: MongoOperation) {
        _uiState.update { it.copy(activeOperation = op) }
    }

    fun onFilterChange(filter: String) {
        _uiState.update { it.copy(filterJson = filter) }
    }

    fun onInsertChange(json: String) {
        _uiState.update { it.copy(insertJson = json) }
    }

    fun onUpdateChange(json: String) {
        _uiState.update { it.copy(updateJson = json) }
    }

    fun onMultipleToggle(value: Boolean) {
        _uiState.update { it.copy(isMultiple = value) }
    }

    fun connect() {
        val uri = _uiState.value.uri.trim()
        if (uri.isBlank()) {
            log("Error: MongoDB URI cannot be blank.", LogLevel.ERROR)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, connectionState = ConnectionState.Connecting(uri)) }
            log("Connecting to MongoDB URI...", LogLevel.INFO)

            val result = mongoManager.connect(uri)
            result.onSuccess { connected ->
                val firstDb = connected.databases.firstOrNull() ?: ""
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        connectionState = connected,
                        databases = connected.databases,
                        selectedDatabase = firstDb
                    )
                }
                log("Connected successfully to ${connected.serverVersion} (ping: ${connected.pingMs}ms)", LogLevel.SUCCESS)
                log("Databases available: ${connected.databases.joinToString(", ")}", LogLevel.INFO)

                if (firstDb.isNotEmpty()) {
                    loadCollections(firstDb)
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        connectionState = ConnectionState.Error(err.message ?: "Connection failed", err.stackTraceToString())
                    )
                }
                log("Connection Failed: ${err.message}", LogLevel.ERROR)
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            mongoManager.disconnect()
            _uiState.update {
                it.copy(
                    connectionState = ConnectionState.Disconnected,
                    databases = emptyList(),
                    selectedDatabase = "",
                    collections = emptyList(),
                    selectedCollection = "",
                    queryResult = QueryResult(),
                    isLoading = false
                )
            }
            log("Disconnected from MongoDB.", LogLevel.INFO)
        }
    }

    private fun loadCollections(dbName: String) {
        if (dbName.isBlank()) return
        viewModelScope.launch {
            val result = mongoManager.getCollections(dbName)
            result.onSuccess { colls ->
                val firstColl = colls.firstOrNull() ?: ""
                _uiState.update {
                    it.copy(
                        collections = colls,
                        selectedCollection = firstColl
                    )
                }
                log("Loaded ${colls.size} collections for database '$dbName'", LogLevel.INFO)
            }.onFailure { err ->
                log("Failed to list collections for '$dbName': ${err.message}", LogLevel.ERROR)
            }
        }
    }

    fun refreshCollections() {
        val currentDb = _uiState.value.selectedDatabase
        if (currentDb.isNotEmpty()) {
            loadCollections(currentDb)
        }
    }

    fun executeOperation() {
        val state = _uiState.value
        val db = state.selectedDatabase
        val coll = state.selectedCollection

        if (db.isBlank() || coll.isBlank()) {
            log("Error: Please select a Database and Collection first.", LogLevel.WARN)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (state.activeOperation) {
                MongoOperation.FIND -> {
                    log("Executing FIND query on '$db.$coll' with filter: ${state.filterJson}", LogLevel.INFO)
                    val res = mongoManager.findDocuments(db, coll, state.filterJson)
                    handleResult("FIND", res)
                }
                MongoOperation.INSERT -> {
                    log("Executing INSERT into '$db.$coll'", LogLevel.INFO)
                    val res = mongoManager.insertDocument(db, coll, state.insertJson)
                    handleResult("INSERT", res)
                }
                MongoOperation.UPDATE -> {
                    log("Executing UPDATE on '$db.$coll' (multiple: ${state.isMultiple})", LogLevel.INFO)
                    val res = mongoManager.updateDocument(db, coll, state.filterJson, state.updateJson, state.isMultiple)
                    handleResult("UPDATE", res)
                }
                MongoOperation.DELETE -> {
                    log("Executing DELETE on '$db.$coll' (multiple: ${state.isMultiple})", LogLevel.INFO)
                    val res = mongoManager.deleteDocument(db, coll, state.filterJson, state.isMultiple)
                    handleResult("DELETE", res)
                }
            }
        }
    }

    private fun handleResult(opName: String, result: Result<QueryResult>) {
        result.onSuccess { qr ->
            _uiState.update { it.copy(isLoading = false, queryResult = qr) }
            log("[$opName SUCCESS] ${qr.message}", LogLevel.SUCCESS)
        }.onFailure { err ->
            _uiState.update { it.copy(isLoading = false) }
            log("[$opName FAILED] ${err.message}", LogLevel.ERROR)
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    private fun log(message: String, level: LogLevel) {
        val timestamp = timeFormat.format(Date())
        val entry = LogEntry(timestamp = timestamp, message = message, level = level)
        _uiState.update { it.copy(logs = listOf(entry) + it.logs.take(99)) }
    }

    override fun onCleared() {
        super.onCleared()
        mongoManager.disconnect()
    }
}
