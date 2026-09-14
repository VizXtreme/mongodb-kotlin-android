package com.vizx.mongodbclient.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ConnectionStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mongo_connections_pref", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CONNECTIONS = "saved_connections"
        private const val DEFAULT_URI =
            "mongodb+srv://username:password@cluster0.ywgy3ll.mongodb.net/?appName=Cluster0"
    }

    fun getSavedConnections(): List<SavedConnection> {
        val raw = prefs.getString(KEY_CONNECTIONS, null) ?: return defaultList()
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<SavedConnection>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    SavedConnection(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Connection ${i + 1}"),
                        uri = obj.optString("uri", ""),
                        lastConnected = obj.optLong("lastConnected", System.currentTimeMillis())
                    )
                )
            }
            if (list.isEmpty()) defaultList() else list
        } catch (_: Exception) {
            defaultList()
        }
    }

    fun saveConnection(name: String, uri: String): SavedConnection {
        val current = getSavedConnections().toMutableList()
        // Check if URI already exists
        val existingIndex = current.indexOfFirst { it.uri.trim() == uri.trim() }
        val item = if (existingIndex >= 0) {
            val updated = current[existingIndex].copy(
                name = name.ifBlank { current[existingIndex].name },
                lastConnected = System.currentTimeMillis()
            )
            current[existingIndex] = updated
            updated
        } else {
            val newConn = SavedConnection(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { "Cluster (${current.size + 1})" },
                uri = uri.trim(),
                lastConnected = System.currentTimeMillis()
            )
            current.add(0, newConn)
            newConn
        }
        persist(current)
        return item
    }

    fun deleteConnection(id: String) {
        val current = getSavedConnections().filter { it.id != id }
        persist(current)
    }

    private fun persist(list: List<SavedConnection>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("uri", item.uri)
            obj.put("lastConnected", item.lastConnected)
            array.put(obj)
        }
        prefs.edit().putString(KEY_CONNECTIONS, array.toString()).apply()
    }

    private fun defaultList(): List<SavedConnection> {
        return listOf(
            SavedConnection(
                id = "default_atlas",
                name = "Atlas Cluster0 (Sample)",
                uri = DEFAULT_URI,
                lastConnected = System.currentTimeMillis()
            )
        )
    }
}
