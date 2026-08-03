package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogAccentContainer
import com.example.fitlog.core.designsystem.theme.FitLogSpacing
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import com.example.fitlog.core.designsystem.theme.FitLogType

/**
 * 插图式空状态：柔和圆形底 + 图标 + 标题/副标题 + 可选操作按钮。
 */
@Composable
fun EmptyStateIllustration(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.FitnessCenter,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = FitLogSpacing.XXXL, horizontal = FitLogSpacing.XXXL),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(FitLogAccentContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FitLogAccent,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(modifier = Modifier.height(FitLogSpacing.L))
        Text(
            text = title,
            style = FitLogType.body,
            color = FitLogTextPrimary,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(FitLogSpacing.S))
            Text(
                text = subtitle,
                style = FitLogType.caption,
                color = FitLogTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(FitLogSpacing.L))
            OutlinedButton(onClick = onActionClick) {
                Text(text = actionLabel, color = FitLogAccent)
            }
        }
    }
}

@Preview(name = "EmptyState 浅色", showBackground = true, widthDp = 412)
@Composable
private fun EmptyStatePreview() {
    FitLogTheme {
        EmptyStateIllustration(
            title = "今天还没有训练计划",
            subtitle = "前往「计划」页面创建你的每周训练计划",
            actionLabel = "安排训练",
        ) {}
    }
}

@Preview(name = "EmptyState 深色小屏", showBackground = true, widthDp = 360)
@Composable
private fun EmptyStatePreviewDark() {
    FitLogTheme(darkTheme = true) {
        EmptyStateIllustration(
            title = "还没有饮食记录，今天吃得怎么样？",
            subtitle = "记录第一餐，开始追踪你的热量与营养摄入情况",
        )
    }
}
