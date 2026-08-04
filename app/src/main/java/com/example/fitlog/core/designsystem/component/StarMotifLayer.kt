package com.example.fitlog.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import com.example.fitlog.core.designsystem.theme.StarMotif

/**
 * 原创球星图案层：纯 Compose Canvas 绘制，不含任何球队 Logo。
 * 图案透明度一般 0.04～0.12，不干扰文字可读性。
 */
@Composable
fun StarMotifLayer(
    motif: StarMotif,
    color: Color,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    if (motif == StarMotif.NONE || alpha <= 0f) return
    Canvas(modifier = modifier) {
        val c = color.copy(alpha = alpha)
        val w = size.width
        val h = size.height
        when (motif) {
            StarMotif.MAMBA_SCALE -> {
                // 蛇鳞：斜向交叉细线网格
                val step = w / 6f
                for (i in -8..8) {
                    drawLine(
                        color = c,
                        start = Offset(w / 2 + i * step, 0f),
                        end = Offset(w / 2 + i * step - h, h),
                        strokeWidth = w / 90f,
                    )
                    drawLine(
                        color = c,
                        start = Offset(w / 2 + i * step - h, 0f),
                        end = Offset(w / 2 + i * step, h),
                        strokeWidth = w / 90f,
                    )
                }
            }
            StarMotif.CROWN -> {
                // 皇冠轮廓
                val path = Path().apply {
                    moveTo(w * 0.18f, h * 0.72f)
                    lineTo(w * 0.18f, h * 0.30f)
                    lineTo(w * 0.38f, h * 0.50f)
                    lineTo(w * 0.50f, h * 0.22f)
                    lineTo(w * 0.62f, h * 0.50f)
                    lineTo(w * 0.82f, h * 0.30f)
                    lineTo(w * 0.82f, h * 0.72f)
                    close()
                }
                drawPath(path, color = c, style = Stroke(width = w / 45f))
                drawLine(c, Offset(w * 0.18f, h * 0.80f), Offset(w * 0.82f, h * 0.80f), w / 45f)
            }
            StarMotif.NET_GRID -> {
                // 篮网网格
                val cols = 8
                val rows = 7
                val cw = w / cols
                val ch = h / rows
                for (i in 0..cols) {
                    drawLine(c, Offset(i * cw, 0f), Offset(i * cw, h), w / 110f)
                }
                for (j in 0..rows) {
                    drawLine(c, Offset(0f, j * ch), Offset(w, j * ch), w / 110f)
                }
                // 网格弧底（篮网下垂）
                drawArc(
                    color = c,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.1f, h * 0.4f),
                    size = Size(w * 0.8f, h * 0.6f),
                    style = Stroke(width = w / 60f),
                )
            }
            StarMotif.SPLASH_ARC -> {
                // 三分弧线 + 水花
                drawArc(
                    color = c,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.1f, h * 0.15f),
                    size = Size(w * 0.8f, h * 0.8f),
                    style = Stroke(width = w / 55f, cap = StrokeCap.Round),
                )
                // 水花点
                val drops = listOf(
                    Offset(w * 0.28f, h * 0.40f),
                    Offset(w * 0.36f, h * 0.30f),
                    Offset(w * 0.50f, h * 0.24f),
                    Offset(w * 0.64f, h * 0.30f),
                    Offset(w * 0.72f, h * 0.40f),
                )
                drops.forEach { o ->
                    drawCircle(color = c, radius = w / 55f, center = o)
                }
            }
            StarMotif.WINGS -> {
                // 飞翼：左右翼形轮廓
                val left = Path().apply {
                    moveTo(w * 0.5f, h * 0.45f)
                    cubicTo(w * 0.38f, h * 0.20f, w * 0.18f, h * 0.16f, w * 0.06f, h * 0.20f)
                    cubicTo(w * 0.22f, h * 0.30f, w * 0.26f, h * 0.52f, w * 0.24f, h * 0.70f)
                    cubicTo(w * 0.36f, h * 0.62f, w * 0.44f, h * 0.55f, w * 0.5f, h * 0.45f)
                    close()
                }
                val right = Path().apply {
                    moveTo(w * 0.5f, h * 0.45f)
                    cubicTo(w * 0.62f, h * 0.20f, w * 0.82f, h * 0.16f, w * 0.94f, h * 0.20f)
                    cubicTo(w * 0.78f, h * 0.30f, w * 0.74f, h * 0.52f, w * 0.76f, h * 0.70f)
                    cubicTo(w * 0.64f, h * 0.62f, w * 0.56f, h * 0.55f, w * 0.5f, h * 0.45f)
                    close()
                }
                drawPath(left, color = c, style = Stroke(width = w / 50f))
                drawPath(right, color = c, style = Stroke(width = w / 50f))
                drawLine(c, Offset(w * 0.3f, h * 0.72f), Offset(w * 0.7f, h * 0.72f), w / 60f)
            }
            StarMotif.ROCKET -> {
                // 原创 Q 版火箭（圆润）+ 胡须状尾焰
                val body = Path().apply {
                    moveTo(w * 0.5f, h * 0.10f)
                    cubicTo(w * 0.70f, h * 0.28f, w * 0.72f, h * 0.48f, w * 0.66f, h * 0.64f)
                    lineTo(w * 0.34f, h * 0.64f)
                    cubicTo(w * 0.28f, h * 0.48f, w * 0.30f, h * 0.28f, w * 0.5f, h * 0.10f)
                    close()
                }
                drawPath(body, color = c, style = Stroke(width = w / 45f))
                // 舷窗
                drawCircle(color = c, radius = w / 30f, center = Offset(w * 0.5f, h * 0.40f))
                // 尾焰（胡须形状：两条弯曲线）
                val flameL = Path().apply {
                    moveTo(w * 0.38f, h * 0.64f)
                    cubicTo(w * 0.30f, h * 0.74f, w * 0.34f, h * 0.84f, w * 0.42f, h * 0.90f)
                }
                val flameR = Path().apply {
                    moveTo(w * 0.62f, h * 0.64f)
                    cubicTo(w * 0.70f, h * 0.74f, w * 0.66f, h * 0.84f, w * 0.58f, h * 0.90f)
                }
                drawPath(flameL, color = c, style = Stroke(width = w / 38f, cap = StrokeCap.Round))
                drawPath(flameR, color = c, style = Stroke(width = w / 38f, cap = StrokeCap.Round))
            }
            StarMotif.LIGHTNING -> {
                // 闪电
                val path = Path().apply {
                    moveTo(w * 0.58f, h * 0.08f)
                    lineTo(w * 0.30f, h * 0.52f)
                    lineTo(w * 0.50f, h * 0.52f)
                    lineTo(w * 0.42f, h * 0.92f)
                    lineTo(w * 0.72f, h * 0.44f)
                    lineTo(w * 0.52f, h * 0.44f)
                    close()
                }
                drawPath(path, color = c, style = Stroke(width = w / 40f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            StarMotif.SPEED_LINES -> {
                // 速度斜线
                for (i in 0 until 7) {
                    val y = h * (0.12f + i * 0.13f)
                    val len = w * (0.30f + (i % 3) * 0.08f)
                    drawLine(
                        color = c,
                        start = Offset(w * 0.08f, y),
                        end = Offset(w * 0.08f + len, y - h * 0.06f),
                        strokeWidth = w / 70f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            StarMotif.STAR_RAYS -> {
                // 星形 + 放射线
                val star = Path().apply {
                    val cx = w * 0.5f
                    val cy = h * 0.5f
                    val outer = w * 0.22f
                    val inner = w * 0.10f
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) outer else inner
                        val a = Math.toRadians((i * 36 - 90).toDouble())
                        val x = (cx + r * kotlin.math.cos(a)).toFloat()
                        val y = (cy + r * kotlin.math.sin(a)).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(star, color = c, style = Stroke(width = w / 55f))
                // 放射线
                for (i in 0 until 8) {
                    val a = Math.toRadians((i * 45 + 22).toDouble())
                    val dx = kotlin.math.cos(a).toFloat()
                    val dy = kotlin.math.sin(a).toFloat()
                    drawLine(
                        color = c,
                        start = Offset(w * 0.5f + dx * w * 0.30f, h * 0.5f + dy * w * 0.30f),
                        end = Offset(w * 0.5f + dx * w * 0.46f, h * 0.5f + dy * w * 0.46f),
                        strokeWidth = w / 80f,
                    )
                }
            }
            StarMotif.FREESTYLE_ORBIT -> {
                // 自由花式圆弧轨道
                drawCircle(color = c, radius = w * 0.20f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = w / 55f))
                drawArc(
                    color = c,
                    startAngle = 30f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.22f),
                    size = Size(w * 0.64f, h * 0.56f),
                    style = Stroke(width = w / 70f, cap = StrokeCap.Round),
                )
                drawArc(
                    color = c,
                    startAngle = 210f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = Offset(w * 0.30f, h * 0.30f),
                    size = Size(w * 0.40f, h * 0.40f),
                    style = Stroke(width = w / 90f, cap = StrokeCap.Round),
                )
                drawCircle(color = c, radius = w / 45f, center = Offset(w * 0.5f, h * 0.5f))
            }
            StarMotif.NONE -> Unit
        }
    }
}

@Preview(name = "StarMotifLayer 预览", showBackground = true, widthDp = 412)
@Composable
private fun StarMotifLayerPreview() {
    FitLogTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier,
        ) {
            StarMotifLayer(
                motif = StarMotif.CROWN,
                color = FitLogAccent,
                alpha = 0.5f,
                modifier = Modifier.size(160.dp),
            )
        }
    }
}
