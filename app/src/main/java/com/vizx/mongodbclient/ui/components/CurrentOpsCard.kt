package com.vizx.mongodbclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vizx.mongodbclient.data.ActiveOperation

@Composable
fun CurrentOpsCard(
    operations: List<ActiveOperation>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onKillOp: (Long) -> Unit
) {
    var opToKill by remember { mutableStateOf<Long?>(null) }

    SkeletonCard(
        title = "Active Operations Profiler (${operations.size})",
        trailingAction = {
            Text(
                text = if (isLoading) "[INSPECTING...]" else "[INSPECT CURRENT OP]",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.Success,
                modifier = Modifier.clickable(enabled = !isLoading) { onRefresh() }
            )
        }
    ) {
        // Kill Op Confirmation Alert
        if (opToKill != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2B0A0A), RectangleShape)
                    .border(1.dp, SkeletonTheme.Error, RectangleShape)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kill Operation #$opToKill?",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkeletonTheme.Error
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "[CONFIRM KILL]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = SkeletonTheme.Error,
                        modifier = Modifier.clickable {
                            onKillOp(opToKill!!)
                            opToKill = null
                        }
                    )
                    Text(
                        text = "[CANCEL]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = SkeletonTheme.TextSecondary,
                        modifier = Modifier.clickable { opToKill = null }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (operations.isEmpty()) {
            Text(
                text = "No active running operations captured. Tap [INSPECT CURRENT OP] to refresh.",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SkeletonTheme.TextDisabled
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                operations.forEach { op ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SkeletonTheme.SurfaceElevated, RectangleShape)
                            .border(1.dp, SkeletonTheme.Border, RectangleShape)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OpID: ${op.opId} [${op.op.uppercase()}]",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = SkeletonTheme.TextPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${op.secsRunning}s running",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = if (op.secsRunning > 5) SkeletonTheme.Warning else SkeletonTheme.TextSecondary
                                )
                                Text(
                                    text = "[KILL]",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = SkeletonTheme.Error,
                                    modifier = Modifier.clickable { opToKill = op.opId }
                                )
                            }
                        }

                        Text(
                            text = "Namespace: ${op.ns}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = SkeletonTheme.TextSecondary
                        )

                        if (op.queryJson != "{}") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = op.queryJson,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = SkeletonTheme.TextDisabled,
                                maxLines = 4
                            )
                        }
                    }
                }
            }
        }
    }
}
