package com.vizx.mongodbclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vizx.mongodbclient.data.ConnectionState
import com.vizx.mongodbclient.data.LogLevel
import com.vizx.mongodbclient.data.MongoOperation

// Skeleton monochrome styling: Clean, functional, zero flashy decorations
private val SkeletonBg = Color(0xFF121212)
private val SkeletonSurface = Color(0xFF1E1E1E)
private val SkeletonBorder = Color(0xFF333333)
private val SkeletonText = Color(0xFFE0E0E0)
private val SkeletonTextDim = Color(0xFF888888)
private val SkeletonGreen = Color(0xFF4CAF50)
private val SkeletonRed = Color(0xFFE57373)
private val SkeletonOrange = Color(0xFFFFB74D)

@Composable
fun MongoApp(viewModel: MongoViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SkeletonBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header Bar
            HeaderSection(state = uiState.connectionState)

            Spacer(modifier = Modifier.height(8.dp))

            // Main Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Connection Panel
                ConnectionPanel(
                    uri = uiState.uri,
                    connectionState = uiState.connectionState,
                    isLoading = uiState.isLoading,
                    onUriChange = viewModel::onUriChange,
                    onConnect = viewModel::connect,
                    onDisconnect = viewModel::disconnect
                )

                // Database & Collection Selectors (Only shown if connected)
                if (uiState.connectionState is ConnectionState.Connected) {
                    DatabaseCollectionPanel(
                        databases = uiState.databases,
                        selectedDb = uiState.selectedDatabase,
                        collections = uiState.collections,
                        selectedCollection = uiState.selectedCollection,
                        onSelectDb = viewModel::onDatabaseSelected,
                        onSelectCollection = viewModel::onCollectionSelected,
                        onRefresh = viewModel::refreshCollections
                    )

                    // CRUD Operations Panel
                    OperationsPanel(
                        activeOp = uiState.activeOperation,
                        filterJson = uiState.filterJson,
                        insertJson = uiState.insertJson,
                        updateJson = uiState.updateJson,
                        isMultiple = uiState.isMultiple,
                        isLoading = uiState.isLoading,
                        onSelectOp = viewModel::onOperationSelected,
                        onFilterChange = viewModel::onFilterChange,
                        onInsertChange = viewModel::onInsertChange,
                        onUpdateChange = viewModel::onUpdateChange,
                        onMultipleToggle = viewModel::onMultipleToggle,
                        onExecute = viewModel::executeOperation
                    )

                    // Results Panel
                    ResultsPanel(
                        documents = uiState.queryResult.documents,
                        message = uiState.queryResult.message
                    )
                }

                // Log Console
                ConsolePanel(
                    logs = uiState.logs,
                    onClear = viewModel::clearLogs
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(state: ConnectionState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkeletonSurface, RectangleShape)
            .border(1.dp, SkeletonBorder, RectangleShape)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MONGODB CLIENT // CORE",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = SkeletonText
        )

        val (statusText, statusColor) = when (state) {
            is ConnectionState.Disconnected -> "[DISCONNECTED]" to SkeletonTextDim
            is ConnectionState.Connecting -> "[CONNECTING...]" to SkeletonOrange
            is ConnectionState.Connected -> "[CONNECTED]" to SkeletonGreen
            is ConnectionState.Error -> "[ERROR]" to SkeletonRed
        }

        Text(
            text = statusText,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = statusColor
        )
    }
}

@Composable
private fun ConnectionPanel(
    uri: String,
    connectionState: ConnectionState,
    isLoading: Boolean,
    onUriChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkeletonSurface)
            .border(1.dp, SkeletonBorder)
            .padding(10.dp)
    ) {
        Text(
            text = "CONNECTION CONFIGURATION",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SkeletonTextDim
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = uri,
            onValueChange = onUriChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = SkeletonText),
            label = { Text("MongoDB URI", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
            singleLine = false,
            maxLines = 3,
            shape = RectangleShape
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isConnected = connectionState is ConnectionState.Connected

            Button(
                onClick = onConnect,
                enabled = !isLoading && !isConnected,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C5E3B)),
                modifier = Modifier.weight(1f)
            ) {
                if (isLoading && connectionState is ConnectionState.Connecting) {
                    CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp), color = SkeletonText, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("CONNECT", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }

            Button(
                onClick = onDisconnect,
                enabled = !isLoading && isConnected,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6E2828)),
                modifier = Modifier.weight(1f)
            ) {
                Text("DISCONNECT", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DatabaseCollectionPanel(
    databases: List<String>,
    selectedDb: String,
    collections: List<String>,
    selectedCollection: String,
    onSelectDb: (String) -> Unit,
    onSelectCollection: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkeletonSurface)
            .border(1.dp, SkeletonBorder)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DATABASE & COLLECTION",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SkeletonTextDim
            )

            Text(
                text = "[REFRESH]",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SkeletonGreen,
                modifier = Modifier.clickable { onRefresh() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Database Selector
            Box(modifier = Modifier.weight(1f)) {
                DropdownSelector(
                    label = "DB: ${selectedDb.ifEmpty { "Select" }}",
                    items = databases,
                    onSelect = onSelectDb
                )
            }

            // Collection Selector
            Box(modifier = Modifier.weight(1f)) {
                DropdownSelector(
                    label = "COLL: ${selectedCollection.ifEmpty { "Select" }}",
                    items = collections,
                    onSelect = onSelectCollection
                )
            }
        }
    }
}

@Composable
private fun DropdownSelector(
    label: String,
    items: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape
        ) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                maxLines = 1,
                color = SkeletonText
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (items.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("None", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                    onClick = { expanded = false }
                )
            } else {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        onClick = {
                            onSelect(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OperationsPanel(
    activeOp: MongoOperation,
    filterJson: String,
    insertJson: String,
    updateJson: String,
    isMultiple: Boolean,
    isLoading: Boolean,
    onSelectOp: (MongoOperation) -> Unit,
    onFilterChange: (String) -> Unit,
    onInsertChange: (String) -> Unit,
    onUpdateChange: (String) -> Unit,
    onMultipleToggle: (Boolean) -> Unit,
    onExecute: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkeletonSurface)
            .border(1.dp, SkeletonBorder)
            .padding(10.dp)
    ) {
        Text(
            text = "OPERATION (CRUD)",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SkeletonTextDim
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Operation Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MongoOperation.values().forEach { op ->
                val isSelected = activeOp == op
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSelected) Color(0xFF333333) else Color.Transparent)
                        .border(1.dp, if (isSelected) Color.White else SkeletonBorder)
                        .clickable { onSelectOp(op) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = op.name,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else SkeletonTextDim
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic Input fields based on selected operation
        when (activeOp) {
            MongoOperation.FIND -> {
                OutlinedTextField(
                    value = filterJson,
                    onValueChange = onFilterChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Filter JSON (e.g. {})", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = SkeletonText),
                    shape = RectangleShape
                )
            }
            MongoOperation.INSERT -> {
                OutlinedTextField(
                    value = insertJson,
                    onValueChange = onInsertChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Document JSON", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = SkeletonText),
                    minLines = 4,
                    shape = RectangleShape
                )
            }
            MongoOperation.UPDATE -> {
                OutlinedTextField(
                    value = filterJson,
                    onValueChange = onFilterChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Filter JSON", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = SkeletonText),
                    shape = RectangleShape
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = updateJson,
                    onValueChange = onUpdateChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Update Document / Fields", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = SkeletonText),
                    minLines = 3,
                    shape = RectangleShape
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isMultiple, onCheckedChange = onMultipleToggle)
                    Text("Update Many (affects all matching)", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = SkeletonText)
                }
            }
            MongoOperation.DELETE -> {
                OutlinedTextField(
                    value = filterJson,
                    onValueChange = onFilterChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Filter JSON to Delete", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = SkeletonText),
                    shape = RectangleShape
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isMultiple, onCheckedChange = onMultipleToggle)
                    Text("Delete Many (caution: deletes all matching)", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = SkeletonRed)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onExecute,
            enabled = !isLoading,
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp), color = SkeletonText, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text("EXECUTE ${activeOp.name}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ResultsPanel(
    documents: List<String>,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkeletonSurface)
            .border(1.dp, SkeletonBorder)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "QUERY RESULTS",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SkeletonTextDim
            )
            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = SkeletonGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (documents.isEmpty()) {
            Text(
                text = "No documents returned.",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SkeletonTextDim,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                documents.forEachIndexed { index, docJson ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141414))
                            .border(1.dp, SkeletonBorder)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "[#$index]\n$docJson",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = SkeletonText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsolePanel(
    logs: List<com.vizx.mongodbclient.data.LogEntry>,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkeletonSurface)
            .border(1.dp, SkeletonBorder)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SYSTEM LOG CONSOLE",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SkeletonTextDim
            )
            Text(
                text = "[CLEAR]",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SkeletonTextDim,
                modifier = Modifier.clickable { onClear() }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFF0F0F0F))
                .border(1.dp, SkeletonBorder)
                .padding(6.dp)
        ) {
            if (logs.isEmpty()) {
                Text(
                    text = "Logs empty.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = SkeletonTextDim
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(logs) { entry ->
                        val color = when (entry.level) {
                            LogLevel.INFO -> SkeletonTextDim
                            LogLevel.SUCCESS -> SkeletonGreen
                            LogLevel.WARN -> SkeletonOrange
                            LogLevel.ERROR -> SkeletonRed
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
