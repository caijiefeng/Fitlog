package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fitlog.core.designsystem.theme.FitLogSpacing
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import com.example.fitlog.core.designsystem.theme.FitLogType

/**
 * 加载骨架屏：静态柔和高亮块（稳定渲染，便于截图测试）。
 */
@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    lines: Int = 3,
    showThumbnail: Boolean = false,
    showHeader: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FitLogSpacing.M),
    ) {
        if (showHeader) {
            SkeletonBlock(widthFraction = 0.4f, height = 24)
        }
        repeat(lines) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FitLogSurfaceVariant, RoundedCornerShape(16)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showThumbnail) {
                    Box(
                        modifier = Modifier
                            .padding(FitLogSpacing.M)
                            .size(48.dp)
                            .background(
                                FitLogSurfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(8),
                            ),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = if (showThumbnail) 0.dp else FitLogSpacing.L,
                            top = FitLogSpacing.L,
                            end = FitLogSpacing.L,
                            bottom = FitLogSpacing.L,
                        ),
                    verticalArrangement = Arrangement.spacedBy(FitLogSpacing.S),
                ) {
                    SkeletonBlock(widthFraction = if (index == 0) 0.6f else 0.85f, height = 14)
                    SkeletonBlock(widthFraction = 0.4f, height = 12)
                }
            }
        }
    }
}

@Composable
private fun SkeletonBlock(widthFraction: Float, height: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height.dp)
            .background(FitLogSurfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(6)),
    )
}

@Preview(name = "LoadingSkeleton 浅色", showBackground = true, widthDp = 412)
@Composable
private fun LoadingSkeletonPreview() {
    FitLogTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            LoadingSkeleton(showHeader = true)
        }
    }
}

@Preview(name = "LoadingSkeleton 深色+缩略图", showBackground = true, widthDp = 360)
@Composable
private fun LoadingSkeletonPreviewDark() {
    FitLogTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            LoadingSkeleton(showThumbnail = true)
        }
    }
}
