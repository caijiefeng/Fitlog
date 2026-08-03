package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogErrorContainer
import com.example.fitlog.core.designsystem.theme.FitLogSpacing
import com.example.fitlog.core.designsystem.theme.FitLogSuccess
import com.example.fitlog.core.designsystem.theme.FitLogSuccessContainer
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import com.example.fitlog.core.designsystem.theme.FitLogType
import com.example.fitlog.core.designsystem.theme.FitLogWarning
import com.example.fitlog.core.designsystem.theme.FitLogWarningContainer

/**
 * 状态胶囊：语义色圆点 + 小圆角标签，用于训练状态、提醒状态等。
 */
@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = FitLogSuccess,
    containerColor: Color = FitLogSuccessContainer,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(containerColor, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = FitLogType.caption,
            color = color,
        )
    }
}

@Preview(name = "StatusPill 浅色", showBackground = true, widthDp = 412)
@Composable
private fun StatusPillPreview() {
    FitLogTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            StatusPill(text = "已完成", color = FitLogSuccess, containerColor = FitLogSuccessContainer)
            StatusPill(
                text = "已跳过",
                color = FitLogWarning,
                containerColor = FitLogWarningContainer,
                modifier = Modifier.padding(start = FitLogSpacing.S),
            )
            StatusPill(
                text = "已取消",
                color = FitLogError,
                containerColor = FitLogErrorContainer,
                modifier = Modifier.padding(start = FitLogSpacing.S),
            )
            StatusPill(
                text = "未开始",
                color = FitLogTextSecondary,
                containerColor = FitLogTextSecondary.copy(alpha = 0.12f),
                modifier = Modifier.padding(start = FitLogSpacing.S),
            )
        }
    }
}

@Preview(name = "StatusPill 深色", showBackground = true, widthDp = 412)
@Composable
private fun StatusPillPreviewDark() {
    FitLogTheme(darkTheme = true) {
        Row(modifier = Modifier.padding(16.dp)) {
            StatusPill(text = "进行中", color = FitLogWarning, containerColor = FitLogWarningContainer)
            StatusPill(
                text = "已完成",
                modifier = Modifier.padding(start = FitLogSpacing.S),
            )
        }
    }
}
