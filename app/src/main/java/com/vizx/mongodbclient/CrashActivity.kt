package com.vizx.mongodbclient

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vizx.mongodbclient.ui.components.ButtonVariant
import com.vizx.mongodbclient.ui.components.SkeletonBadge
import com.vizx.mongodbclient.ui.components.SkeletonButton
import com.vizx.mongodbclient.ui.components.SkeletonCard
import com.vizx.mongodbclient.ui.components.SkeletonTheme

class CrashActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ERROR_CLASS = "extra_error_class"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
        const val EXTRA_STACK_TRACE = "extra_stack_trace"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val errorClass = intent.getStringExtra(EXTRA_ERROR_CLASS) ?: "Unknown Throwable"
        val errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE) ?: "No message provided"
        val stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE) ?: "No stack trace available"

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = SkeletonTheme.Background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SkeletonTheme.Surface, RectangleShape)
                            .border(1.dp, SkeletonTheme.Error, RectangleShape)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SYSTEM ANOMALY DETECTED",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SkeletonTheme.Error
                            )
                            Text(
                                text = "CRASH DIAGNOSTIC CONSOLE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = SkeletonTheme.TextSecondary
                            )
                        }
                        SkeletonBadge(text = "[FATAL]", color = SkeletonTheme.Error)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SkeletonCard(title = "Exception Details") {
                            Text(
                                text = "Class: $errorClass",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = SkeletonTheme.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Message: $errorMessage",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = SkeletonTheme.Warning
                            )
                        }

                        SkeletonCard(title = "Full Stack Trace") {
                            val vScroll = rememberScrollState()
                            val hScroll = rememberScrollState()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .background(Color(0xFF0A0A0A), RectangleShape)
                                    .border(1.dp, SkeletonTheme.Border, RectangleShape)
                                    .padding(8.dp)
                                    .verticalScroll(vScroll)
                                    .horizontalScroll(hScroll)
                            ) {
                                Text(
                                    text = stackTrace,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = SkeletonTheme.TextPrimary,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkeletonButton(
                            text = "COPY STACK TRACE",
                            onClick = {
                                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Mongo App Crash", "$errorClass: $errorMessage\n\n$stackTrace")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(this@CrashActivity, "Crash log copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            variant = ButtonVariant.PRIMARY,
                            modifier = Modifier.weight(1f)
                        )

                        SkeletonButton(
                            text = "RESTART APP",
                            onClick = {
                                val restartIntent = Intent(this@CrashActivity, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                }
                                startActivity(restartIntent)
                                finish()
                            },
                            variant = ButtonVariant.SUCCESS,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
