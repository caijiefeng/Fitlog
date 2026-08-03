package com.example.fitlog.core.designsystem.theme

import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// ── Spacing ──────────────────────────────────────────────────────────────────

/** 统一间距刻度：4 / 8 / 12 / 16 / 20 / 24 / 32 dp */
object FitLogSpacing {
    val XS = 4.dp
    val S = 8.dp
    val M = 12.dp
    val L = 16.dp
    val XL = 20.dp
    val XXL = 24.dp
    val XXXL = 32.dp
}

// ── Shapes ───────────────────────────────────────────────────────────────────

/** 统一圆角：小控件 8 / 输入框 12 / 普通卡片 16 / 重点卡片 20 / Hero 24 */
object FitLogShapes {
    val small = RoundedCornerShape(8.dp)
    val input = RoundedCornerShape(12.dp)
    val card = RoundedCornerShape(16.dp)
    val emphasized = RoundedCornerShape(20.dp)
    val hero = RoundedCornerShape(24.dp)
    val circle = RoundedCornerShape(50)
}

// ── Elevation ────────────────────────────────────────────────────────────────

object FitLogElevation {
    val none = 0.dp
    val subtle = 1.dp
    val raised = 3.dp
    val overlay = 6.dp
}

// ── Motion ───────────────────────────────────────────────────────────────────

/**
 * 短动效规范：页面淡入 180ms、卡片展开 220ms、筛选切换 150ms、
 * 进度变化 300ms。尊重系统动画缩放（Compose 动画默认跟随系统设置）。
 */
object FitLogMotion {
    fun screenFadeIn() = tween<Float>(durationMillis = 180)
    fun cardExpand() = tween<Int>(durationMillis = 220)
    fun filterSwitch() = tween<Float>(durationMillis = 150)
    fun progressChange() = tween<Float>(durationMillis = 300)
}

// ── Dimensions ───────────────────────────────────────────────────────────────

object FitLogDimensions {
    /** 最小触摸区域 48×48 dp */
    val touchTarget = 48.dp

    /** 页面左右留白 */
    val pageGutter = 16.dp

    /** 列表缩略图 */
    val listThumbnail = 80.dp

    /** 网格间距 */
    val gridGutter = 12.dp

    /** 底部固定操作条高度 */
    val bottomBar = 72.dp

    /** 动作示意面板高度 */
    val mediaPanelHeight = 220.dp
}
