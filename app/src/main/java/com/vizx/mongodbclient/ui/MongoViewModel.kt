package com.vizx.mongodbclient.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vizx.mongodbclient.data.AppScreen
import com.vizx.mongodbclient.data.CollectionSummary
import com.vizx.mongodbclient.data.ConnectionState
import com.vizx.mongodbclient.data.ConnectionStorage
import com.vizx.mongodbclient.data.DatabaseStats
import com.vizx.mongodbclient.data.LogEntry
import com.vizx.mongodbclient.data.LogLevel
import com.vizx.mongodbclient.data.MongoManager
import com.vizx.mongodbclient.data.MongoOperation
import com.vizx.mongodbclient.data.QueryResult
import com.vizx.mongodbclient.data.SavedConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val databases: List<String> = emptyList(),
    val selectedDatabase: String = "",
    val databaseStats: DatabaseStats? = null,
    val collectionSummaries: List<CollectionSummary> = emptyList(),
    val selectedCollection: String = "",
    val activeOperation: MongoOperation = MongoOperation.FIND,
    val filterJson: String = "{}",
    val insertJson: String = "{\n  \"name\": \"Sample Document\",\n  \"status\": \"active\",\n  \"createdAt\": \"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\"\n}",
    val updateJson: String = "{\n  \"status\": \"updated\",\n  \"version\": 2\n}",
    val isMultiple: Boolean = false,
    val queryResult: QueryResult = QueryResult(),
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
        val name = _uiState.value.profileName.trim()
        if (uri.isBlank()) {
            log("Error: MongoDB URI cannot be blank.", LogLevel.ERROR)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, connectionState = ConnectionState.Connecting(uri)) }
            log("Connecting to MongoDB URI...", LogLevel.INFO)

            try {
                val result = mongoManager.connect(uri)
                result.onSuccess { connected ->
                    // Persist profile
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
                _uiState.update {
                    it.copy(
                        collectionSummaries = colls,
                        selectedCollection = if (it.selectedCollection.isEmpty() || colls.none { c -> c.name == it.selectedCollection }) firstColl else it.selectedCollection,
                        isRefreshingStats = false
                    )
                }
                log("Loaded ${colls.size} collections for '$dbName'", LogLevel.INFO)
            }.onFailure { err ->
                _uiState.update { it.copy(isRefreshingStats = false) }
                log("Failed to load collections for '$dbName': ${err.message}", LogLevel.ERROR)
            }
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
                    // Refresh collection stats after write
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
