package com.vizx.mongodbclient.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vizx.mongodbclient.data.ActiveOperation
import com.vizx.mongodbclient.data.AppScreen
import com.vizx.mongodbclient.data.CollectionSummary
import com.vizx.mongodbclient.data.ConnectionState
import com.vizx.mongodbclient.data.ConnectionStorage
import com.vizx.mongodbclient.data.DatabaseStats
import com.vizx.mongodbclient.data.IndexSummary
import com.vizx.mongodbclient.data.LogEntry
import com.vizx.mongodbclient.data.LogLevel
import com.vizx.mongodbclient.data.MongoManager
import com.vizx.mongodbclient.data.MongoOperation
import com.vizx.mongodbclient.data.NetworkConfig
import com.vizx.mongodbclient.data.QueryResult
import com.vizx.mongodbclient.data.SavedConnection
import com.vizx.mongodbclient.data.ServerStatusMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MongoUiState(
    val currentScreen: AppScreen = AppScreen.LOGIN,
    val uri: String = "mongodb+srv://username:password@cluster0.ywgy3ll.mongodb.net/?appName=Cluster0",
    val profileName: String = "",
    val showPassword: Boolean = false,
    val savedConnections: List<SavedConnection> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val networkConfig: NetworkConfig = NetworkConfig(),
    val databases: List<String> = emptyList(),
    val selectedDatabase: String = "",
    val databaseStats: DatabaseStats? = null,
    val collectionSummaries: List<CollectionSummary> = emptyList(),
    val selectedCollection: String = "",
    val activeOperation: MongoOperation = MongoOperation.FIND,
    val filterJson: String = "{}",
    val sortJson: String = "{}",
    val projectionJson: String = "{}",
    val limit: Int = 20,
    val skip: Int = 0,
    val pipelineJson: String = "[\n  { \"\$limit\": 10 }\n]",
    val insertJson: String = "{\n  \"name\": \"Sample Document\",\n  \"status\": \"active\",\n  \"createdAt\": \"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\"\n}",
    val updateJson: String = "{\n  \"status\": \"updated\",\n  \"version\": 2\n}",
    val isMultiple: Boolean = false,
    val queryResult: QueryResult = QueryResult(),
    val indexSummaries: List<IndexSummary> = emptyList(),
    val serverMetrics: ServerStatusMetrics? = null,
    val activeOperations: List<ActiveOperation> = emptyList(),
    val isLoadingMetrics: Boolean = false,
    val isLoadingOps: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshingStats: Boolean = false,
    val logs: List<LogEntry> = emptyList()
)

class MongoViewModel @JvmOverloads constructor(
    application: Application,
    private val mongoManager: MongoManager = MongoManager()
) : AndroidViewModel(application) {

    private val storage = ConnectionStorage(application.applicationContext)
    private val _uiState = MutableStateFlow(MongoUiState())
    val uiState: StateFlow<MongoUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    init {
        loadSavedConnections()
        log("App initialized. Ready to connect to MongoDB.", LogLevel.INFO)
    }

    private fun loadSavedConnections() {
        val saved = storage.getSavedConnections()
        _uiState.update {
            it.copy(
                savedConnections = saved,
                uri = saved.firstOrNull()?.uri ?: it.uri,
                profileName = saved.firstOrNull()?.name ?: ""
            )
        }
    }

    fun onUriChange(newUri: String) {
        _uiState.update { it.copy(uri = newUri) }
    }

    fun onProfileNameChange(newName: String) {
        _uiState.update { it.copy(profileName = newName) }
    }

    fun toggleShowPassword() {
        _uiState.update { it.copy(showPassword = !it.showPassword) }
    }

    fun selectSavedConnection(connection: SavedConnection) {
        _uiState.update {
            it.copy(
                uri = connection.uri,
                profileName = connection.name
            )
        }
        log("Loaded connection profile: '${connection.name}'", LogLevel.INFO)
    }

    fun deleteSavedConnection(id: String) {
        storage.deleteConnection(id)
        loadSavedConnections()
        log("Deleted connection profile.", LogLevel.INFO)
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun onDatabaseSelected(dbName: String) {
        _uiState.update {
            it.copy(
                selectedDatabase = dbName,
                selectedCollection = "",
                databaseStats = null,
                collectionSummaries = emptyList()
            )
        }
        loadDatabaseDetails(dbName)
    }

    fun onCollectionSelected(collName: String) {
        _uiState.update { it.copy(selectedCollection = collName, indexSummaries = emptyList()) }
        log("Selected collection: '${_uiState.value.selectedDatabase}.$collName'", LogLevel.INFO)
        loadIndexes(_uiState.value.selectedDatabase, collName)
    }

    fun onOperationSelected(op: MongoOperation) {
        _uiState.update { it.copy(activeOperation = op) }
    }

    fun onFilterChange(filter: String) {
        _uiState.update { it.copy(filterJson = filter) }
    }

    fun onSortChange(sort: String) {
        _uiState.update { it.copy(sortJson = sort) }
    }

    fun onProjectionChange(proj: String) {
        _uiState.update { it.copy(projectionJson = proj) }
    }

    fun onLimitChange(limit: Int) {
        _uiState.update { it.copy(limit = limit) }
    }

    fun onSkipChange(skip: Int) {
        _uiState.update { it.copy(skip = skip) }
    }

    fun onPipelineChange(pipeline: String) {
        _uiState.update { it.copy(pipelineJson = pipeline) }
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

    fun onNetworkConfigChange(config: NetworkConfig) {
        _uiState.update { it.copy(networkConfig = config) }
    }

    fun connect() {
        val uri = _uiState.value.uri.trim()
        val name = _uiState.value.profileName.trim()
        val networkConfig = _uiState.value.networkConfig
        if (uri.isBlank()) {
            log("Error: MongoDB URI cannot be blank.", LogLevel.ERROR)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, connectionState = ConnectionState.Connecting(uri)) }
            log("Connecting to MongoDB URI (timeout: ${networkConfig.connectTimeoutSeconds}s, pool: ${networkConfig.maxPoolSize})...", LogLevel.INFO)

            try {
                val result = mongoManager.connect(uri, networkConfig)
                result.onSuccess { connected ->
                    // Persist profile (encrypted via Android Keystore)
                    storage.saveConnection(name, uri)
                    loadSavedConnections()

                    val firstDb = connected.databases.firstOrNull() ?: ""
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            connectionState = connected,
                            databases = connected.databases,
                            selectedDatabase = firstDb,
                            currentScreen = AppScreen.HOME // Automatically switch to HomePage upon successful connection
                        )
                    }
                    log("Connected successfully to ${connected.serverVersion} (ping: ${connected.pingMs}ms)", LogLevel.SUCCESS)
                    log("Databases available: ${connected.databases.joinToString(", ")}", LogLevel.INFO)

                    if (firstDb.isNotEmpty()) {
                        loadDatabaseDetails(firstDb)
                    }
                    loadServerMetrics()
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            connectionState = ConnectionState.Error(err.message ?: "Connection failed", err.stackTraceToString())
                        )
                    }
                    log("Connection Failed: ${err.message}", LogLevel.ERROR)
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        connectionState = ConnectionState.Error(t.localizedMessage ?: t.toString(), t.stackTraceToString())
                    )
                }
                log("Connection Exception (${t.javaClass.simpleName}): ${t.message}", LogLevel.ERROR)
            }
        }
    }

    fun loadServerMetrics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMetrics = true) }
            val res = mongoManager.getServerStatus()
            res.onSuccess { metrics ->
                _uiState.update { it.copy(serverMetrics = metrics, isLoadingMetrics = false) }
                log("ServerStatus: ${metrics.currentConnections} active conn, ${metrics.residentMemoryMb}MB mem, ${metrics.opcountersCommand} cmds", LogLevel.INFO)
            }.onFailure { err ->
                _uiState.update { it.copy(isLoadingMetrics = false) }
                log("Telemetry: ${err.message}", LogLevel.WARN)
            }
        }
    }

    fun loadCurrentOps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOps = true) }
            val res = mongoManager.getCurrentOps()
            res.onSuccess { ops ->
                _uiState.update { it.copy(activeOperations = ops, isLoadingOps = false) }
                log("CurrentOp: Captured ${ops.size} active operations in cluster", LogLevel.INFO)
            }.onFailure { err ->
                _uiState.update { it.copy(isLoadingOps = false) }
                log("CurrentOp notice: ${err.message}", LogLevel.WARN)
            }
        }
    }

    fun killOp(opId: Long) {
        viewModelScope.launch {
            val res = mongoManager.killOp(opId)
            res.onSuccess { msg ->
                log(msg, LogLevel.SUCCESS)
                loadCurrentOps()
            }.onFailure { err ->
                log("KillOp failed: ${err.message}", LogLevel.ERROR)
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
                    databaseStats = null,
                    collectionSummaries = emptyList(),
                    selectedCollection = "",
                    queryResult = QueryResult(),
                    isLoading = false,
                    currentScreen = AppScreen.LOGIN // Return to LoginPage
                )
            }
            log("Disconnected from MongoDB. Returned to Login.", LogLevel.INFO)
        }
    }

    fun ping() {
        viewModelScope.launch {
            val res = mongoManager.ping()
            res.onSuccess { pingMs ->
                log("Live Ping: ${pingMs}ms", LogLevel.SUCCESS)
            }.onFailure { err ->
                log("Live Ping Failed: ${err.message}", LogLevel.WARN)
            }
        }
    }

    fun loadDatabaseDetails(dbName: String) {
        if (dbName.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingStats = true) }

            // Fetch DB stats
            val statsRes = mongoManager.getDatabaseStats(dbName)
            statsRes.onSuccess { stats ->
                _uiState.update { it.copy(databaseStats = stats) }
                log("Loaded stats for database '$dbName' (${stats.dataSizeFormatted})", LogLevel.INFO)
            }.onFailure { err ->
                log("Failed to fetch stats for '$dbName': ${err.message}", LogLevel.WARN)
            }

            // Fetch collection summaries with document counts
            val collsRes = mongoManager.getCollectionSummaries(dbName)
            collsRes.onSuccess { colls ->
                val firstColl = colls.firstOrNull()?.name ?: ""
                val currentSelected = _uiState.value.selectedCollection
                val chosenColl = if (currentSelected.isEmpty() || colls.none { c -> c.name == currentSelected }) firstColl else currentSelected
                _uiState.update { state ->
                    state.copy(
                        collectionSummaries = colls,
                        selectedCollection = chosenColl,
                        isRefreshingStats = false
                    )
                }
                log("Loaded ${colls.size} collections for '$dbName'", LogLevel.INFO)
                if (chosenColl.isNotEmpty()) {
                    loadIndexes(dbName, chosenColl)
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isRefreshingStats = false) }
                log("Failed to load collections for '$dbName': ${err.message}", LogLevel.ERROR)
            }
        }
    }

    fun loadIndexes(dbName: String, collName: String) {
        if (dbName.isBlank() || collName.isBlank()) return
        viewModelScope.launch {
            try {
                val res = mongoManager.getIndexes(dbName, collName)
                res.onSuccess { indexes ->
                    _uiState.update { it.copy(indexSummaries = indexes) }
                    log("Loaded ${indexes.size} index(es) for '$dbName.$collName'", LogLevel.INFO)
                }.onFailure { err ->
                    log("Failed to load indexes: ${err.message}", LogLevel.WARN)
                }
            } catch (t: Throwable) {
                log("Error loading indexes: ${t.message}", LogLevel.WARN)
            }
        }
    }

    fun createCollection(name: String) {
        val db = _uiState.value.selectedDatabase
        if (db.isBlank() || name.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = mongoManager.createCollection(db, name)
            res.onSuccess { msg ->
                log(msg, LogLevel.SUCCESS)
                loadDatabaseDetails(db)
                onCollectionSelected(name.trim())
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false) }
                log("Create collection failed: ${err.message}", LogLevel.ERROR)
            }
        }
    }

    fun dropCollection(name: String) {
        val db = _uiState.value.selectedDatabase
        if (db.isBlank() || name.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = mongoManager.dropCollection(db, name)
            res.onSuccess { msg ->
                log(msg, LogLevel.SUCCESS)
                _uiState.update { it.copy(selectedCollection = "", indexSummaries = emptyList(), queryResult = QueryResult()) }
                loadDatabaseDetails(db)
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false) }
                log("Drop collection failed: ${err.message}", LogLevel.ERROR)
            }
        }
    }

    fun createIndex(keys: String, isUnique: Boolean) {
        val db = _uiState.value.selectedDatabase
        val coll = _uiState.value.selectedCollection
        if (db.isBlank() || coll.isBlank() || keys.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = mongoManager.createIndex(db, coll, keys, isUnique)
            res.onSuccess { msg ->
                log(msg, LogLevel.SUCCESS)
                _uiState.update { it.copy(isLoading = false) }
                loadIndexes(db, coll)
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false) }
                log("Create index failed: ${err.message}", LogLevel.ERROR)
            }
        }
    }

    fun dropIndex(indexName: String) {
        val db = _uiState.value.selectedDatabase
        val coll = _uiState.value.selectedCollection
        if (db.isBlank() || coll.isBlank() || indexName.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = mongoManager.dropIndex(db, coll, indexName)
            res.onSuccess { msg ->
                log(msg, LogLevel.SUCCESS)
                _uiState.update { it.copy(isLoading = false) }
                loadIndexes(db, coll)
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false) }
                log("Drop index failed: ${err.message}", LogLevel.ERROR)
            }
        }
    }

    fun prepareEditDocument(docJson: String) {
        try {
            val json = JSONObject(docJson)
            val idVal = json.opt("_id")
            val idFilter = if (idVal != null) "{\n  \"_id\": $idVal\n}" else "{}"
            json.remove("_id")
            val updateBody = json.toString(2)

            _uiState.update {
                it.copy(
                    activeOperation = MongoOperation.UPDATE,
                    filterJson = idFilter,
                    updateJson = updateBody,
                    isMultiple = false
                )
            }
            log("Loaded document into UPDATE editor (matched by _id).", LogLevel.INFO)
        } catch (e: Throwable) {
            log("Could not prepare document for edit: ${e.message}", LogLevel.WARN)
        }
    }

    fun deleteSingleDocument(docJson: String) {
        val db = _uiState.value.selectedDatabase
        val coll = _uiState.value.selectedCollection
        if (db.isBlank() || coll.isBlank()) return

        try {
            val json = JSONObject(docJson)
            val idVal = json.opt("_id")
            if (idVal == null) {
                log("Cannot delete document: '_id' field not found.", LogLevel.WARN)
                return
            }
            val filter = "{\"_id\": $idVal}"
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                log("Deleting document with _id: $idVal", LogLevel.INFO)
                val res = mongoManager.deleteDocument(db, coll, filter, isDeleteMany = false)
                handleResult("DELETE", res)
                loadDatabaseDetails(db)
            }
        } catch (e: Throwable) {
            log("Delete failed: ${e.message}", LogLevel.ERROR)
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
                    log("Executing FIND query on '$db.$coll' (limit: ${state.limit}, skip: ${state.skip})", LogLevel.INFO)
                    val res = mongoManager.findDocuments(
                        dbName = db,
                        collectionName = coll,
                        filterJson = state.filterJson,
                        sortJson = state.sortJson,
                        projectionJson = state.projectionJson,
                        limit = state.limit,
                        skip = state.skip
                    )
                    handleResult("FIND", res)
                }
                MongoOperation.INSERT -> {
                    log("Executing INSERT into '$db.$coll'", LogLevel.INFO)
                    val res = mongoManager.insertDocument(db, coll, state.insertJson)
                    handleResult("INSERT", res)
                    loadDatabaseDetails(db)
                }
                MongoOperation.UPDATE -> {
                    log("Executing UPDATE on '$db.$coll' (multiple: ${state.isMultiple})", LogLevel.INFO)
                    val res = mongoManager.updateDocument(db, coll, state.filterJson, state.updateJson, state.isMultiple)
                    handleResult("UPDATE", res)
                    loadDatabaseDetails(db)
                }
                MongoOperation.DELETE -> {
                    log("Executing DELETE on '$db.$coll' (multiple: ${state.isMultiple})", LogLevel.INFO)
                    val res = mongoManager.deleteDocument(db, coll, state.filterJson, state.isMultiple)
                    handleResult("DELETE", res)
                    loadDatabaseDetails(db)
                }
                MongoOperation.AGGREGATE -> {
                    log("Executing AGGREGATION pipeline on '$db.$coll'", LogLevel.INFO)
                    val res = mongoManager.aggregateDocuments(db, coll, state.pipelineJson)
                    handleResult("AGGREGATE", res)
                }
                MongoOperation.COUNT -> {
                    log("Executing COUNT on '$db.$coll' with filter: ${state.filterJson}", LogLevel.INFO)
                    val res = mongoManager.countDocuments(db, coll, state.filterJson)
                    handleResult("COUNT", res)
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
