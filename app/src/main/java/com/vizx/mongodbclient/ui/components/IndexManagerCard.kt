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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vizx.mongodbclient.data.IndexSummary

@Composable
fun IndexManagerCard(
    indexes: List<IndexSummary>,
    selectedCollection: String,
    onCreateIndex: (keys: String, unique: Boolean) -> Unit,
    onDropIndex: (String) -> Unit,
    onRefreshIndexes: () -> Unit
) {
    var showCreateForm by remember { mutableStateOf(false) }
    var keysJson by remember { mutableStateOf("{\n  \"createdAt\": -1\n}") }
    var isUnique by remember { mutableStateOf(false) }

    SkeletonCard(
        title = "Indexes ($selectedCollection: ${indexes.size})",
        trailingAction = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (showCreateForm) "[- CANCEL]" else "[+ NEW INDEX]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SkeletonTheme.Success,
                    modifier = Modifier.clickable { showCreateForm = !showCreateForm }
                )
                Text(
                    text = "[REFRESH]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SkeletonTheme.Info,
                    modifier = Modifier.clickable { onRefreshIndexes() }
                )
            }
        }
    ) {
        // Create Index Inline Form
        if (showCreateForm) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SkeletonTheme.SurfaceElevated, RectangleShape)
                    .border(1.dp, SkeletonTheme.BorderFocused, RectangleShape)
                    .padding(8.dp)
            ) {
                SkeletonTextField(
                    value = keysJson,
                    onValueChange = { keysJson = it },
                    label = "Index Keys (e.g. {\"email\": 1} or {\"status\": 1, \"createdAt\": -1})",
                    minLines = 2,
                    maxLines = 4
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isUnique, onCheckedChange = { isUnique = it })
                    Text("Unique Index Constraint", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = SkeletonTheme.TextPrimary)
                }

                Spacer(modifier = Modifier.height(4.dp))

                SkeletonButton(
                    text = "CONFIRM & CREATE INDEX",
                    onClick = {
                        onCreateIndex(keysJson, isUnique)
                        showCreateForm = false
                    },
                    variant = ButtonVariant.SUCCESS,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Indexes List
        if (indexes.isEmpty()) {
            Text(
                text = "No indexes loaded. Tap [REFRESH] to fetch.",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SkeletonTheme.TextDisabled
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                indexes.forEach { idx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SkeletonTheme.SurfaceElevated, RectangleShape)
                            .border(1.dp, SkeletonTheme.Border, RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = idx.name,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = SkeletonTheme.TextPrimary
                                )
                                if (idx.isUnique) {
                                    Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                                    SkeletonBadge(text = "[UNIQUE]", color = SkeletonTheme.Warning)
                                }
                            }
                            Text(
                                text = "Keys: ${idx.keys}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = SkeletonTheme.TextSecondary
                            )
                        }

                        if (idx.name != "_id_") {
                            Text(
                                text = "[DROP]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = SkeletonTheme.Error,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clickable { onDropIndex(idx.name) }
                            )
                        }
                    }
                }
            }
        }
    }
}
