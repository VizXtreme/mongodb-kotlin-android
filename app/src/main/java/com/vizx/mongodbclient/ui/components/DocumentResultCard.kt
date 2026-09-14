package com.vizx.mongodbclient.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DocumentResultCard(
    index: Int,
    documentJson: String,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0C0C), RectangleShape)
            .border(1.dp, SkeletonTheme.Border, RectangleShape)
            .padding(8.dp)
    ) {
        // Document Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[#$index]",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = SkeletonTheme.TextSecondary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "[COPY]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SkeletonTheme.Info,
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Mongo Document", documentJson)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Document copied", Toast.LENGTH_SHORT).show()
                    }
                )

                Text(
                    text = "[EDIT]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SkeletonTheme.Success,
                    modifier = Modifier.clickable { onEdit(documentJson) }
                )

                Text(
                    text = "[DEL]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SkeletonTheme.Error,
                    modifier = Modifier.clickable { onDelete(documentJson) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Document Body
        Text(
            text = documentJson,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = SkeletonTheme.TextPrimary,
            lineHeight = 15.sp
        )
    }
}
