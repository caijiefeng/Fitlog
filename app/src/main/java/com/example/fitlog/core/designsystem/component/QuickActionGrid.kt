package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogAccentContainer
import com.example.fitlog.core.designsystem.theme.FitLogShapes
import com.example.fitlog.core.designsystem.theme.FitLogSpacing
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import com.example.fitlog.core.designsystem.theme.FitLogType

/** 快速操作项定义。 */
data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

/**
 * 快速操作 2×2 网格（替代旧的横向滚动），每格至少 48dp 触摸区域。
 */
@Composable
fun QuickActionGrid(
    actions: List<QuickAction>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
) {
    val rows = actions.chunked(columns)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FitLogSpacing.M),
    ) {
        rows.forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitLogSpacing.M),
            ) {
                rowActions.forEach { action ->
                    QuickActionTile(
                        action = action,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - rowActions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    action: QuickAction,
    modifier: Modifier = Modifier,
) {
    FitLogCard(
        modifier = modifier,
        style = FitLogCardStyle.TONAL,
        onClick = action.onClick,
        contentPadding = PaddingValues(
            horizontal = FitLogSpacing.M,
            vertical = FitLogSpacing.L,
        ),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(FitLogAccentContainer, FitLogShapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = FitLogAccent,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.height(FitLogSpacing.M))
        Text(
            text = action.label,
            style = FitLogType.caption,
            color = FitLogTextPrimary,
            maxLines = 1,
        )
    }
}

@Preview(name = "QuickActionGrid 浅色", showBackground = true, widthDp = 412)
@Composable
private fun QuickActionGridPreview() {
    FitLogTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            QuickActionGrid(
                actions = listOf(
                    QuickAction(Icons.Filled.FitnessCenter, "开始训练") {},
                    QuickAction(Icons.Filled.Restaurant, "饮食记录") {},
                    QuickAction(Icons.Filled.Straighten, "身体测量") {},
                    QuickAction(Icons.Filled.PhotoCamera, "拍照记录") {},
                ),
            )
        }
    }
}

@Preview(name = "QuickActionGrid 深色小屏", showBackground = true, widthDp = 360)
@Composable
private fun QuickActionGridPreviewDark() {
    FitLogTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            QuickActionGrid(
                actions = listOf(
                    QuickAction(Icons.Filled.FitnessCenter, "继续当前训练（已进行 37 分钟）") {},
                    QuickAction(Icons.Filled.Restaurant, "饮食记录") {},
                ),
            )
        }
    }
}
