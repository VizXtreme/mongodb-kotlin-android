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
                // Ignore if buildInfo is restricted by user permissions
            }

            // Fetch database list
            val dbNames = try {
                newClient.listDatabaseNames().into(ArrayList())
            } catch (_: Exception) {
                // Fallback: If listDatabases permission is restricted, try getting default db from URI
                val defaultDb = connectionString.database
                if (!defaultDb.isNullOrBlank()) listOf(defaultDb) else listOf("test")
            }

            client = newClient
            currentUri = uri

            Result.success(
                ConnectionState.Connected(
                    uri = uri,
                    serverVersion = serverVersion,
                    databases = dbNames,
                    pingMs = pingMs
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

            // If user did not provide an update operator (e.g. $set), wrap it in $set
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

    fun disconnect() {
        try {
            client?.close()
        } catch (_: Exception) {
        } finally {
            client = null
        }
    }
}
