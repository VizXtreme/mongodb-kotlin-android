package com.vizx.mongodbclient.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QueryOptionsCard(
    sortJson: String,
    projectionJson: String,
    limit: Int,
    skip: Int,
    onSortChange: (String) -> Unit,
    onProjectionChange: (String) -> Unit,
    onLimitChange: (Int) -> Unit,
    onSkipChange: (Int) -> Unit
) {
    SkeletonCard(title = "Query Modifiers (Sort / Proj / Limit / Skip)") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkeletonTextField(
                value = sortJson,
                onValueChange = onSortChange,
                label = "Sort JSON (e.g. {\"_id\": -1})",
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            SkeletonTextField(
                value = projectionJson,
                onValueChange = onProjectionChange,
                label = "Projection (e.g. {\"name\": 1})",
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonTextField(
                value = limit.toString(),
                onValueChange = { str ->
                    val num = str.filter { it.isDigit() }.toIntOrNull() ?: 20
                    onLimitChange(num.coerceIn(1, 1000))
                },
                label = "Limit (1-1000)",
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            SkeletonTextField(
                value = skip.toString(),
                onValueChange = { str ->
                    val num = str.filter { it.isDigit() }.toIntOrNull() ?: 0
                    onSkipChange(num.coerceAtLeast(0))
                },
                label = "Skip (Offset)",
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
