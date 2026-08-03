package com.example.fitlog.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogOnAccent
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import com.example.fitlog.core.designsystem.theme.FitLogType

/**
 * 分段筛选控件（如 最近使用 / 收藏 / 全部），150ms 切换动效。
 */
@Composable
fun <T> SegmentedFilter(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FitLogSurfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val container by animateColorAsState(
                targetValue = if (isSelected) FitLogAccent else androidx.compose.ui.graphics.Color.Transparent,
                animationSpec = tween(durationMillis = 150),
                label = "segment",
            )
            Text(
                text = label(option),
                style = FitLogType.caption,
                color = if (isSelected) FitLogOnAccent else FitLogAccent,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(container)
                    .heightIn(min = 36.dp)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .clickable { onSelect(option) },
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(name = "SegmentedFilter 浅色", showBackground = true, widthDp = 412)
@Composable
private fun SegmentedFilterPreview() {
    FitLogTheme {
        SegmentedFilter(
            options = listOf("最近使用", "收藏", "全部"),
            selected = "全部",
            onSelect = {},
            label = { it },
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "SegmentedFilter 深色", showBackground = true, widthDp = 360)
@Composable
private fun SegmentedFilterPreviewDark() {
    FitLogTheme(darkTheme = true) {
        SegmentedFilter(
            options = listOf("胸部", "背部", "肩部", "腿部"),
            selected = "肩部",
            onSelect = {},
            label = { it },
            modifier = Modifier.padding(16.dp),
        )
    }
}
