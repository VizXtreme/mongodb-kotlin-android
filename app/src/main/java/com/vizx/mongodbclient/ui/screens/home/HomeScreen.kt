package com.vizx.mongodbclient.ui.screens.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vizx.mongodbclient.data.CollectionSummary
import com.vizx.mongodbclient.data.ConnectionState
import com.vizx.mongodbclient.data.DatabaseStats
import com.vizx.mongodbclient.data.MongoOperation
import com.vizx.mongodbclient.ui.MongoUiState
import com.vizx.mongodbclient.ui.MongoViewModel
import com.vizx.mongodbclient.ui.components.ButtonVariant
import com.vizx.mongodbclient.ui.components.ClusterMetricsCard
import com.vizx.mongodbclient.ui.components.ConsoleLogViewer
import com.vizx.mongodbclient.ui.components.CurrentOpsCard
import com.vizx.mongodbclient.ui.components.DocumentResultCard
import com.vizx.mongodbclient.ui.components.IndexManagerCard
import com.vizx.mongodbclient.ui.components.MetricTile
import com.vizx.mongodbclient.ui.components.QueryOptionsCard
import com.vizx.mongodbclient.ui.components.ReplicaSetCard
import com.vizx.mongodbclient.ui.components.SkeletonBadge
import com.vizx.mongodbclient.ui.components.SkeletonButton
import com.vizx.mongodbclient.ui.components.SkeletonCard
import com.vizx.mongodbclient.ui.components.SkeletonTextField
import com.vizx.mongodbclient.ui.components.SkeletonTheme
import com.vizx.mongodbclient.ui.components.ThemeSelectorCard
import com.vizx.mongodbclient.ui.components.VisualQueryBuilderCard

@Composable
fun HomeScreen(
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
            // Top Navigation & Cluster Status
            HomeHeader(
                connectionState = uiState.connectionState,
                onPing = viewModel::ping,
                onDisconnect = viewModel::disconnect
            )

            Spacer(modifier = Modifier.height(10.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val isWideScreen = maxWidth >= 760.dp

                if (isWideScreen) {
                    // Two-Pane Master-Detail Responsive Layout for Tablets/Foldables/Landscape
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left Master Pane: Cluster info, Replica Set, Telemetry, DBs, Collections, Indexes
                        Column(
                            modifier = Modifier
                                .width(360.dp)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ThemeSelectorCard(
                                currentTheme = uiState.theme,
                                onSelectTheme = viewModel::onThemeSelected
                            )

                            val connected = uiState.connectionState as? ConnectionState.Connected
                            if (connected != null) {
                                ClusterTopologyCard(connected = connected)

                                ReplicaSetCard(
                                    replicaSetInfo = uiState.replicaSetInfo,
                                    isLoading = uiState.isLoadingReplicaSet,
                                    onRefresh = viewModel::loadReplicaSetStatus
                                )

                                ClusterMetricsCard(
                                    metrics = uiState.serverMetrics,
                                    isLoading = uiState.isLoadingMetrics,
                                    onRefresh = viewModel::loadServerMetrics
                                )

                                CurrentOpsCard(
                                    operations = uiState.activeOperations,
                                    isLoading = uiState.isLoadingOps,
                                    onRefresh = viewModel::loadCurrentOps,
                                    onKillOp = viewModel::killOp
                                )
                            }

                            DatabaseInspectorCard(
                                databases = uiState.databases,
                                selectedDb = uiState.selectedDatabase,
                                stats = uiState.databaseStats,
                                isRefreshing = uiState.isRefreshingStats,
                                onSelectDb = viewModel::onDatabaseSelected,
                                onRefresh = { viewModel.loadDatabaseDetails(uiState.selectedDatabase) }
                            )

                            CollectionsExplorerCard(
                                collections = uiState.collectionSummaries,
                                selectedCollection = uiState.selectedCollection,
                                onSelectCollection = viewModel::onCollectionSelected,
                                onCreateCollection = viewModel::createCollection,
                                onDropCollection = viewModel::dropCollection
                            )

                            if (uiState.selectedCollection.isNotEmpty()) {
                                IndexManagerCard(
                                    indexes = uiState.indexSummaries,
                                    selectedCollection = uiState.selectedCollection,
                                    onCreateIndex = viewModel::createIndex,
                                    onDropIndex = viewModel::dropIndex,
                                    onRefreshIndexes = {
                                        viewModel.loadIndexes(uiState.selectedDatabase, uiState.selectedCollection)
                                    }
                                )
                            }
                        }

                        // Right Detail Pane: CRUD operations, Visual query builder, Results, Console logs
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CrudOperationsCard(
                                selectedDb = uiState.selectedDatabase,
                                selectedCollection = uiState.selectedCollection,
                                activeOp = uiState.activeOperation,
                                filterJson = uiState.filterJson,
                                sortJson = uiState.sortJson,
                                projectionJson = uiState.projectionJson,
                                limit = uiState.limit,
                                skip = uiState.skip,
                                pipelineJson = uiState.pipelineJson,
                                insertJson = uiState.insertJson,
                                updateJson = uiState.updateJson,
                                isMultiple = uiState.isMultiple,
                                isLoading = uiState.isLoading,
                                isVisualBuilderMode = uiState.isVisualBuilderMode,
                                queryRules = uiState.queryRules,
                                onToggleVisualBuilder = viewModel::onToggleVisualBuilderMode,
                                onAddQueryRule = viewModel::onAddQueryRule,
                                onUpdateQueryRule = viewModel::onUpdateQueryRule,
                                onRemoveQueryRule = viewModel::onRemoveQueryRule,
                                onClearQueryRules = viewModel::onClearQueryRules,
                                onApplyVisualFilter = viewModel::onApplyVisualFilter,
                                onSelectOp = viewModel::onOperationSelected,
                                onFilterChange = viewModel::onFilterChange,
                                onSortChange = viewModel::onSortChange,
                                onProjectionChange = viewModel::onProjectionChange,
                                onLimitChange = viewModel::onLimitChange,
                                onSkipChange = viewModel::onSkipChange,
                                onPipelineChange = viewModel::onPipelineChange,
                                onInsertChange = viewModel::onInsertChange,
                                onUpdateChange = viewModel::onUpdateChange,
                                onMultipleToggle = viewModel::onMultipleToggle,
                                onExecute = viewModel::executeOperation
                            )

                            ResultsViewerCard(
                                documents = uiState.queryResult.documents,
                                totalCount = uiState.queryResult.totalCount,
                                message = uiState.queryResult.message,
                                onEditDoc = viewModel::prepareEditDocument,
                                onDeleteDoc = viewModel::deleteSingleDocument
                            )

                            ConsoleLogViewer(
                                logs = uiState.logs,
                                onClear = viewModel::clearLogs,
                                height = 180.dp
                            )
                        }
                    }
                } else {
                    // Single Column Scroll for Mobile Portrait
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeSelectorCard(
                            currentTheme = uiState.theme,
                            onSelectTheme = viewModel::onThemeSelected
                        )

                        val connected = uiState.connectionState as? ConnectionState.Connected
                        if (connected != null) {
                            ClusterTopologyCard(connected = connected)

                            ReplicaSetCard(
                                replicaSetInfo = uiState.replicaSetInfo,
                                isLoading = uiState.isLoadingReplicaSet,
                                onRefresh = viewModel::loadReplicaSetStatus
                            )

                            ClusterMetricsCard(
                                metrics = uiState.serverMetrics,
                                isLoading = uiState.isLoadingMetrics,
                                onRefresh = viewModel::loadServerMetrics
                            )

                            CurrentOpsCard(
                                operations = uiState.activeOperations,
                                isLoading = uiState.isLoadingOps,
                                onRefresh = viewModel::loadCurrentOps,
                                onKillOp = viewModel::killOp
                            )
                        }

                        DatabaseInspectorCard(
                            databases = uiState.databases,
                            selectedDb = uiState.selectedDatabase,
                            stats = uiState.databaseStats,
                            isRefreshing = uiState.isRefreshingStats,
                            onSelectDb = viewModel::onDatabaseSelected,
                            onRefresh = { viewModel.loadDatabaseDetails(uiState.selectedDatabase) }
                        )

                        CollectionsExplorerCard(
                            collections = uiState.collectionSummaries,
                            selectedCollection = uiState.selectedCollection,
                            onSelectCollection = viewModel::onCollectionSelected,
                            onCreateCollection = viewModel::createCollection,
                            onDropCollection = viewModel::dropCollection
                        )

                        if (uiState.selectedCollection.isNotEmpty()) {
                            IndexManagerCard(
                                indexes = uiState.indexSummaries,
                                selectedCollection = uiState.selectedCollection,
                                onCreateIndex = viewModel::createIndex,
                                onDropIndex = viewModel::dropIndex,
                                onRefreshIndexes = {
                                    viewModel.loadIndexes(uiState.selectedDatabase, uiState.selectedCollection)
                                }
                            )
                        }

                        CrudOperationsCard(
                            selectedDb = uiState.selectedDatabase,
                            selectedCollection = uiState.selectedCollection,
                            activeOp = uiState.activeOperation,
                            filterJson = uiState.filterJson,
                            sortJson = uiState.sortJson,
                            projectionJson = uiState.projectionJson,
                            limit = uiState.limit,
                            skip = uiState.skip,
                            pipelineJson = uiState.pipelineJson,
                            insertJson = uiState.insertJson,
                            updateJson = uiState.updateJson,
                            isMultiple = uiState.isMultiple,
                            isLoading = uiState.isLoading,
                            isVisualBuilderMode = uiState.isVisualBuilderMode,
                            queryRules = uiState.queryRules,
                            onToggleVisualBuilder = viewModel::onToggleVisualBuilderMode,
                            onAddQueryRule = viewModel::onAddQueryRule,
                            onUpdateQueryRule = viewModel::onUpdateQueryRule,
                            onRemoveQueryRule = viewModel::onRemoveQueryRule,
                            onClearQueryRules = viewModel::onClearQueryRules,
                            onApplyVisualFilter = viewModel::onApplyVisualFilter,
                            onSelectOp = viewModel::onOperationSelected,
                            onFilterChange = viewModel::onFilterChange,
                            onSortChange = viewModel::onSortChange,
                            onProjectionChange = viewModel::onProjectionChange,
                            onLimitChange = viewModel::onLimitChange,
                            onSkipChange = viewModel::onSkipChange,
                            onPipelineChange = viewModel::onPipelineChange,
                            onInsertChange = viewModel::onInsertChange,
                            onUpdateChange = viewModel::onUpdateChange,
                            onMultipleToggle = viewModel::onMultipleToggle,
                            onExecute = viewModel::executeOperation
                        )

                        ResultsViewerCard(
                            documents = uiState.queryResult.documents,
                            totalCount = uiState.queryResult.totalCount,
                            message = uiState.queryResult.message,
                            onEditDoc = viewModel::prepareEditDocument,
                            onDeleteDoc = viewModel::deleteSingleDocument
                        )

                        ConsoleLogViewer(
                            logs = uiState.logs,
                            onClear = viewModel::clearLogs,
                            height = 140.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    connectionState: ConnectionState,
    onPing: () -> Unit,
    onDisconnect: () -> Unit
) {
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
                text = "MONGODB CLUSTER",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = SkeletonTheme.TextPrimary
            )
            val pingMs = (connectionState as? ConnectionState.Connected)?.pingMs ?: 0L
            Text(
                text = "PING: ${pingMs}ms [TEST]",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.Success,
                modifier = Modifier.clickable { onPing() }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBadge(text = "[CONNECTED]", color = SkeletonTheme.Success)
            SkeletonButton(
                text = "DISCONNECT",
                onClick = onDisconnect,
                variant = ButtonVariant.DANGER,
                modifier = Modifier.height(30.dp)
            )
        }
    }
}

@Composable
private fun ClusterTopologyCard(connected: ConnectionState.Connected) {
    val info = connected.clusterInfo
    SkeletonCard(title = "Cluster Topology & Environment") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Version:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = SkeletonTheme.TextSecondary)
                Text(connected.serverVersion, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SkeletonTheme.TextPrimary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Architecture:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = SkeletonTheme.TextSecondary)
                Text(info?.connectionMode ?: "Replica Set", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = SkeletonTheme.TextPrimary)
            }
            if (info != null && info.hosts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Active Nodes (${info.hosts.size}):", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = SkeletonTheme.TextSecondary)
                info.hosts.forEach { host ->
                    Text("• $host", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = SkeletonTheme.TextDisabled)
                }
            }
        }
    }
}

@Composable
private fun DatabaseInspectorCard(
    databases: List<String>,
    selectedDb: String,
    stats: DatabaseStats?,
    isRefreshing: Boolean,
    onSelectDb: (String) -> Unit,
    onRefresh: () -> Unit
) {
    SkeletonCard(
        title = "Database Inspector & Storage Stats",
        trailingAction = {
            Text(
                text = if (isRefreshing) "[REFRESHING...]" else "[REFRESH]",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.Success,
                modifier = Modifier.clickable(enabled = !isRefreshing) { onRefresh() }
            )
        }
    ) {
        DatabaseDropdown(
            databases = databases,
            selectedDb = selectedDb,
            onSelect = onSelectDb
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (stats != null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricTile("Collections", stats.collectionsCount.toString(), Modifier.weight(1f))
                    MetricTile("Objects / Docs", stats.objectsCount.toString(), Modifier.weight(1f))
                    MetricTile("Data Size", stats.dataSizeFormatted, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricTile("Storage Size", stats.storageSizeFormatted, Modifier.weight(1f))
                    MetricTile("Indexes Count", stats.indexesCount.toString(), Modifier.weight(1f))
                    MetricTile("Indexes Size", stats.indexSizeFormatted, Modifier.weight(1f))
                }
            }
        } else {
            Text(
                text = "Loading database statistics...",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SkeletonTheme.TextDisabled
            )
        }
    }
}

@Composable
private fun DatabaseDropdown(
    databases: List<String>,
    selectedDb: String,
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
                text = "ACTIVE DATABASE: ${selectedDb.ifEmpty { "SELECT" }}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SkeletonTheme.TextPrimary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            databases.forEach { db ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = db,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = if (db == selectedDb) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(db)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CollectionsExplorerCard(
    collections: List<CollectionSummary>,
    selectedCollection: String,
    onSelectCollection: (String) -> Unit,
    onCreateCollection: (String) -> Unit,
    onDropCollection: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCollName by remember { mutableStateOf("") }
    var showDropConfirm by remember { mutableStateOf(false) }

    SkeletonCard(
        title = "Collections Explorer (${collections.size})",
        trailingAction = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (showCreateDialog) "[- CANCEL]" else "[+ NEW]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SkeletonTheme.Success,
                    modifier = Modifier.clickable { showCreateDialog = !showCreateDialog }
                )
                if (selectedCollection.isNotEmpty()) {
                    Text(
                        text = "[DROP]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = SkeletonTheme.Error,
                        modifier = Modifier.clickable { showDropConfirm = true }
                    )
                }
            }
        }
    ) {
        // Inline Create Collection Input
        if (showCreateDialog) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonTextField(
                    value = newCollName,
                    onValueChange = { newCollName = it },
                    label = "Collection Name",
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                SkeletonButton(
                    text = "CREATE",
                    onClick = {
                        if (newCollName.isNotBlank()) {
                            onCreateCollection(newCollName)
                            newCollName = ""
                            showCreateDialog = false
                        }
                    },
                    variant = ButtonVariant.SUCCESS,
                    modifier = Modifier.height(48.dp)
                )
            }
        }

        // Inline Drop Confirmation
        if (showDropConfirm) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E0A0A), RectangleShape)
                    .border(1.dp, SkeletonTheme.Error, RectangleShape)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Drop '$selectedCollection'?",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkeletonTheme.Error
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "[CONFIRM DROP]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = SkeletonTheme.Error,
                        modifier = Modifier.clickable {
                            onDropCollection(selectedCollection)
                            showDropConfirm = false
                        }
                    )
                    Text(
                        text = "[CANCEL]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = SkeletonTheme.TextSecondary,
                        modifier = Modifier.clickable { showDropConfirm = false }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Collections Scroll List
        if (collections.isEmpty()) {
            Text(
                text = "No collections found in this database.",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SkeletonTheme.TextDisabled
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(collections) { coll ->
                    val isSelected = coll.name == selectedCollection
                    Row(
                        modifier = Modifier
                            .background(if (isSelected) SkeletonTheme.BorderFocused else SkeletonTheme.SurfaceElevated, RectangleShape)
                            .border(1.dp, if (isSelected) Color.White else SkeletonTheme.Border, RectangleShape)
                            .clickable { onSelectCollection(coll.name) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = coll.name,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else SkeletonTheme.TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[${coll.documentCount}]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = if (isSelected) SkeletonTheme.Success else SkeletonTheme.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CrudOperationsCard(
    selectedDb: String,
    selectedCollection: String,
    activeOp: MongoOperation,
    filterJson: String,
    sortJson: String,
    projectionJson: String,
    limit: Int,
    skip: Int,
    pipelineJson: String,
    insertJson: String,
    updateJson: String,
    isMultiple: Boolean,
    isLoading: Boolean,
    isVisualBuilderMode: Boolean,
    queryRules: List<com.vizx.mongodbclient.data.QueryFilterRule>,
    onToggleVisualBuilder: () -> Unit,
    onAddQueryRule: () -> Unit,
    onUpdateQueryRule: (com.vizx.mongodbclient.data.QueryFilterRule) -> Unit,
    onRemoveQueryRule: (String) -> Unit,
    onClearQueryRules: () -> Unit,
    onApplyVisualFilter: (String) -> Unit,
    onSelectOp: (MongoOperation) -> Unit,
    onFilterChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onProjectionChange: (String) -> Unit,
    onLimitChange: (Int) -> Unit,
    onSkipChange: (Int) -> Unit,
    onPipelineChange: (String) -> Unit,
    onInsertChange: (String) -> Unit,
    onUpdateChange: (String) -> Unit,
    onMultipleToggle: (Boolean) -> Unit,
    onExecute: () -> Unit
) {
    SkeletonCard(title = "Data Operations ($selectedDb.$selectedCollection)") {
        // Operation Tabs
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(MongoOperation.values()) { op ->
                val isSelected = activeOp == op
                Box(
                    modifier = Modifier
                        .background(if (isSelected) SkeletonTheme.BorderFocused else Color.Transparent, RectangleShape)
                        .border(1.dp, if (isSelected) Color.White else SkeletonTheme.Border, RectangleShape)
                        .clickable { onSelectOp(op) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = op.name,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else SkeletonTheme.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic CRUD Inputs
        when (activeOp) {
            MongoOperation.FIND -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isVisualBuilderMode) "MODE: VISUAL BUILDER" else "MODE: RAW JSON FILTER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkeletonTheme.TextPrimary
                    )
                    Text(
                        text = if (isVisualBuilderMode) "[SWITCH TO RAW JSON]" else "[SWITCH TO VISUAL BUILDER]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = SkeletonTheme.Success,
                        modifier = Modifier.clickable { onToggleVisualBuilder() }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                if (isVisualBuilderMode) {
                    VisualQueryBuilderCard(
                        rules = queryRules,
                        onAddRule = onAddQueryRule,
                        onUpdateRule = onUpdateQueryRule,
                        onRemoveRule = onRemoveQueryRule,
                        onClearRules = onClearQueryRules,
                        onApplyGeneratedFilter = onApplyVisualFilter
                    )
                } else {
                    SkeletonTextField(
                        value = filterJson,
                        onValueChange = onFilterChange,
                        label = "Filter Query JSON (e.g. {} or {\"status\": \"active\"})",
                        minLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                QueryOptionsCard(
                    sortJson = sortJson,
                    projectionJson = projectionJson,
                    limit = limit,
                    skip = skip,
                    onSortChange = onSortChange,
                    onProjectionChange = onProjectionChange,
                    onLimitChange = onLimitChange,
                    onSkipChange = onSkipChange
                )
            }
            MongoOperation.INSERT -> {
                SkeletonTextField(
                    value = insertJson,
                    onValueChange = onInsertChange,
                    label = "Document to Insert (BSON JSON)",
                    minLines = 4,
                    maxLines = 8
                )
            }
            MongoOperation.UPDATE -> {
                SkeletonTextField(
                    value = filterJson,
                    onValueChange = onFilterChange,
                    label = "Filter Query JSON to Match",
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonTextField(
                    value = updateJson,
                    onValueChange = onUpdateChange,
                    label = "Update JSON (auto-wraps in \$set if needed)",
                    minLines = 3
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isMultiple, onCheckedChange = onMultipleToggle)
                    Text("Update Many (affects all matching documents)", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = SkeletonTheme.TextPrimary)
                }
            }
            MongoOperation.DELETE -> {
                SkeletonTextField(
                    value = filterJson,
                    onValueChange = onFilterChange,
                    label = "Filter Query JSON to Delete",
                    minLines = 2
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isMultiple, onCheckedChange = onMultipleToggle)
                    Text("Delete Many (warning: removes all matches)", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = SkeletonTheme.Error)
                }
            }
            MongoOperation.AGGREGATE -> {
                SkeletonTextField(
                    value = pipelineJson,
                    onValueChange = onPipelineChange,
                    label = "Aggregation Pipeline Array (JSON)",
                    placeholder = "[\n  { \"\$match\": {} },\n  { \"\$group\": { \"_id\": \"\$status\", \"count\": { \"\$sum\": 1 } } }\n]",
                    minLines = 5,
                    maxLines = 10
                )
            }
            MongoOperation.COUNT -> {
                SkeletonTextField(
                    value = filterJson,
                    onValueChange = onFilterChange,
                    label = "Filter Query JSON for Document Count",
                    minLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        SkeletonButton(
            text = if (isLoading) "EXECUTING OPERATION..." else "EXECUTE ${activeOp.name}",
            onClick = onExecute,
            variant = when (activeOp) {
                MongoOperation.DELETE -> ButtonVariant.DANGER
                MongoOperation.INSERT -> ButtonVariant.SUCCESS
                else -> ButtonVariant.PRIMARY
            },
            enabled = !isLoading && selectedDb.isNotBlank() && selectedCollection.isNotBlank(),
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ResultsViewerCard(
    documents: List<String>,
    totalCount: Long,
    message: String,
    onEditDoc: (String) -> Unit,
    onDeleteDoc: (String) -> Unit
) {
    val context = LocalContext.current

    SkeletonCard(
        title = "Query Results (${documents.size} displayed / $totalCount total)",
        trailingAction = {
            if (documents.isNotEmpty()) {
                Text(
                    text = "[COPY ALL]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SkeletonTheme.Info,
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val arrayJson = "[\n${documents.joinToString(",\n")}\n]"
                        clipboard.setPrimaryClip(ClipData.newPlainText("Mongo Query Results", arrayJson))
                        Toast.makeText(context, "All results copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    ) {
        if (message.isNotEmpty()) {
            Text(
                text = message,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.Success,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        if (documents.isEmpty()) {
            Text(
                text = "No documents returned.",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SkeletonTheme.TextDisabled,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                documents.forEachIndexed { idx, doc ->
                    DocumentResultCard(
                        index = idx,
                        documentJson = doc,
                        onEdit = onEditDoc,
                        onDelete = onDeleteDoc
                    )
                }
            }
        }
    }
}
