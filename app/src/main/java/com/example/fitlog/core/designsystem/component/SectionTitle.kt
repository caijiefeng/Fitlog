package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogSpacing
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import com.example.fitlog.core.designsystem.theme.FitLogType

/**
 * 区块标题：标题 + 可选副标题 + 可选操作按钮。
 */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = FitLogType.cardTitle,
                color = FitLogTextPrimary,
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = FitLogType.caption,
                    color = FitLogTextSecondary,
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.width(FitLogSpacing.S))
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    style = FitLogType.caption,
                    color = FitLogAccent,
                )
            }
        }
    }
}

@Preview(name = "SectionTitle 浅色", showBackground = true, widthDp = 412)
@Composable
private fun SectionTitlePreview() {
    FitLogTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(title = "今日训练")
            Spacer(modifier = Modifier.height(12.dp))
            SectionTitle(
                title = "本周统计",
                subtitle = "6月29日 – 7月5日",
                actionLabel = "查看全部",
            ) {}
        }
    }
}

@Preview(name = "SectionTitle 深色长文本", showBackground = true, widthDp = 360)
@Composable
private fun SectionTitlePreviewDark() {
    FitLogTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(
                title = "本周训练总容量统计（含自重与附加重量）",
                subtitle = "6月29日 – 7月5日 · 共 5 次训练",
            ) {}
        }
    }
}
