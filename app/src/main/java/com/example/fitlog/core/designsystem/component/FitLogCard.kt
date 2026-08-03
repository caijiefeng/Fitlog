package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogAccentContainer
import com.example.fitlog.core.designsystem.theme.FitLogCard
import com.example.fitlog.core.designsystem.theme.FitLogDivider
import com.example.fitlog.core.designsystem.theme.FitLogElevation
import com.example.fitlog.core.designsystem.theme.FitLogShapes
import com.example.fitlog.core.designsystem.theme.FitLogSpacing
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant

/**
 * 卡片层级类型。避免所有区块都用同一种白色矩形：
 * - 今日训练 / 重点区块：HERO
 * - 统计数据：TONAL
 * - 普通设置 / 列表行：STANDARD
 * - 可选择动作：OUTLINED（配合 selected）
 * - 高优先级操作：ELEVATED
 */
enum class FitLogCardStyle {
    STANDARD,
    ELEVATED,
    OUTLINED,
    TONAL,
    HERO,
}

@Composable
fun FitLogCard(
    modifier: Modifier = Modifier,
    style: FitLogCardStyle = FitLogCardStyle.STANDARD,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    containerColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(FitLogSpacing.L),
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedColor = containerColor ?: when (style) {
        FitLogCardStyle.STANDARD,
        FitLogCardStyle.ELEVATED,
        FitLogCardStyle.HERO,
        -> FitLogCard

        FitLogCardStyle.OUTLINED -> FitLogCard
        FitLogCardStyle.TONAL -> FitLogSurfaceVariant
    }
    val shape = when (style) {
        FitLogCardStyle.STANDARD,
        FitLogCardStyle.ELEVATED,
        FitLogCardStyle.OUTLINED,
        -> FitLogShapes.card

        FitLogCardStyle.TONAL -> FitLogShapes.card
        FitLogCardStyle.HERO -> FitLogShapes.emphasized
    }
    val elevation = when (style) {
        FitLogCardStyle.ELEVATED -> FitLogElevation.raised
        FitLogCardStyle.HERO -> FitLogElevation.subtle
        else -> FitLogElevation.none
    }
    val border = when {
        selected -> BorderStroke(2.dp, FitLogAccent)
        style == FitLogCardStyle.OUTLINED -> BorderStroke(1.dp, FitLogDivider)
        style == FitLogCardStyle.HERO -> BorderStroke(1.dp, FitLogAccent.copy(alpha = 0.25f))
        else -> null
    }

    val interactionSource = remember { MutableInteractionSource() }
    val clickModifier = if (onClick != null && enabled) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    val contentAlpha = if (enabled) 1f else 0.5f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier),
        shape = shape,
        color = if (style == FitLogCardStyle.HERO && !selected) {
            FitLogAccentContainer
        } else {
            resolvedColor
        },
        border = border,
        shadowElevation = elevation,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingContent != null) {
                Box(
                    modifier = Modifier
                        .padding(end = FitLogSpacing.M)
                        .alpha(contentAlpha),
                    contentAlignment = Alignment.Center,
                ) { leadingContent() }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(contentAlpha),
                content = content,
            )
            if (trailingContent != null) {
                Box(
                    modifier = Modifier
                        .padding(start = FitLogSpacing.M)
                        .alpha(contentAlpha),
                    contentAlignment = Alignment.Center,
                ) { trailingContent() }
            }
        }
    }
}
