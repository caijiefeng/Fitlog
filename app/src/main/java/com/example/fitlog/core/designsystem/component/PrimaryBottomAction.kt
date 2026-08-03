package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogSpacing
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import com.example.fitlog.core.designsystem.theme.FitLogType

/**
 * 底部固定主操作条：已选数量等提示 + 全宽主按钮（如「已选择 5 个动作 / 加入训练」）。
 */
@Composable
fun PrimaryBottomAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    badge: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FitLogBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (badge != null) {
            Text(
                text = badge,
                style = FitLogType.caption,
                color = FitLogTextPrimary,
            )
            Spacer(modifier = Modifier.height(FitLogSpacing.S))
        }
        Button(
            onClick = onClick,
            enabled = enabled && !loading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = FitLogAccent,
                )
                Spacer(modifier = Modifier.width(FitLogSpacing.S))
            }
            Text(
                text = text,
                style = FitLogType.body,
            )
        }
    }
}

@Preview(name = "PrimaryBottomAction 浅色", showBackground = true, widthDp = 412)
@Composable
private fun PrimaryBottomActionPreview() {
    FitLogTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PrimaryBottomAction(
                text = "加入训练",
                badge = "已选择 5 个动作",
                onClick = {},
            )
        }
    }
}

@Preview(name = "PrimaryBottomAction 深色小屏", showBackground = true, widthDp = 360)
@Composable
private fun PrimaryBottomActionPreviewDark() {
    FitLogTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            PrimaryBottomAction(
                text = "保存头像",
                onClick = {},
                enabled = false,
            )
        }
    }
}
