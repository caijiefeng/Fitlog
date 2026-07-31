package com.example.fitlog.feature.avatar

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogDivider
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.domain.avatar.AvatarType
import com.example.fitlog.domain.avatar.BuiltInAvatar
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Avatar picker: a large preview on top, a 3-column grid of built-in sports
 * star avatars grouped by sport, and a section to pick a photo from the
 * phone. Tapping an avatar updates the preview immediately; "保存头像"
 * persists the pending selection, then navigates back.
 */
@Composable
fun AvatarPickerScreen(
    viewModel: AvatarPickerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val copyFailedMessage = stringResource(R.string.avatar_picker_photo_copy_failed)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            try {
                val path = copyAvatarToInternalStorage(context, uri)
                viewModel.selectCustom(path)
            } catch (_: Exception) {
                viewModel.onError(copyFailedMessage)
            }
        }
    }

    // Navigate back once the selection has been persisted.
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            FitLogTopAppBar(
                title = stringResource(R.string.avatar_picker_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = FitLogTextPrimary,
                        )
                    }
                },
            )
        },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AvatarPreview(
                avatarType = uiState.avatarType,
                avatarKey = uiState.avatarKey,
                customAvatarPath = uiState.customAvatarPath,
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(stringResource(R.string.avatar_picker_section_basketball))
                }
                items(BuiltInAvatar.BASKETBALL, key = { it.key }) { avatar ->
                    BuiltInAvatarItem(
                        avatar = avatar,
                        isSelected = uiState.avatarType == AvatarType.BUILT_IN &&
                            uiState.avatarKey == avatar.key,
                        onClick = { viewModel.selectBuiltIn(avatar) },
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(stringResource(R.string.avatar_picker_section_soccer))
                }
                items(BuiltInAvatar.SOCCER, key = { it.key }) { avatar ->
                    BuiltInAvatarItem(
                        avatar = avatar,
                        isSelected = uiState.avatarType == AvatarType.BUILT_IN &&
                            uiState.avatarKey == avatar.key,
                        onClick = { viewModel.selectBuiltIn(avatar) },
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(stringResource(R.string.avatar_picker_section_custom))
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .heightIn(min = 48.dp),
                        border = BorderStroke(1.dp, FitLogAccent),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !uiState.isSaving,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = FitLogAccent,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.avatar_picker_from_photos),
                            color = FitLogAccent,
                        )
                    }
                }

                uiState.error?.let { error ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = FitLogError,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Button(
                        onClick = { viewModel.save() },
                        enabled = !uiState.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 20.dp)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(text = stringResource(R.string.avatar_picker_save))
                    }
                }
            }
        }
    }
}

/** Large circular preview of the pending avatar with its name below. */
@Composable
private fun AvatarPreview(
    avatarType: AvatarType,
    avatarKey: String?,
    customAvatarPath: String?,
) {
    val builtIn = BuiltInAvatar.byKey(avatarKey)
    val label = when {
        avatarType == AvatarType.BUILT_IN && builtIn != null ->
            stringResource(builtIn.labelRes)
        avatarType == AvatarType.CUSTOM -> stringResource(R.string.avatar_picker_section_custom)
        else -> stringResource(R.string.avatar_default)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            AvatarImage(
                avatarType = avatarType,
                avatarKey = avatarKey,
                customAvatarPath = customAvatarPath,
                contentDescription = label,
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(FitLogSurfaceVariant),
            )
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .border(2.dp, FitLogDivider, CircleShape),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = FitLogTextPrimary,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = FitLogTextPrimary,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
    )
}

@Composable
private fun BuiltInAvatarItem(
    avatar: BuiltInAvatar,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(avatar.labelRes)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(avatar.drawableRes),
                contentDescription = label,
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            // Selection ring + check badge.
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth()
                        .border(3.dp, FitLogAccent, CircleShape),
                )
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = FitLogAccent,
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.TopEnd)
                        .background(FitLogBackground, CircleShape),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) FitLogAccent else FitLogTextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Copies a photo picked from the system photo picker into app-internal
 * storage and returns the relative path (from [Context.filesDir]) to persist
 * in `user_profiles.custom_avatar_path`.
 */
private fun copyAvatarToInternalStorage(context: Context, uri: Uri): String {
    val extension = context.contentResolver.getType(uri)?.let { mime ->
        when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
            else -> null
        }
    } ?: "jpg"

    val avatarsDir = File(context.filesDir, "avatars").apply { mkdirs() }
    val target = File(avatarsDir, "avatar_${System.currentTimeMillis()}.$extension")

    val input = context.contentResolver.openInputStream(uri)
        ?: throw IOException("Cannot open picked image")
    input.use { source ->
        FileOutputStream(target).use { out ->
            source.copyTo(out)
        }
    }
    return "avatars/${target.name}"
}
