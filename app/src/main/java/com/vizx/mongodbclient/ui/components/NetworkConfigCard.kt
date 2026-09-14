package com.vizx.mongodbclient.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vizx.mongodbclient.data.NetworkConfig

@Composable
fun NetworkConfigCard(
    config: NetworkConfig,
    onConfigChange: (NetworkConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    SkeletonCard(
        title = "Network & Connection Pool Tuning",
        trailingAction = {
            Text(
                text = if (expanded) "[- COLLAPSE]" else "[+ EXPAND]",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.Success,
                modifier = Modifier.clickable { expanded = !expanded }
            )
        }
    ) {
        if (!expanded) {
            Text(
                text = "Timeouts: ${config.connectTimeoutSeconds}s connect / ${config.readTimeoutSeconds}s read | Pool: ${config.maxPoolSize} max | SSL Hostname verify: ${!config.allowInvalidHostnames}",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.TextSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkeletonTextField(
                        value = config.connectTimeoutSeconds.toString(),
                        onValueChange = { str ->
                            val num = str.filter { it.isDigit() }.toIntOrNull() ?: 15
                            onConfigChange(config.copy(connectTimeoutSeconds = num.coerceIn(1, 120)))
                        },
                        label = "Connect Timeout (s)",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    SkeletonTextField(
                        value = config.readTimeoutSeconds.toString(),
                        onValueChange = { str ->
                            val num = str.filter { it.isDigit() }.toIntOrNull() ?: 20
                            onConfigChange(config.copy(readTimeoutSeconds = num.coerceIn(1, 300)))
                        },
                        label = "Socket Read Timeout (s)",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkeletonTextField(
                        value = config.serverSelectionTimeoutSeconds.toString(),
                        onValueChange = { str ->
                            val num = str.filter { it.isDigit() }.toIntOrNull() ?: 15
                            onConfigChange(config.copy(serverSelectionTimeoutSeconds = num.coerceIn(1, 120)))
                        },
                        label = "Server Select Timeout (s)",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    SkeletonTextField(
                        value = config.maxPoolSize.toString(),
                        onValueChange = { str ->
                            val num = str.filter { it.isDigit() }.toIntOrNull() ?: 20
                            onConfigChange(config.copy(maxPoolSize = num.coerceIn(1, 500)))
                        },
                        label = "Max Pool Size",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = config.allowInvalidHostnames,
                        onCheckedChange = { onConfigChange(config.copy(allowInvalidHostnames = it)) }
                    )
                    Column {
                        Text(
                            text = "Allow Invalid TLS/SSL Hostnames",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (config.allowInvalidHostnames) SkeletonTheme.Warning else SkeletonTheme.TextPrimary
                        )
                        Text(
                            text = "Enable only for test clusters or self-signed certs",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = SkeletonTheme.TextDisabled
                        )
                    }
                }
            }
        }
    }
}
