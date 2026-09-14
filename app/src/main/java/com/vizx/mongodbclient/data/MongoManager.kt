package com.vizx.mongodbclient.data

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoSecurityException
import com.mongodb.MongoTimeoutException
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.vizx.mongodbclient.dns.AndroidDnsClient
import com.mongodb.client.model.IndexOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.json.JsonWriterSettings
import org.json.JSONArray
import java.util.Locale
import java.util.concurrent.TimeUnit

class MongoManager {

    private var client: MongoClient? = null
    private var currentUri: String = ""

    private val prettyJsonSettings = JsonWriterSettings.builder()
        .indent(true)
        .build()

    val isConnected: Boolean
        get() = client != null

    suspend fun connect(
        uri: String,
        networkConfig: NetworkConfig = NetworkConfig()
    ): Result<ConnectionState.Connected> = withContext(Dispatchers.IO) {
        try {
            disconnect()

            val connectionString = ConnectionString(uri)
            val settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .dnsClient(AndroidDnsClient())
                .applyToSocketSettings { builder ->
                    builder.connectTimeout(networkConfig.connectTimeoutSeconds.toLong(), TimeUnit.SECONDS)
                    builder.readTimeout(networkConfig.readTimeoutSeconds.toLong(), TimeUnit.SECONDS)
                }
                .applyToClusterSettings { builder ->
                    builder.serverSelectionTimeout(networkConfig.serverSelectionTimeoutSeconds.toLong(), TimeUnit.SECONDS)
                }
                .applyToConnectionPoolSettings { builder ->
                    builder.maxSize(networkConfig.maxPoolSize)
                    builder.minSize(networkConfig.minPoolSize)
                }
                .applyToSslSettings { builder ->
                    if (networkConfig.allowInvalidHostnames) {
                        builder.invalidHostNameAllowed(true)
                    }
                }
                .build()

            val newClient = MongoClients.create(settings)

            // Test connection by executing ping on admin database
            val startTime = System.currentTimeMillis()
            val adminDb = newClient.getDatabase("admin")
            adminDb.runCommand(Document("ping", 1))
            val pingMs = System.currentTimeMillis() - startTime

            // Fetch server version
            var serverVersion = "MongoDB Server"
            try {
                val buildInfo = adminDb.runCommand(Document("buildInfo", 1))
                serverVersion = "MongoDB v${buildInfo.getString("version") ?: "Unknown"}"
            } catch (_: Throwable) {
            }

            // Fetch database list
            val dbNames = try {
                newClient.listDatabaseNames().into(ArrayList())
            } catch (_: Throwable) {
                val defaultDb = connectionString.database
                if (!defaultDb.isNullOrBlank()) listOf(defaultDb) else listOf("test")
            }

            val clusterType = try {
                newClient.clusterDescription.type.name
            } catch (_: Throwable) {
                "REPLICA_SET"
            }

            val hosts = try {
                newClient.clusterDescription.serverDescriptions.map { it.address.toString() }
            } catch (_: Throwable) {
                emptyList()
            }

            val clusterInfo = ClusterInfo(
                serverVersion = serverVersion,
                clusterType = clusterType,
                pingMs = pingMs,
                hosts = hosts,
                connectionMode = if (clusterType.contains("REPLICA", ignoreCase = true)) "Replica Set" else "Cluster"
            )

            client = newClient
            currentUri = uri

            Result.success(
                ConnectionState.Connected(
                    uri = uri,
                    serverVersion = serverVersion,
                    databases = dbNames,
                    pingMs = pingMs,
                    clusterInfo = clusterInfo
                )
            )
        } catch (e: MongoSecurityException) {
            disconnect()
            Result.failure(Exception("Authentication Failed: Check your username and password.\n${e.message}", e))
        } catch (e: MongoTimeoutException) {
            disconnect()
            Result.failure(Exception("Connection Timed Out: Unable to reach MongoDB cluster.\nVerify that your IP address is whitelisted in MongoDB Atlas (Network Access -> Add IP Address: 0.0.0.0/0 for testing).\n${e.message}", e))
        } catch (e: Throwable) {
            disconnect()
            Result.failure(Exception("Connection Error (${e.javaClass.simpleName}): ${e.localizedMessage ?: e.message}", e))
        }
    }

    suspend fun ping(): Result<Long> = withContext(Dispatchers.IO) {
        val activeClient = client ?: return@withContext Result.failure(Exception("Not connected"))
        try {
            val start = System.currentTimeMillis()
            activeClient.getDatabase("admin").runCommand(Document("ping", 1))
            Result.success(System.currentTimeMillis() - start)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun getDatabaseStats(dbName: String): Result<DatabaseStats> = withContext(Dispatchers.IO) {
        val activeClient = client ?: return@withContext Result.failure(Exception("Not connected to MongoDB"))
        if (dbName.isBlank()) return@withContext Result.failure(Exception("Database name is blank"))

        try {
            val db = activeClient.getDatabase(dbName)
            val statsDoc = db.runCommand(Document("dbStats", 1))

            val collCount = (statsDoc.get("collections") as? Number)?.toInt() ?: 0
            val objCount = (statsDoc.get("objects") as? Number)?.toLong() ?: 0L
            val avgSize = (statsDoc.get("avgObjSize") as? Number)?.toDouble() ?: 0.0
            val dataSize = (statsDoc.get("dataSize") as? Number)?.toDouble() ?: 0.0
            val storageSize = (statsDoc.get("storageSize") as? Number)?.toDouble() ?: 0.0
            val idxCount = (statsDoc.get("indexes") as? Number)?.toInt() ?: 0
            val idxSize = (statsDoc.get("indexSize") as? Number)?.toDouble() ?: 0.0

            Result.success(
                DatabaseStats(
                    dbName = dbName,
                    collectionsCount = collCount,
                    objectsCount = objCount,
                    avgObjSize = avgSize,
                    dataSizeFormatted = formatBytes(dataSize),
                    storageSizeFormatted = formatBytes(storageSize),
                    indexesCount = idxCount,
                    indexSizeFormatted = formatBytes(idxSize)
                )
            )
        } catch (e: Throwable) {
            Result.failure(Exception("Failed to fetch stats for '$dbName': ${e.message}", e))
        }
    }

    suspend fun getCollectionSummaries(dbName: String): Result<List<CollectionSummary>> = withContext(Dispatchers.IO) {
        val activeClient = client ?: return@withContext Result.failure(Exception("Not connected to MongoDB"))
        if (dbName.isBlank()) return@withContext Result.failure(Exception("Database name is blank"))

        try {
            val db = activeClient.getDatabase(dbName)
            val collNames = db.listCollectionNames().into(ArrayList())
            val summaries = collNames.map { name ->
                val count = try {
                    db.getCollection(name).estimatedDocumentCount()
                } catch (_: Throwable) {
                    0L
                }
                CollectionSummary(name = name, documentCount = count)
            }
            Result.success(summaries)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun getCollections(dbName: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val activeClient = client ?: return@withContext Result.failure(Exception("Not connected to MongoDB"))
        try {
            val db = activeClient.getDatabase(dbName)
            val colls = db.listCollectionNames().into(ArrayList())
            Result.success(colls)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun findDocuments(
        dbName: String,
        collectionName: String,
        filterJson: String,
        sortJson: String = "",
        projectionJson: String = "",
        limit: Int = 25,
        skip: Int = 0
    ): Result<QueryResult> = withContext(Dispatchers.IO) {
        val coll = getCollection(dbName, collectionName)
            ?: return@withContext Result.failure(Exception("Not connected or invalid collection"))

        try {
            val startTime = System.currentTimeMillis()
            val filterDoc = if (filterJson.isBlank()) Document() else Document.parse(filterJson)
            val totalCount = coll.countDocuments(filterDoc)

            var query = coll.find(filterDoc)
            if (sortJson.isNotBlank() && sortJson.trim() != "{}") {
                query = query.sort(Document.parse(sortJson))
            }
            if (projectionJson.isNotBlank() && projectionJson.trim() != "{}") {
                query = query.projection(Document.parse(projectionJson))
            }
            if (skip > 0) {
                query = query.skip(skip)
            }
            if (limit > 0) {
                query = query.limit(limit)
            }

            val docs = query.into(ArrayList())
            val executionTime = System.currentTimeMillis() - startTime

            val jsonDocs = docs.map { it.toJson(prettyJsonSettings) }
            Result.success(
                QueryResult(
                    documents = jsonDocs,
                    totalCount = totalCount,
                    executionTimeMs = executionTime,
                    message = "Found ${docs.size} of $totalCount matching document(s) (${executionTime}ms)"
                )
            )
        } catch (e: Throwable) {
            Result.failure(Exception("Find Error: ${e.message}", e))
        }
    }

    suspend fun insertDocument(
        dbName: String,
        collectionName: String,
        documentJson: String
    ): Result<QueryResult> = withContext(Dispatchers.IO) {
        val coll = getCollection(dbName, collectionName)
            ?: return@withContext Result.failure(Exception("Not connected or invalid collection"))

        try {
            val startTime = System.currentTimeMillis()
            val doc = Document.parse(documentJson)
            val insertResult = coll.insertOne(doc)
            val executionTime = System.currentTimeMillis() - startTime

            val idStr = insertResult.insertedId?.toString() ?: doc.get("_id")?.toString() ?: "Unknown ID"
            val insertedDocJson = doc.toJson(prettyJsonSettings)

            Result.success(
                QueryResult(
                    documents = listOf(insertedDocJson),
                    totalCount = 1,
                    executionTimeMs = executionTime,
                    message = "Inserted 1 document with _id: $idStr (${executionTime}ms)"
                )
            )
        } catch (e: Throwable) {
            Result.failure(Exception("Insert Error: ${e.message}", e))
        }
    }

    suspend fun updateDocument(
        dbName: String,
        collectionName: String,
        filterJson: String,
        updateJson: String,
        isUpdateMany: Boolean = false
    ): Result<QueryResult> = withContext(Dispatchers.IO) {
        val coll = getCollection(dbName, collectionName)
            ?: return@withContext Result.failure(Exception("Not connected or invalid collection"))

        try {
            val startTime = System.currentTimeMillis()
            val filterDoc = Document.parse(filterJson)
            var updateDoc = Document.parse(updateJson)

            val hasOperator = updateDoc.keys.any { it.startsWith("$") }
            if (!hasOperator) {
                updateDoc = Document("\$set", updateDoc)
            }

            val result = if (isUpdateMany) {
                coll.updateMany(filterDoc, updateDoc)
            } else {
                coll.updateOne(filterDoc, updateDoc)
            }
            val executionTime = System.currentTimeMillis() - startTime

            Result.success(
                QueryResult(
                    documents = emptyList(),
                    totalCount = result.modifiedCount,
                    executionTimeMs = executionTime,
                    message = "Matched: ${result.matchedCount}, Modified: ${result.modifiedCount}, Acknowledged: ${result.wasAcknowledged()} (${executionTime}ms)"
                )
            )
        } catch (e: Throwable) {
            Result.failure(Exception("Update Error: ${e.message}", e))
        }
    }

    suspend fun deleteDocument(
        dbName: String,
        collectionName: String,
        filterJson: String,
        isDeleteMany: Boolean = false
    ): Result<QueryResult> = withContext(Dispatchers.IO) {
        val coll = getCollection(dbName, collectionName)
            ?: return@withContext Result.failure(Exception("Not connected or invalid collection"))

        try {
            val startTime = System.currentTimeMillis()
            val filterDoc = Document.parse(filterJson)

            val result = if (isDeleteMany) {
                coll.deleteMany(filterDoc)
            } else {
                coll.deleteOne(filterDoc)
            }
            val executionTime = System.currentTimeMillis() - startTime

            Result.success(
                QueryResult(
                    documents = emptyList(),
                    totalCount = result.deletedCount,
                    executionTimeMs = executionTime,
                    message = "Deleted: ${result.deletedCount} document(s) (${executionTime}ms)"
                )
            )
        } catch (e: Throwable) {
            Result.failure(Exception("Delete Error: ${e.message}", e))
        }
    }

    suspend fun aggregateDocuments(
        dbName: String,
        collectionName: String,
        pipelineJson: String
    ): Result<QueryResult> = withContext(Dispatchers.IO) {
        val coll = getCollection(dbName, collectionName)
            ?: return@withContext Result.failure(Exception("Not connected or invalid collection"))

        try {
            val startTime = System.currentTimeMillis()
            val pipelineList = mutableListOf<Document>()

            val trimmed = pipelineJson.trim()
            if (trimmed.startsWith("[")) {
                val jsonArray = JSONArray(trimmed)
                for (i in 0 until jsonArray.length()) {
                    pipelineList.add(Document.parse(jsonArray.getJSONObject(i).toString()))
                }
            } else if (trimmed.startsWith("{")) {
                pipelineList.add(Document.parse(trimmed))
            }

            val docs = coll.aggregate(pipelineList).into(ArrayList())
            val executionTime = System.currentTimeMillis() - startTime
            val jsonDocs = docs.map { it.toJson(prettyJsonSettings) }

            Result.success(
                QueryResult(
                    documents = jsonDocs,
                    totalCount = docs.size.toLong(),
                    executionTimeMs = executionTime,
                    message = "Pipeline executed (${pipelineList.size} stages), returned ${docs.size} document(s) (${executionTime}ms)"
                )
            )
        } catch (e: Throwable) {
            Result.failure(Exception("Aggregation Error: ${e.message}", e))
        }
    }

    suspend fun countDocuments(
        dbName: String,
        collectionName: String,
        filterJson: String
    ): Result<QueryResult> = withContext(Dispatchers.IO) {
        val coll = getCollection(dbName, collectionName)
            ?: return@withContext Result.failure(Exception("Not connected or invalid collection"))

        try {
            val startTime = System.currentTimeMillis()
            val filterDoc = if (filterJson.isBlank()) Document() else Document.parse(filterJson)
            val count = coll.countDocuments(filterDoc)
            val executionTime = System.currentTimeMillis() - startTime

            Result.success(
                QueryResult(
                    documents = listOf("{\n  \"collection\": \"$dbName.$collectionName\",\n  \"count\": $count,\n  \"filter\": ${filterDoc.toJson()}\n}"),
                    totalCount = count,
                    executionTimeMs = executionTime,
                    message = "Counted $count document(s) matching filter (${executionTime}ms)"
                )
            )
        } catch (e: Throwable) {
            Result.failure(Exception("Count Error: ${e.message}", e))
        }
    }

    suspend fun createCollection(dbName: String, collectionName: String): Result<String> = withContext(Dispatchers.IO) {
        val activeClient = client ?: return@withContext Result.failure(Exception("Not connected to MongoDB"))
        if (dbName.isBlank() || collectionName.isBlank()) return@withContext Result.failure(Exception("Invalid database or collection name"))

        try {
            activeClient.getDatabase(dbName).createCollection(collectionName.trim())
            Result.success("Collection '${collectionName.trim()}' created successfully.")
        } catch (e: Throwable) {
            Result.failure(Exception("Create Collection Error: ${e.message}", e))
        }
    }

    suspend fun dropCollection(dbName: String, collectionName: String): Result<String> = withContext(Dispatchers.IO) {
        val coll = getCollection(dbName, collectionName)
            ?: return@withContext Result.failure(Exception("Not connected or invalid collection"))

        try {
            coll.drop()
            Result.success("Collection '$dbName.$collectionName' dropped successfully.")
        } catch (e: Throwable) {
            Result.failure(Exception("Drop Collection Error: ${e.message}", e))
        }
    }

    suspend fun getIndexes(dbName: String, collectionName: String): Result<List<IndexSummary>> = withContext(Dispatchers.IO) {
        val coll = getCollection(dbName, collectionName)
            ?: return@withContext Result.failure(Exception("Not connected or invalid collection"))

        try {
            val indexes = coll.listIndexes().into(ArrayList())
            val summaries = indexes.map { doc ->
                val name = doc.getString("name") ?: "unnamed"
                val keyDoc = doc.get("key") as? Document
                val keysStr = keyDoc?.toJson() ?: "{}"
                val isUnique = doc.getBoolean("unique") ?: false
                IndexSummary(name = name, keys = keysStr, isUnique = isUnique)
            }
            Result.success(summaries)
        } catch (e: Throwable) {
            Result.failure(Exception("Get Indexes Error: ${e.message}", e))
        }
    }

    suspend fun createIndex(
        dbName: String,
        collectionName: String,
        keysJson: String,
        isUnique: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        val coll = getCollection(dbName, collectionName)
            ?: return@withContext Result.failure(Exception("Not connected or invalid collection"))

        try {
            val keysDoc = Document.parse(keysJson)
            val options = IndexOptions().unique(isUnique)
            val indexName = coll.createIndex(keysDoc, options)
            Result.success("Created index '$indexName' successfully.")
        } catch (e: Throwable) {
            Result.failure(Exception("Create Index Error: ${e.message}", e))
        }
    }

    suspend fun dropIndex(
        dbName: String,
        collectionName: String,
        indexName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val coll = getCollection(dbName, collectionName)
            ?: return@withContext Result.failure(Exception("Not connected or invalid collection"))

        try {
            coll.dropIndex(indexName)
            Result.success("Dropped index '$indexName' successfully.")
        } catch (e: Throwable) {
            Result.failure(Exception("Drop Index Error: ${e.message}", e))
        }
    }

    suspend fun getServerStatus(): Result<ServerStatusMetrics> = withContext(Dispatchers.IO) {
        val activeClient = client ?: return@withContext Result.failure(Exception("Not connected to MongoDB"))
        try {
            val statusDoc = activeClient.getDatabase("admin").runCommand(Document("serverStatus", 1))

            val connections = statusDoc.get("connections") as? Document
            val currConn = (connections?.get("current") as? Number)?.toLong() ?: 0L
            val availConn = (connections?.get("available") as? Number)?.toLong() ?: 0L
            val totalConn = (connections?.get("totalCreated") as? Number)?.toLong() ?: 0L

            val uptime = (statusDoc.get("uptime") as? Number)?.toLong() ?: 0L

            val mem = statusDoc.get("mem") as? Document
            val residentMem = (mem?.get("resident") as? Number)?.toLong() ?: 0L
            val virtualMem = (mem?.get("virtual") as? Number)?.toLong() ?: 0L

            val opcounters = statusDoc.get("opcounters") as? Document
            val ins = (opcounters?.get("insert") as? Number)?.toLong() ?: 0L
            val qry = (opcounters?.get("query") as? Number)?.toLong() ?: 0L
            val upd = (opcounters?.get("update") as? Number)?.toLong() ?: 0L
            val del = (opcounters?.get("delete") as? Number)?.toLong() ?: 0L
            val cmd = (opcounters?.get("command") as? Number)?.toLong() ?: 0L

            val network = statusDoc.get("network") as? Document
            val bytesIn = (network?.get("bytesIn") as? Number)?.toDouble() ?: 0.0
            val bytesOut = (network?.get("bytesOut") as? Number)?.toDouble() ?: 0.0

            Result.success(
                ServerStatusMetrics(
                    currentConnections = currConn,
                    availableConnections = availConn,
                    totalCreatedConnections = totalConn,
                    uptimeSeconds = uptime,
                    residentMemoryMb = residentMem,
                    virtualMemoryMb = virtualMem,
                    opcountersInsert = ins,
                    opcountersQuery = qry,
                    opcountersUpdate = upd,
                    opcountersDelete = del,
                    opcountersCommand = cmd,
                    networkBytesInFormatted = formatBytes(bytesIn),
                    networkBytesOutFormatted = formatBytes(bytesOut)
                )
            )
        } catch (e: Throwable) {
            Result.failure(Exception("Failed to fetch serverStatus: ${e.message}", e))
        }
    }

    suspend fun getCurrentOps(): Result<List<ActiveOperation>> = withContext(Dispatchers.IO) {
        val activeClient = client ?: return@withContext Result.failure(Exception("Not connected to MongoDB"))
        try {
            val cmd = Document("currentOp", 1).append("\$all", true)
            val res = activeClient.getDatabase("admin").runCommand(cmd)

            val inprog = res.get("inprog") as? List<*> ?: emptyList<Any>()
            val ops = inprog.filterIsInstance<Document>().map { doc ->
                val opId = (doc.get("opid") as? Number)?.toLong() ?: 0L
                val ns = doc.getString("ns") ?: "system"
                val opType = doc.getString("op") ?: "command"
                val secs = (doc.get("secs_running") as? Number)?.toLong() ?: 0L
                val queryDoc = doc.get("command") as? Document ?: doc.get("query") as? Document
                val queryJson = queryDoc?.toJson(prettyJsonSettings) ?: "{}"
                ActiveOperation(
                    opId = opId,
                    ns = ns,
                    op = opType,
                    secsRunning = secs,
                    queryJson = queryJson
                )
            }
            Result.success(ops)
        } catch (e: Throwable) {
            Result.failure(Exception("Failed to fetch currentOp: ${e.message}", e))
        }
    }

    suspend fun killOp(opId: Long): Result<String> = withContext(Dispatchers.IO) {
        val activeClient = client ?: return@withContext Result.failure(Exception("Not connected to MongoDB"))
        try {
            activeClient.getDatabase("admin").runCommand(Document("killOp", 1).append("op", opId))
            Result.success("Operation $opId killed successfully.")
        } catch (e: Throwable) {
            Result.failure(Exception("Failed to kill op $opId: ${e.message}", e))
        }
    }

    suspend fun getReplicaSetStatus(): Result<ReplicaSetInfo> = withContext(Dispatchers.IO) {
        val activeClient = client ?: return@withContext Result.failure(Exception("Not connected to MongoDB"))
        try {
            val statusDoc = activeClient.getDatabase("admin").runCommand(Document("replSetGetStatus", 1))
            val setName = statusDoc.getString("set") ?: "ReplicaSet"
            val myState = statusDoc.get("myState")?.toString() ?: "1"
            val membersList = statusDoc.get("members") as? List<*> ?: emptyList<Any>()
            var primaryHost: String? = null
            val members = membersList.filterIsInstance<Document>().map { doc ->
                val id = (doc.get("_id") as? Number)?.toLong() ?: 0L
                val name = doc.getString("name") ?: ""
                val stateStr = doc.getString("stateStr") ?: "UNKNOWN"
                if (stateStr.equals("PRIMARY", ignoreCase = true)) {
                    primaryHost = name
                }
                val health = (doc.get("health") as? Number)?.toDouble() ?: 1.0
                val uptime = (doc.get("uptime") as? Number)?.toLong() ?: 0L
                val ping = (doc.get("pingMs") as? Number)?.toLong() ?: 0L
                val self = doc.getBoolean("self", false)
                ReplicaSetMember(
                    id = id,
                    name = name,
                    stateStr = stateStr,
                    health = health,
                    uptimeSeconds = uptime,
                    pingMs = ping,
                    isSelf = self
                )
            }
            Result.success(
                ReplicaSetInfo(
                    setName = setName,
                    isReplicaSet = true,
                    myState = myState,
                    primaryHost = primaryHost,
                    members = members
                )
            )
        } catch (e: Throwable) {
            val msg = e.message ?: ""
            if (msg.contains("not running with --replSet") || msg.contains("no replSet") || msg.contains("CommandNotFound") || msg.contains("not supported")) {
                Result.success(
                    ReplicaSetInfo(
                        setName = "Standalone / Serverless",
                        isReplicaSet = false,
                        myState = "STANDALONE",
                        primaryHost = null,
                        members = emptyList()
                    )
                )
            } else {
                Result.failure(Exception("ReplicaSet status failed: ${e.message}", e))
            }
        }
    }

    private fun getCollection(dbName: String, collectionName: String): MongoCollection<Document>? {
        val activeClient = client ?: return null
        if (dbName.isBlank() || collectionName.isBlank()) return null
        return activeClient.getDatabase(dbName).getCollection(collectionName)
    }

    private fun formatBytes(bytes: Double): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return String.format(Locale.US, "%.2f %s", value, units[digitGroups])
    }

    fun disconnect() {
        try {
            client?.close()
        } catch (_: Throwable) {
        } finally {
            client = null
        }
    }
}
