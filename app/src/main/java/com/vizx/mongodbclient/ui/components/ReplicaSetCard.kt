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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vizx.mongodbclient.data.ReplicaSetInfo

@Composable
fun ReplicaSetCard(
    replicaSetInfo: ReplicaSetInfo?,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    SkeletonCard(
        title = "Replica Set Topology Inspector",
        trailingAction = {
            Text(
                text = if (isLoading) "[INSPECTING...]" else "[CHECK RS STATUS]",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.Success,
                modifier = Modifier.clickable(enabled = !isLoading) { onRefresh() }
            )
        }
    ) {
        if (replicaSetInfo == null) {
            Text(
                text = "Tap [CHECK RS STATUS] to probe replica set topology and member health.",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SkeletonTheme.TextDisabled
            )
        } else if (!replicaSetInfo.isReplicaSet) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SkeletonBadge(text = "[STANDALONE / SHARD ROUTER]", color = SkeletonTheme.Warning)
                Text(
                    text = "This instance is running as a Standalone mongod or through a mongos cluster router. 'replSetGetStatus' is not active.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SkeletonTheme.TextSecondary
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // RS Header summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SET: ${replicaSetInfo.setName}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SkeletonTheme.TextPrimary
                    )
                    Text(
                        text = "PRIMARY: ${replicaSetInfo.primaryHost ?: "ELECTING"}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = SkeletonTheme.Success
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Member list
                replicaSetInfo.members.forEach { member ->
                    val stateColor = when (member.stateStr.uppercase()) {
                        "PRIMARY" -> SkeletonTheme.Success
                        "SECONDARY" -> SkeletonTheme.Info
                        "ARBITER" -> SkeletonTheme.Warning
                        else -> SkeletonTheme.TextSecondary
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SkeletonTheme.SurfaceElevated, RectangleShape)
                            .border(1.dp, SkeletonTheme.Border, RectangleShape)
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = member.name + if (member.isSelf) " (active)" else "",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = SkeletonTheme.TextPrimary
                            )
                            SkeletonBadge(text = "[${member.stateStr}]", color = stateColor)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Health: ${if (member.health > 0.0) "HEALTHY (1.0)" else "DOWN (0.0)"}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = if (member.health > 0.0) SkeletonTheme.Success else SkeletonTheme.Error
                            )
                            if (member.pingMs > 0) {
                                Text(
                                    text = "Ping: ${member.pingMs}ms",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = SkeletonTheme.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
