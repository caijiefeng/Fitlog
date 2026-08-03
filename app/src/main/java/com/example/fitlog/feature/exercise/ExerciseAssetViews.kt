package com.example.fitlog.feature.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.fitlog.core.designsystem.component.ExerciseMediaPanel
import com.example.fitlog.core.designsystem.theme.FitLogShapes
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.data.repository.ExerciseAssetRepository
import com.example.fitlog.domain.exercise.ExerciseAsset
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * 通过 Hilt EntryPoint 获取 [ExerciseAssetRepository]（供非注入的 Composable 使用）。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ExerciseAssetRepositoryEntryPoint {
    fun exerciseAssetRepository(): ExerciseAssetRepository
}

/**
 * 加载 builtInKey 对应的素材条目（整个 manifest 只解析一次，由仓库缓存）。
 */
@Composable
fun rememberExerciseAsset(builtInKey: String?): ExerciseAsset? {
    val context = LocalContext.current
    val repository = remember {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                ExerciseAssetRepositoryEntryPoint::class.java,
            ).exerciseAssetRepository()
        }.getOrNull()
    }
    var asset by remember(builtInKey) { mutableStateOf<ExerciseAsset?>(null) }
    LaunchedEffect(builtInKey) {
        asset = if (repository != null && builtInKey != null) {
            repository.getByBuiltInKey(builtInKey)
        } else {
            null
        }
    }
    return asset
}

/**
 * 按 builtInKey 加载动作缩略图；无素材/占位图/加载失败时回退到灰色底盒 + 图标。
 */
@Composable
fun ExerciseThumbnailByKey(
    builtInKey: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    shape: Shape = FitLogShapes.small,
) {
    val asset = rememberExerciseAsset(builtInKey)
    val hasImages = asset != null && !asset.isPlaceholder
    Box(
        modifier = modifier
            .clip(shape)
            .background(FitLogSurfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (hasImages) {
            AsyncImage(
                model = assetUri(asset.thumbnailPath),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = if (asset?.isPlaceholder == true) {
                    Icons.Filled.ImageNotSupported
                } else {
                    Icons.Filled.FitnessCenter
                },
                contentDescription = contentDescription,
                tint = FitLogTextSecondary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/**
 * 按 builtInKey 加载起始/结束姿势面板；占位或缺失时显示占位面板。
 */
@Composable
fun ExerciseStartEndImages(
    builtInKey: String?,
    modifier: Modifier = Modifier,
    contentDescription: String = "",
    fallbackText: String?,
    autoAlternate: Boolean = true,
) {
    val asset = rememberExerciseAsset(builtInKey)
    val hasImages = asset != null && !asset.isPlaceholder
    ExerciseMediaPanel(
        modifier = modifier,
        startModel = if (hasImages) assetUri(asset.startImagePath) else null,
        endModel = if (hasImages) assetUri(asset.endImagePath) else null,
        startLabel = "起始姿势",
        endLabel = "结束姿势",
        fallbackText = fallbackText,
        contentDescription = contentDescription,
        autoAlternate = autoAlternate,
    )
}

/** 占位面板（用于素材缺失时的回退展示） */
@Composable
fun ExerciseMediaFallback(
    fallbackText: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(FitLogShapes.card)
            .background(FitLogSurfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.ImageNotSupported,
                contentDescription = null,
                tint = FitLogTextSecondary,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = fallbackText,
                style = MaterialTheme.typography.bodySmall,
                color = FitLogTextSecondary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun assetUri(path: String): String = "file:///android_asset/$path"
