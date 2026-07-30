package com.example.fitlog.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import androidx.compose.material.icons.filled.ArrowBack
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.ScrollablePageContainer
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogSuccess
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    viewModel: DataManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showDeleteAllDialog by remember { mutableStateOf(false) }

    // ── SAF launchers ─────────────────────────────────────────────────────
    val workoutExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? -> uri?.let { viewModel.exportWorkoutsToUri(it) } }

    val measurementExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? -> uri?.let { viewModel.exportBodyMeasurementsToUri(it) } }

    val nutritionExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? -> uri?.let { viewModel.exportNutritionToUri(it) } }

    val checkInExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? -> uri?.let { viewModel.exportCheckInsToUri(it) } }

    val backupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri: Uri? -> uri?.let { viewModel.exportBackupToUri(it) } }

    val backupImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                if (inputStream != null) {
                    viewModel.importBackup(inputStream)
                }
            } catch (_: Exception) {
                // Error handled via viewModel state
            }
        }
    }

    // ── React to results ──────────────────────────────────────────────────
    LaunchedEffect(uiState.exportResult) {
        uiState.exportResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissExportResult()
        }
    }

    LaunchedEffect(uiState.backupExportResult) {
        uiState.backupExportResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissBackupExportResult()
        }
    }

    LaunchedEffect(uiState.importResult) {
        uiState.importResult?.let { result ->
            val message = when (result) {
                is com.example.fitlog.data.backup.ImportResult.Success -> result.message
                is com.example.fitlog.data.backup.ImportResult.Error -> context.getString(R.string.import_failed_format, result.message)
            }
            snackbarHostState.showSnackbar(message)
            viewModel.dismissImportResult()
        }
    }

    LaunchedEffect(uiState.cleanupResult) {
        uiState.cleanupResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissCleanupResult()
        }
    }

    LaunchedEffect(uiState.deleteAllResult) {
        uiState.deleteAllResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissDeleteAllResult()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // ── Delete-All confirmation dialog ────────────────────────────────────
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = {
                Text(stringResource(R.string.data_management_delete_all_confirm_title))
            },
            text = {
                Text(stringResource(R.string.data_management_delete_all_confirm_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllDialog = false
                        viewModel.deleteAllData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FitLogError,
                    ),
                ) {
                    Text(stringResource(R.string.data_management_delete_all_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.data_management_title),
                        color = FitLogTextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = FitLogTextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FitLogBackground,
                    titleContentColor = FitLogTextPrimary,
                    navigationIconContentColor = FitLogTextPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        ScrollablePageContainer(
            modifier = Modifier.padding(innerPadding),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Loading indicator ──────────────────────────────────────────
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                        .size(32.dp),
                    color = FitLogTextSecondary,
                )
            }

            // ── Media Storage Stats ────────────────────────────────────────
            Text(
                text = stringResource(R.string.media_stats_title),
                style = MaterialTheme.typography.titleMedium,
                color = FitLogTextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            FitLogCard(onClick = null) {
                val stats = uiState.storageStats
                if (stats != null) {
                    val sizeMb = if (stats.totalSizeBytes > 0) {
                        "%.1f MB".format(stats.totalSizeBytes / (1024.0 * 1024.0))
                    } else "0 B"

                    Text(
                        text = stringResource(R.string.media_stats_total, stats.totalCount, sizeMb),
                        style = MaterialTheme.typography.bodyLarge,
                        color = FitLogTextPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.media_stats_orphan_records, stats.orphanRecordCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = FitLogTextSecondary,
                    )
                    Text(
                        text = stringResource(R.string.media_stats_orphan_files, stats.orphanFileCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = FitLogTextSecondary,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.progress_placeholder),
                        style = MaterialTheme.typography.bodySmall,
                        color = FitLogTextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Cleanup ────────────────────────────────────────────────────
            FitLogCard(onClick = null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = FitLogTextSecondary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.media_cleanup_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = FitLogTextPrimary,
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.runCleanup() },
                        enabled = !uiState.isLoading,
                    ) {
                        Text(stringResource(R.string.media_cleanup_action))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── CSV Export ─────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.data_management_export),
                style = MaterialTheme.typography.titleMedium,
                color = FitLogTextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            ExportCard(
                label = stringResource(R.string.export_workouts),
                onClick = { workoutExportLauncher.launch("fitlog_workouts.csv") },
                enabled = !uiState.isLoading,
            )
            ExportCard(
                label = stringResource(R.string.export_body_measurements),
                onClick = { measurementExportLauncher.launch("fitlog_measurements.csv") },
                enabled = !uiState.isLoading,
            )
            ExportCard(
                label = stringResource(R.string.export_nutrition),
                onClick = { nutritionExportLauncher.launch("fitlog_nutrition.csv") },
                enabled = !uiState.isLoading,
            )
            ExportCard(
                label = stringResource(R.string.export_checkins),
                onClick = { checkInExportLauncher.launch("fitlog_checkins.csv") },
                enabled = !uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Backup & Restore ───────────────────────────────────────────
            Text(
                text = stringResource(R.string.data_management_backup),
                style = MaterialTheme.typography.titleMedium,
                color = FitLogTextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Create Backup
            FitLogCard(onClick = null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = FitLogTextSecondary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.backup_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = FitLogTextPrimary,
                        )
                    }
                    OutlinedButton(
                        onClick = { backupExportLauncher.launch("fitlog_backup.zip") },
                        enabled = !uiState.isLoading,
                    ) {
                        Text(stringResource(R.string.backup_title))
                    }
                }
            }

            // Restore Backup
            FitLogCard(onClick = null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = FitLogTextSecondary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.data_management_restore),
                            style = MaterialTheme.typography.bodyLarge,
                            color = FitLogTextPrimary,
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.import_confirm_message),
                                )
                            }
                            backupImportLauncher.launch("application/zip")
                        },
                        enabled = !uiState.isLoading,
                    ) {
                        Text(stringResource(R.string.data_management_restore))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Delete All Data ────────────────────────────────────────────
            FitLogCard(onClick = null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = FitLogError,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.data_management_delete_all),
                            style = MaterialTheme.typography.bodyLarge,
                            color = FitLogTextPrimary,
                        )
                    }
                    Button(
                        onClick = { showDeleteAllDialog = true },
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FitLogError,
                        ),
                    ) {
                        Text(stringResource(R.string.data_management_delete_all))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * A single-row card showing an export option.
 */
@Composable
private fun ExportCard(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    FitLogCard(
        modifier = Modifier.padding(vertical = 4.dp),
        onClick = null,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = FitLogTextPrimary,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
            ) {
                Text(stringResource(R.string.action_share))
            }
        }
    }
}
