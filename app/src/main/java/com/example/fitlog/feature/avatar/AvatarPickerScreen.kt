package com.example.fitlog.feature.avatar

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Avatar picker: a grid of the 12 built-in sports avatars plus an entry to
 * pick a photo from the phone. Selecting an avatar persists it immediately
 * and navigates back.
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

    // Navigate back once a selection has been persisted.
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
        Column(modifier = Modifier.padding(innerPadding)) {
            Text(
                text = stringResource(R.string.avatar_picker_section_builtin),
                style = MaterialTheme.typography.titleMedium,
                color = FitLogTextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(BuiltInAvatar.ALL, key = { it.key }) { avatar ->
                    BuiltInAvatarItem(
                        avatar = avatar,
                        isSelected = uiState.avatarType == AvatarType.BUILT_IN &&
                            uiState.avatarKey == avatar.key,
                        onClick = { viewModel.selectBuiltIn(avatar) },
                    )
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = FitLogError,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
    }
}

@Composable
private fun BuiltInAvatarItem(
    avatar: BuiltInAvatar,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(avatar.labelRes)
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) FitLogAccent else FitLogDivider,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) FitLogAccent.copy(alpha = 0.12f) else FitLogSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(avatar.drawableRes),
                contentDescription = label,
                modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) FitLogAccent else FitLogTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
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
