package com.vizx.mongodbclient.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.vizx.mongodbclient.data.ConnectionState
import com.vizx.mongodbclient.ui.MongoUiState
import com.vizx.mongodbclient.ui.MongoViewModel
import com.vizx.mongodbclient.ui.components.ButtonVariant
import com.vizx.mongodbclient.ui.components.ConsoleLogViewer
import com.vizx.mongodbclient.ui.components.SkeletonBadge
import com.vizx.mongodbclient.ui.components.SkeletonButton
import com.vizx.mongodbclient.ui.components.SkeletonCard
import com.vizx.mongodbclient.ui.components.SkeletonTextField
import com.vizx.mongodbclient.ui.components.SkeletonTheme

@Composable
fun LoginScreen(
    uiState: MongoUiState,
    viewModel: MongoViewModel
) {
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
            LoginHeader(state = uiState.connectionState)

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Saved Profiles Section
                SavedProfilesCard(
                    saved = uiState.savedConnections,
                    onSelect = viewModel::selectSavedConnection,
                    onDelete = viewModel::deleteSavedConnection
                )

                // Credentials Input Card
                ConnectionInputCard(
                    profileName = uiState.profileName,
                    uri = uiState.uri,
                    isLoading = uiState.isLoading,
                    connectionState = uiState.connectionState,
                    onProfileNameChange = viewModel::onProfileNameChange,
                    onUriChange = viewModel::onUriChange,
                    onConnect = viewModel::connect
                )

                // Atlas Setup Helper Box
                AtlasSetupGuideCard()

                // Live Diagnostic Console
                ConsoleLogViewer(
                    logs = uiState.logs,
                    onClear = viewModel::clearLogs,
                    height = 140.dp
                )
            }
        }
    }
}

@Composable
private fun LoginHeader(state: ConnectionState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkeletonTheme.Surface, RectangleShape)
            .border(1.dp, SkeletonTheme.Border, RectangleShape)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "MONGODB CLIENT",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = SkeletonTheme.TextPrimary
            )
            Text(
                text = "CONNECTION GATEWAY",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.TextSecondary
            )
        }

        val (statusText, statusColor) = when (state) {
            is ConnectionState.Disconnected -> "[DISCONNECTED]" to SkeletonTheme.TextSecondary
            is ConnectionState.Connecting -> "[CONNECTING...]" to SkeletonTheme.Warning
            is ConnectionState.Connected -> "[CONNECTED]" to SkeletonTheme.Success
            is ConnectionState.Error -> "[FAILED]" to SkeletonTheme.Error
        }

        SkeletonBadge(text = statusText, color = statusColor)
    }
}

@Composable
private fun SavedProfilesCard(
    saved: List<com.vizx.mongodbclient.data.SavedConnection>,
    onSelect: (com.vizx.mongodbclient.data.SavedConnection) -> Unit,
    onDelete: (String) -> Unit
) {
    SkeletonCard(title = "Saved Connection Profiles") {
        if (saved.isEmpty()) {
            Text(
                text = "No saved profiles yet.",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SkeletonTheme.TextDisabled
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(saved) { item ->
                    Row(
                        modifier = Modifier
                            .background(SkeletonTheme.SurfaceElevated, RectangleShape)
                            .border(1.dp, SkeletonTheme.Border, RectangleShape)
                            .clickable { onSelect(item) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SkeletonTheme.TextPrimary
                        )
                        if (saved.size > 1) {
                            Text(
                                text = " [x]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = SkeletonTheme.Error,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .clickable { onDelete(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionInputCard(
    profileName: String,
    uri: String,
    isLoading: Boolean,
    connectionState: ConnectionState,
    onProfileNameChange: (String) -> Unit,
    onUriChange: (String) -> Unit,
    onConnect: () -> Unit
) {
    SkeletonCard(title = "Cluster Connection Parameters") {
        SkeletonTextField(
            value = profileName,
            onValueChange = onProfileNameChange,
            label = "Profile Name (Optional)",
            placeholder = "e.g. Cluster0 Production",
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        SkeletonTextField(
            value = uri,
            onValueChange = onUriChange,
            label = "MongoDB Connection URI",
            placeholder = "mongodb+srv://user:password@host/db?appName=...",
            minLines = 3,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[PASTE SAMPLE ATLAS URI]",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.Info,
                modifier = Modifier.clickable {
                    onUriChange("mongodb+srv://username:password@cluster0.ywgy3ll.mongodb.net/?appName=Cluster0")
                }
            )

            Text(
                text = "[CLEAR URI]",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.TextSecondary,
                modifier = Modifier.clickable { onUriChange("") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        SkeletonButton(
            text = if (isLoading) "ESTABLISHING CONNECTION..." else "CONNECT TO CLUSTER",
            onClick = onConnect,
            variant = ButtonVariant.SUCCESS,
            enabled = !isLoading && uri.isNotBlank(),
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AtlasSetupGuideCard() {
    SkeletonCard(title = "Atlas Setup & Troubleshooting") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "• SRV Resolution: Powered by Android DNS-over-HTTPS (DoH) engine.",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.TextSecondary
            )
            Text(
                text = "• IP Access List: In MongoDB Atlas Dashboard -> 'Network Access', ensure your current IP or 0.0.0.0/0 (allow all) is whitelisted.",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.Warning
            )
            Text(
                text = "• Database User: Ensure user has readWriteAnyDatabase or appropriate role.",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.TextSecondary
            )
        }
    }
}
