package com.vizx.mongodbclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vizx.mongodbclient.data.LogEntry
import com.vizx.mongodbclient.data.LogLevel

@Composable
fun ConsoleLogViewer(
    logs: List<LogEntry>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp
) {
    SkeletonCard(
        title = "Diagnostic Console",
        trailingAction = {
            Text(
                text = "[CLEAR]",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.TextSecondary,
                modifier = Modifier.clickable { onClear() }
            )
        },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(Color(0xFF0C0C0C), RectangleShape)
                .border(1.dp, SkeletonTheme.Border, RectangleShape)
                .padding(6.dp)
        ) {
            if (logs.isEmpty()) {
                Text(
                    text = "Console empty.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = SkeletonTheme.TextDisabled
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(logs) { entry ->
                        val color = when (entry.level) {
                            LogLevel.INFO -> SkeletonTheme.TextSecondary
                            LogLevel.SUCCESS -> SkeletonTheme.Success
                            LogLevel.WARN -> SkeletonTheme.Warning
                            LogLevel.ERROR -> SkeletonTheme.Error
                        }
                        Text(
                            text = "[${entry.timestamp}] ${entry.message}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = color
                        )
                    }
                }
            }
        }
    }
}
