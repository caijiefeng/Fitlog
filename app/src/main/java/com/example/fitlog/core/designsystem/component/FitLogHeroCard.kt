package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * 大型 Hero 区块卡片：今日训练、训练执行中的当前动作等最显眼内容。
 * 使用重点卡片圆角 + 强调色容器底色。
 */
@Composable
fun FitLogHeroCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    FitLogCard(
        modifier = modifier,
        style = FitLogCardStyle.HERO,
        onClick = onClick,
        contentPadding = PaddingValues(
            start = FitLogSpacing.L,
            end = FitLogSpacing.L,
            top = FitLogSpacing.XL,
            bottom = FitLogSpacing.XL,
        ),
    ) {
        Text(
            text = title,
            style = FitLogType.cardTitle,
            color = FitLogAccent,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(FitLogSpacing.XS))
            Text(
                text = subtitle,
                style = FitLogType.caption,
                color = FitLogTextSecondary,
            )
        }
        if (content != null) {
            Spacer(modifier = Modifier.height(FitLogSpacing.M))
            Column(content = content)
        }
    }
}

@Preview(name = "HeroCard 浅色", showBackground = true, widthDp = 412)
@Composable
private fun HeroCardPreviewLight() {
    FitLogTheme {
        FitLogHeroCard(
            title = "今日训练",
            subtitle = "Push A · 6 个动作 · 18 组 · 预计 65 分钟",
        ) {
            Row {
                Text(
                    text = "开始训练",
                    style = MaterialTheme.typography.titleMedium,
                    color = FitLogTextPrimary,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }
    }
}

@Preview(name = "HeroCard 深色", showBackground = true, widthDp = 412)
@Composable
private fun HeroCardPreviewDark() {
    FitLogTheme(darkTheme = true) {
        FitLogHeroCard(
            title = "今日训练",
            subtitle = "6 个动作 · 18 组 · 预计 65 分钟",
        )
    }
}

@Preview(name = "HeroCard 长文本小屏", showBackground = true, widthDp = 360)
@Composable
private fun HeroCardPreviewLongText() {
    FitLogTheme {
        FitLogHeroCard(
            title = "今日训练",
            subtitle = "上半身推举综合训练日 · 6 个动作 · 18 组 · 预计 65 分钟",
        )
    }
}
