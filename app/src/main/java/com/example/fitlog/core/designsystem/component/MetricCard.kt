package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogSpacing
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import com.example.fitlog.core.designsystem.theme.FitLogType

/**
 * 统计数据卡片（TONAL 风格）：大号等宽数字 + 标签 + 可选趋势与图标。
 * 用于本周统计、本月训练次数、总容量等。
 */
@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = FitLogAccent,
    icon: ImageVector? = null,
    trend: String? = null,
    onClick: (() -> Unit)? = null,
) {
    FitLogCard(
        modifier = modifier,
        style = FitLogCardStyle.TONAL,
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(FitLogSpacing.L),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FitLogTextSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(FitLogSpacing.S))
            }
            Text(
                text = label,
                style = FitLogType.caption,
                color = FitLogTextSecondary,
            )
        }
        Spacer(modifier = Modifier.height(FitLogSpacing.S))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = FitLogType.heroNumber,
                color = valueColor,
            )
            if (trend != null) {
                Spacer(modifier = Modifier.width(FitLogSpacing.S))
                Text(
                    text = trend,
                    style = FitLogType.caption,
                    color = FitLogTextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }
    }
}

/**
 * 数据行：标签 + 等宽数值，用于统计列表与详情摘要。
 */
@Composable
fun MetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = FitLogTextPrimary,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = FitLogType.caption,
            color = FitLogTextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = FitLogType.statistic,
            color = valueColor,
        )
    }
}

@Preview(name = "MetricCard 浅色", showBackground = true, widthDp = 412)
@Composable
private fun MetricCardPreviewLight() {
    FitLogTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MetricCard(label = "本周训练", value = "3", trend = "次", icon = Icons.Filled.FitnessCenter)
            Spacer(modifier = Modifier.height(12.dp))
            MetricCard(label = "本周总容量", value = "12 350", trend = "kg", icon = Icons.Filled.TrendingUp)
        }
    }
}

@Preview(name = "MetricCard 深色", showBackground = true, widthDp = 412)
@Composable
private fun MetricCardPreviewDark() {
    FitLogTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            MetricCard(label = "当前体重", value = "80", trend = "kg")
        }
    }
}

@Preview(name = "MetricCard 长文本小屏", showBackground = true, widthDp = 360)
@Composable
private fun MetricCardPreviewLongText() {
    FitLogTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MetricCard(label = "本月训练总容量（含自重与附加重量）", value = "12 350", trend = "kg")
        }
    }
}

@Preview(name = "MetricRow 浅色", showBackground = true, widthDp = 412)
@Composable
private fun MetricRowPreview() {
    FitLogTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MetricRow(label = "身高", value = "178 cm")
            MetricRow(label = "体重", value = "80.5 kg")
            MetricRow(label = "体脂率", value = "18.2 %")
        }
    }
}
