package com.example.fitlog.feature.media

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.data.repository.MediaRecord
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    mediaId: Long,
    viewModel: MediaDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(mediaId) {
        viewModel.load(mediaId)
    }

    // Navigate back when delete completes
    LaunchedEffect(uiState.deleteCompleted) {
        if (uiState.deleteCompleted) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            FitLogTopAppBar(
                title = stringResource(R.string.media_detail_title),
                actions = {
                    if (uiState.record != null && !uiState.isLoading) {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (uiState.record?.isFavorite == true) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Filled.FavoriteBorder
                                },
                                contentDescription = stringResource(R.string.action_favorite),
                                tint = if (uiState.record?.isFavorite == true) {
                                    FitLogError
                                } else {
                                    FitLogTextSecondary
                                },
                            )
                        }
                        IconButton(onClick = { shareMedia(context, viewModel) }) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = stringResource(R.string.action_share),
                                tint = FitLogTextSecondary,
                            )
                        }
                        IconButton(onClick = { viewModel.showDeleteConfirmation() }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = FitLogError,
                            )
                        }
                    }
                },
            )
        },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = FitLogAccent)
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error ?: stringResource(R.string.media_loading_failed),
                            color = FitLogError,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.load(mediaId) },
                            colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                        ) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }
            uiState.record == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.media_not_found_label),
                        color = FitLogTextSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            else -> {
                val record = uiState.record!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Photo preview
                    MediaPhotoPreview(record = record)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metadata section
                    FitLogCard(modifier = Modifier.fillMaxWidth()) {
                        MetadataRow(
                            label = stringResource(R.string.media_detail_date),
                            value = formatDetailDate(record.capturedAt),
                        )
                        MetadataRow(
                            label = stringResource(R.string.media_detail_category),
                            value = record.category.name,
                        )
                        MetadataRow(
                            label = stringResource(R.string.media_detail_type),
                            value = if (record.mediaType.name == "PHOTO") {
                                stringResource(R.string.media_detail_photo)
                            } else {
                                stringResource(R.string.media_detail_video)
                            },
                        )
                        MetadataRow(
                            label = stringResource(R.string.media_detail_size),
                            value = formatFileSize(record.sizeBytes),
                        )
                        record.poseTag?.let { pose ->
                            MetadataRow(
                                label = stringResource(R.string.media_detail_pose),
                                value = pose.name,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Note section
                    NoteSection(
                        note = record.note,
                        isEditing = uiState.isEditingNote,
                        editText = uiState.editNoteText,
                        onStartEdit = { viewModel.startEditingNote() },
                        onTextChange = { viewModel.updateEditNoteText(it) },
                        onSave = { viewModel.saveNote() },
                        onCancel = { viewModel.cancelEditingNote() },
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirmation() },
            title = { Text(stringResource(R.string.media_delete_title)) },
            text = { Text(stringResource(R.string.media_delete_message)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = FitLogError),
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirmation() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun MediaPhotoPreview(record: MediaRecord) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(12.dp))
            .background(FitLogSurfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        // Show image icon as placeholder since actual bitmap loading
        // would require a proper image loading library call.
        // In production, use Coil: AsyncImage(model = File(relativePath), ...)
        Icon(
            Icons.Filled.Image,
            contentDescription = stringResource(R.string.media_detail_photo),
            tint = FitLogTextSecondary,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = record.relativePath.substringAfterLast("/"),
            style = MaterialTheme.typography.bodySmall,
            color = FitLogTextSecondary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp),
        )
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = FitLogTextSecondary,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = FitLogTextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f),
        )
    }
}

@Composable
private fun NoteSection(
    note: String?,
    isEditing: Boolean,
    editText: String,
    onStartEdit: () -> Unit,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    FitLogCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.media_note_label),
                style = MaterialTheme.typography.titleSmall,
                color = FitLogTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            if (!isEditing) {
                IconButton(onClick = onStartEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.media_note_add),
                        tint = FitLogAccent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (isEditing) {
            OutlinedTextField(
                value = editText,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = {
                    Text(
                        stringResource(R.string.media_note_add),
                        color = FitLogTextSecondary,
                    )
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        } else {
            Text(
                text = note ?: stringResource(R.string.media_note_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = if (note != null) FitLogTextPrimary else FitLogTextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private fun shareMedia(context: android.content.Context, viewModel: MediaDetailViewModel) {
    val file = viewModel.resolveFile() ?: return
    val record = viewModel.uiState.value.record ?: return
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = record.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    } catch (_: Exception) {
        // Silently fail share
    }
}

private val detailDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

private fun formatDetailDate(timestamp: Long): String {
    val instant = java.time.Instant.ofEpochMilli(timestamp)
    val localDateTime = java.time.LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return localDateTime.format(detailDateFormat)
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}
