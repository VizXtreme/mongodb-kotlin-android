package com.vizx.mongodbclient.dns

import android.util.Log
import com.mongodb.spi.dns.DnsClient
import com.mongodb.spi.dns.DnsException
import org.json.JSONObject
import org.xbill.DNS.Lookup
import org.xbill.DNS.Record
import org.xbill.DNS.SRVRecord
import org.xbill.DNS.TXTRecord
import org.xbill.DNS.Type
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Custom DNS Client for MongoDB Driver on Android.
 * Resolves SRV and TXT records required for mongodb+srv:// connection strings
 * without relying on javax.naming (which is absent in the Android runtime).
 *
 * Implements a dual-layer resolution engine:
 * 1. DNS-over-HTTPS (DoH via Google DNS & Cloudflare DNS) - highly reliable across mobile carrier networks.
 * 2. Pure Java UDP/TCP DNS lookup via dnsjava as a robust fallback.
 */
class AndroidDnsClient : DnsClient {

    companion object {
        private const val TAG = "AndroidDnsClient"
    }

    override fun getResourceRecordData(name: String, type: String): List<String> {
        val trimmedName = name.trim().removeSuffix(".")
        Log.d(TAG, "Resolving DNS record: name='$trimmedName', type='$type'")

        // 1. Try DNS-over-HTTPS (DoH) first
        try {
            val dohResults = resolveViaDoH(trimmedName, type)
            if (dohResults.isNotEmpty()) {
                Log.d(TAG, "DoH resolved ${dohResults.size} records for '$trimmedName'")
                return dohResults
            }
        } catch (e: Throwable) {
            Log.w(TAG, "DoH resolution failed for '$trimmedName': ${e.message}, falling back to dnsjava")
        }

        // 2. Fallback to dnsjava
        try {
            val dnsJavaResults = resolveViaDnsJava(trimmedName, type)
            if (dnsJavaResults.isNotEmpty()) {
                Log.d(TAG, "dnsjava resolved ${dnsJavaResults.size} records for '$trimmedName'")
                return dnsJavaResults
            }
        } catch (e: Throwable) {
            Log.e(TAG, "dnsjava resolution failed for '$trimmedName': ${e.message}", e)
        }

        Log.w(TAG, "No records found for name='$trimmedName', type='$type'")
        return emptyList()
    }

    private fun resolveViaDoH(name: String, type: String): List<String> {
        val dohEndpoints = listOf(
            "https://dns.google/resolve?name=${URLEncoder.encode(name, "UTF-8")}&type=$type",
            "https://cloudflare-dns.com/dns-query?name=${URLEncoder.encode(name, "UTF-8")}&type=$type"
        )

        for (endpoint in dohEndpoints) {
            try {
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("Accept", "application/dns-json")

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val json = JSONObject(response)
                    val status = json.optInt("Status", -1)
                    if (status == 0 && json.has("Answer")) {
                        val answers = json.getJSONArray("Answer")
                        val results = mutableListOf<String>()
                        for (i in 0 until answers.length()) {
                            val answerObj = answers.getJSONObject(i)
                            var data = answerObj.getString("data").trim()
                            if (type.equals("TXT", ignoreCase = true)) {
                                // Strip wrapping quotes from TXT strings if present
                                if (data.startsWith("\"") && data.endsWith("\"") && data.length >= 2) {
                                    data = data.substring(1, data.length - 1)
                                }
                            }
                            results.add(data)
                        }
                        if (results.isNotEmpty()) {
                            return results
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Endpoint $endpoint failed: ${e.message}")
            }
        }
        return emptyList()
    }

    private fun resolveViaDnsJava(name: String, type: String): List<String> {
        val dnsType = when (type.uppercase()) {
            "SRV" -> Type.SRV
            "TXT" -> Type.TXT
            else -> Type.value(type.uppercase())
        }

        val lookup = Lookup(name, dnsType)
        val records: Array<Record>? = lookup.run()

        if (records == null || lookup.result != Lookup.SUCCESSFUL) {
            return emptyList()
        }

        val results = mutableListOf<String>()
        for (record in records) {
            when (record) {
                is SRVRecord -> {
                    // Expected format: "priority weight port target."
                    val target = record.target.toString()
                    val formatted = "${record.priority} ${record.weight} ${record.port} $target"
                    results.add(formatted)
                }
                is TXTRecord -> {
                    val txt = record.strings.joinToString("")
                    results.add(txt)
                }
                else -> {
                    results.add(record.rdataToString())
                }
            }
        }
        return results
    }
}
