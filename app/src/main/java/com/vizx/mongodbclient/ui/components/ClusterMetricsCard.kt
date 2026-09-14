package com.vizx.mongodbclient.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vizx.mongodbclient.data.ServerStatusMetrics

@Composable
fun ClusterMetricsCard(
    metrics: ServerStatusMetrics?,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    SkeletonCard(
        title = "Real-Time Server Status & Diagnostics",
        trailingAction = {
            Text(
                text = if (isLoading) "[POLLING...]" else "[POLL SERVER STATUS]",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.Success,
                modifier = Modifier.clickable(enabled = !isLoading) { onRefresh() }
            )
        }
    ) {
        if (metrics == null) {
            Text(
                text = "Tap [POLL SERVER STATUS] to fetch live cluster telemetry.",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SkeletonTheme.TextDisabled
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Connections Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricTile("Active Conn", metrics.currentConnections.toString(), Modifier.weight(1f))
                    MetricTile("Avail Conn", metrics.availableConnections.toString(), Modifier.weight(1f))
                    MetricTile("Created Total", metrics.totalCreatedConnections.toString(), Modifier.weight(1f))
                }

                // Memory & Network Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricTile("Resident Mem", "${metrics.residentMemoryMb} MB", Modifier.weight(1f))
                    MetricTile("Virtual Mem", "${metrics.virtualMemoryMb} MB", Modifier.weight(1f))
                    MetricTile("Uptime", formatUptime(metrics.uptimeSeconds), Modifier.weight(1f))
                }

                // Network Throughput
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricTile("Network In", metrics.networkBytesInFormatted, Modifier.weight(1f))
                    MetricTile("Network Out", metrics.networkBytesOutFormatted, Modifier.weight(1f))
                    MetricTile("Commands", metrics.opcountersCommand.toString(), Modifier.weight(1f))
                }

                // Opcounters Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricTile("Inserts", metrics.opcountersInsert.toString(), Modifier.weight(1f))
                    MetricTile("Queries", metrics.opcountersQuery.toString(), Modifier.weight(1f))
                    MetricTile("Updates", metrics.opcountersUpdate.toString(), Modifier.weight(1f))
                    MetricTile("Deletes", metrics.opcountersDelete.toString(), Modifier.weight(1f))
                }
            }
        }
    }
}

private fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val mins = (seconds % 3600) / 60
    return if (days > 0) "${days}d ${hours}h" else "${hours}h ${mins}m"
}
