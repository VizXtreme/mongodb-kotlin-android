package com.vizx.mongodbclient.data

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoSecurityException
import com.mongodb.MongoTimeoutException
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.vizx.mongodbclient.dns.AndroidDnsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.json.JsonWriterSettings
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

    suspend fun connect(uri: String): Result<ConnectionState.Connected> = withContext(Dispatchers.IO) {
        try {
            disconnect()

            val connectionString = ConnectionString(uri)
            val settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .dnsClient(AndroidDnsClient())
                .applyToSocketSettings { builder ->
                    builder.connectTimeout(15, TimeUnit.SECONDS)
                    builder.readTimeout(20, TimeUnit.SECONDS)
                }
                .applyToClusterSettings { builder ->
                    builder.serverSelectionTimeout(15, TimeUnit.SECONDS)
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
            } catch (_: Exception) {
            }

            // Fetch database list
            val dbNames = try {
                newClient.listDatabaseNames().into(ArrayList())
            } catch (_: Exception) {
                val defaultDb = connectionString.database
                if (!defaultDb.isNullOrBlank()) listOf(defaultDb) else listOf("test")
            }

            val clusterType = try {
                newClient.clusterDescription.type.name
            } catch (_: Exception) {
                "REPLICA_SET"
            }

            val hosts = try {
                newClient.clusterDescription.serverDescriptions.map { it.address.toString() }
            } catch (_: Exception) {
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
        } catch (e: Exception) {
            disconnect()
            Result.failure(Exception("Connection Error: ${e.localizedMessage ?: e.message}", e))
        }
    }

    suspend fun ping(): Result<Long> = withContext(Dispatchers.IO) {
        val activeClient = client ?: return@withContext Result.failure(Exception("Not connected"))
        try {
            val start = System.currentTimeMillis()
            activeClient.getDatabase("admin").runCommand(Document("ping", 1))
            Result.success(System.currentTimeMillis() - start)
        } catch (e: Exception) {
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
        } catch (e: Exception) {
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
                } catch (_: Exception) {
                    0L
                }
                CollectionSummary(name = name, documentCount = count)
            }
            Result.success(summaries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCollections(dbName: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val activeClient = client ?: return@withContext Result.failure(Exception("Not connected to MongoDB"))
        try {
            val db = activeClient.getDatabase(dbName)
            val colls = db.listCollectionNames().into(ArrayList())
            Result.success(colls)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findDocuments(
        dbName: String,
        collectionName: String,
        filterJson: String,
        limit: Int = 25
    ): Result<QueryResult> = withContext(Dispatchers.IO) {
        val coll = getCollection(dbName, collectionName)
            ?: return@withContext Result.failure(Exception("Not connected or invalid collection"))

        try {
            val startTime = System.currentTimeMillis()
            val filterDoc = if (filterJson.isBlank()) Document() else Document.parse(filterJson)
            val totalCount = coll.countDocuments(filterDoc)
            val docs = coll.find(filterDoc).limit(limit).into(ArrayList())
            val executionTime = System.currentTimeMillis() - startTime

            val jsonDocs = docs.map { it.toJson(prettyJsonSettings) }
            Result.success(
                QueryResult(
                    documents = jsonDocs,
                    totalCount = totalCount,
                    executionTimeMs = executionTime,
                    message = "Found ${docs.size} of $totalCount document(s) (${executionTime}ms)"
                )
            )
        } catch (e: Exception) {
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
        } catch (e: Exception) {
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
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            Result.failure(Exception("Delete Error: ${e.message}", e))
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
        } catch (_: Exception) {
        } finally {
            client = null
        }
    }
}
