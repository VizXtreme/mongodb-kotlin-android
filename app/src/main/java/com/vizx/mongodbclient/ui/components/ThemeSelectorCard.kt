package com.vizx.mongodbclient.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vizx.mongodbclient.data.AppTheme

@Composable
fun ThemeSelectorCard(
    currentTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    SkeletonCard(
        title = "App Theme & Appearance [${currentTheme.label}]",
        trailingAction = {
            Text(
                text = if (expanded) "[- HIDE]" else "[+ CHANGE]",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.Success,
                modifier = Modifier.clickable { expanded = !expanded }
            )
        }
    ) {
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppTheme.entries.take(2).forEach { theme ->
                        val isSelected = theme == currentTheme
                        SkeletonButton(
                            text = if (isSelected) "● ${theme.label}" else "○ ${theme.label}",
                            onClick = { onSelectTheme(theme) },
                            variant = if (isSelected) ButtonVariant.SUCCESS else ButtonVariant.PRIMARY,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppTheme.entries.drop(2).forEach { theme ->
                        val isSelected = theme == currentTheme
                        SkeletonButton(
                            text = if (isSelected) "● ${theme.label}" else "○ ${theme.label}",
                            onClick = { onSelectTheme(theme) },
                            variant = if (isSelected) ButtonVariant.SUCCESS else ButtonVariant.PRIMARY,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
