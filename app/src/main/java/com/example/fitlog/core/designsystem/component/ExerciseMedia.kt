package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogDimensions
import com.example.fitlog.core.designsystem.theme.FitLogShapes
import com.example.fitlog.core.designsystem.theme.FitLogSpacing
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import com.example.fitlog.core.designsystem.theme.FitLogType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 动作缩略图：固定尺寸圆角图，图片缺失时回退到图标占位。
 */
@Composable
fun ExerciseThumbnail(
    modifier: Modifier = Modifier,
    painter: Painter? = null,
    contentDescription: String?,
    fallbackIcon: ImageVector = Icons.Filled.FitnessCenter,
    shape: Shape = FitLogShapes.small,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(FitLogSurfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = contentDescription,
                tint = FitLogTextSecondary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/**
 * 动作列表行：缩略图 + 名称 + 副标题 + 可选勾选/箭头。
 */
@Composable
fun ExerciseListItem(
    modifier: Modifier = Modifier,
    name: String,
    subtitle: String? = null,
    thumbnailPainter: Painter? = null,
    hasIllustration: Boolean = false,
    selected: Boolean = false,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    FitLogCard(
        modifier = modifier,
        style = if (selected) FitLogCardStyle.OUTLINED else FitLogCardStyle.STANDARD,
        selected = selected,
        onClick = onClick,
        leadingContent = {
            ExerciseThumbnail(
                painter = thumbnailPainter,
                contentDescription = name,
                modifier = Modifier.size(FitLogDimensions.listThumbnail),
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasIllustration && !selected) {
                    Icon(
                        imageVector = Icons.Filled.ImageNotSupported,
                        contentDescription = null,
                        tint = FitLogTextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(FitLogSpacing.S))
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = FitLogAccent,
                        modifier = Modifier.size(24.dp),
                    )
                } else if (showChevron) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = FitLogTextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
    ) {
        Text(
            text = name,
            style = FitLogType.body,
            color = FitLogTextPrimary,
            maxLines = 1,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = FitLogType.caption,
                color = FitLogTextSecondary,
                maxLines = 2,
            )
        }
    }
}

/**
 * 动作示意面板：起始姿势 / 结束姿势双图。
 *
 * - 左右滑动切换
 * - 点击下方标签切换
 * - 默认 3 秒缓慢自动交替（用户交互后暂停）
 * - 双指缩放（1x–4x）
 * - 图片缺失时显示 [fallbackText] 占位
 */
@Composable
fun ExerciseMediaPanel(
    modifier: Modifier = Modifier,
    startPainter: Painter? = null,
    endPainter: Painter? = null,
    startLabel: String,
    endLabel: String,
    fallbackText: String? = null,
    contentDescription: String,
    autoAlternate: Boolean = true,
) {
    val painters = listOf(startPainter, endPainter)
    val labels = listOf(startLabel, endLabel)
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var isUserActive by remember { mutableStateOf(false) }

    // 缓慢自动交替；用户交互（拖动/缩放）后暂停
    LaunchedEffect(autoAlternate, isUserActive) {
        if (!autoAlternate || isUserActive) return@LaunchedEffect
        while (true) {
            delay(3000)
            if (pagerState.isScrollInProgress) continue
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % 2)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(FitLogShapes.card)
            .background(FitLogSurfaceVariant),
    ) {
        if (startPainter == null && endPainter == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center),
            ) {
                Icon(
                    imageVector = Icons.Filled.ImageNotSupported,
                    contentDescription = null,
                    tint = FitLogTextSecondary,
                    modifier = Modifier.size(40.dp),
                )
                if (fallbackText != null) {
                    Spacer(modifier = Modifier.height(FitLogSpacing.S))
                    Text(
                        text = fallbackText,
                        style = FitLogType.caption,
                        color = FitLogTextSecondary,
                    )
                }
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val painter = painters[page]
                if (painter != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    if (zoom != 1f) {
                                        isUserActive = true
                                        scale = (scale * zoom).coerceIn(1f, 4f)
                                    }
                                }
                            }
                            .graphicsLayer {
                                val s = if (scale > 1f) scale else 1f
                                scaleX = s
                                scaleY = s
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painter,
                            contentDescription = contentDescription,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
            // 底部标签切换
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = FitLogSpacing.S),
            ) {
                labels.forEachIndexed { index, label ->
                    val active = pagerState.currentPage == index
                    Text(
                        text = label,
                        style = FitLogType.caption,
                        color = if (active) FitLogTextPrimary else FitLogTextSecondary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (active) FitLogAccent.copy(alpha = 0.18f)
                                else FitLogAccent.copy(alpha = 0.0f),
                            )
                            .clickable {
                                isUserActive = true
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Preview(name = "ExerciseListItem 浅色", showBackground = true, widthDp = 412)
@Composable
private fun ExerciseListItemPreviewLight() {
    FitLogTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ExerciseListItem(
                name = "杠铃卧推",
                subtitle = "胸部 · 杠铃 · 重量×次数",
                selected = true,
            ) {}
            Spacer(modifier = Modifier.height(8.dp))
            ExerciseListItem(
                name = "哑铃侧平举",
                subtitle = "肩部 · 哑铃 · 重量×次数",
            ) {}
        }
    }
}

@Preview(name = "ExerciseListItem 深色+长文本", showBackground = true, widthDp = 360)
@Composable
private fun ExerciseListItemPreviewDark() {
    FitLogTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            ExerciseListItem(
                name = "保加利亚分腿蹲（长名称动作示例）",
                subtitle = "股四头肌 · 哑铃 · 重量×次数 · 自定义标签",
            ) {}
        }
    }
}

@Preview(name = "ExerciseMediaPanel 浅色", showBackground = true, widthDp = 412)
@Composable
private fun ExerciseMediaPanelPreview() {
    FitLogTheme {
        ExerciseMediaPanel(
            startPainter = painterResource(com.example.fitlog.R.drawable.avatar_kobe),
            endPainter = painterResource(com.example.fitlog.R.drawable.avatar_messi),
            startLabel = "起始姿势",
            endLabel = "结束姿势",
            contentDescription = "动作姿势预览",
            fallbackText = "暂无动作示意图",
        )
    }
}

@Preview(name = "ExerciseMediaPanel 无图占位", showBackground = true, widthDp = 412)
@Composable
private fun ExerciseMediaPanelFallbackPreview() {
    FitLogTheme {
        ExerciseMediaPanel(
            startLabel = "起始姿势",
            endLabel = "结束姿势",
            contentDescription = "动作姿势预览",
            fallbackText = "暂无动作示意图",
        )
    }
}
