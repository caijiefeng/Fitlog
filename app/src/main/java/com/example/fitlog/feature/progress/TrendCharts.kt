package com.example.fitlog.feature.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogCard
import com.example.fitlog.core.designsystem.theme.FitLogSuccess
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.data.repository.ProgressRepository
import com.example.fitlog.data.repository.TrendPoint
import com.example.fitlog.data.repository.TrendRange
import java.time.format.DateTimeFormatter

/**
 * Trend charts screen showing configurable line charts for key metrics.
 *
 * Uses Compose Canvas for custom rendering.  Supports a time-range
 * selector and displays a tooltip on tap.
 */

// ── Chart metric configuration ─────────────────────────────────────────

private enum class ChartMetric(
    val labelResName: Int,
    val color: Color,
    val extractValue: (TrendPoint) -> Double?,
    val unit: String,
) {
    WEIGHT(
        labelResName = R.string.chart_weight,
        color = FitLogAccent,
        extractValue = { it.weight },
        unit = "kg",
    ),
    BODY_FAT(
        labelResName = R.string.chart_body_fat,
        color = Color(0xFFFF7043),
        extractValue = { it.bodyFat },
        unit = "%",
    ),
    WAIST(
        labelResName = R.string.chart_waist,
        color = Color(0xFF42A5F5),
        extractValue = { it.waist },
        unit = "cm",
    ),
    CALORIES(
        labelResName = R.string.chart_calories,
        color = Color(0xFFFFCA28),
        extractValue = { it.calories },
        unit = "kcal",
    ),
    PROTEIN(
        labelResName = R.string.chart_protein,
        color = FitLogSuccess,
        extractValue = { it.protein },
        unit = "g",
    ),
    VOLUME(
        labelResName = R.string.chart_volume,
        color = Color(0xFFAB47BC),
        extractValue = { it.volume },
        unit = "kg",
    ),
}

// ── Composable ─────────────────────────────────────────────────────────

@Composable
fun TrendChartsContent(
    points: List<TrendPoint>,
    selectedRange: TrendRange,
    isLoading: Boolean,
    onRangeChange: (TrendRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Range selector
        RangeSelector(
            selected = selectedRange,
            onSelect = onRangeChange,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = FitLogAccent)
            }
        } else if (points.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.chart_empty),
                    color = FitLogTextSecondary,
                )
            }
        } else {
            ChartMetric.entries.forEach { metric ->
                Spacer(modifier = Modifier.height(12.dp))
                TrendLineChart(
                    title = stringResource(metric.labelResName),
                    unit = metric.unit,
                    points = points,
                    valueExtractor = metric.extractValue,
                    lineColor = metric.color,
                )
            }
        }
    }
}

@Composable
private fun RangeSelector(
    selected: TrendRange,
    onSelect: (TrendRange) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TrendRange.entries.forEach { range ->
            Button(
                onClick = { onSelect(range) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (range == selected) FitLogAccent else FitLogSurfaceVariant,
                    contentColor = FitLogTextPrimary,
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(
                        when (range) {
                            TrendRange.WEEK_7 -> R.string.trend_range_7d
                            TrendRange.MONTH_30 -> R.string.trend_range_30d
                            TrendRange.MONTH_90 -> R.string.trend_range_90d
                            TrendRange.MONTHS_6 -> R.string.trend_range_6m
                            TrendRange.ALL -> R.string.trend_range_all
                        }
                    ),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

// ── Single line chart ──────────────────────────────────────────────────

@Composable
private fun TrendLineChart(
    title: String,
    unit: String,
    points: List<TrendPoint>,
    valueExtractor: (TrendPoint) -> Double?,
    lineColor: Color,
) {
    val values = points.mapNotNull { valueExtractor(it) }
    if (values.isEmpty()) return

    val allDates = points.map { it.date }
    val minValue = values.min()
    val maxValue = values.max()
    val valueRange = if (maxValue == minValue) 1.0 else maxValue - minValue
    val padding = valueRange * 0.1

    Card(
        colors = CardDefaults.cardColors(containerColor = FitLogCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = FitLogTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.chart_latest_value, values.last(), unit),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = lineColor,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            var selectedIndex by remember { mutableStateOf(-1) }

            val canvasHeight = 180.dp
            val canvasWidth = androidx.compose.ui.unit.Dp.Infinity
            val density = LocalDensity.current
            val labelColor = FitLogTextSecondary
            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#999999")
                textSize = with(density) { 10.sp.toPx() }
                isAntiAlias = true
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
                    .background(Color.Transparent)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            val stepX = size.width.toFloat() / (points.size - 1).coerceAtLeast(1)
                            val index = ((offset.x / stepX) + 0.5f).toInt()
                                .coerceIn(0, points.size - 1)
                            selectedIndex = if (selectedIndex == index) -1 else index
                        }
                    },
            ) {
                val width = size.width
                val height = size.height
                val topPadding = 16.dp.toPx()
                val bottomPadding = 24.dp.toPx()
                val chartHeight = height - topPadding - bottomPadding
                val leftPadding = 40.dp.toPx()
                val chartWidth = width - leftPadding - 8.dp.toPx()

                if (points.size < 2 || chartWidth <= 0 || chartHeight <= 0) return@Canvas

                val stepX = chartWidth / (points.size - 1)
                val minVal = minValue - padding
                val maxVal = maxValue + padding
                val range = maxVal - minVal

                // Y-axis labels
                val ySteps = 4
                for (i in 0..ySteps) {
                    val yVal = minVal + (range * i / ySteps)
                    val y = height - bottomPadding - (chartHeight * i / ySteps)
                    drawContext.canvas.nativeCanvas.drawText(
                        "%.1f".format(yVal),
                        2.dp.toPx(),
                        y + 4.dp.toPx(),
                        labelPaint,
                    )
                    // Grid line
                    drawLine(
                        color = Color(0xFF2A2A2A),
                        start = Offset(leftPadding, y),
                        end = Offset(width - 4.dp.toPx(), y),
                        strokeWidth = 0.5.dp.toPx(),
                    )
                }

                // Build and draw path
                val path = Path()
                var first = true
                for ((i, point) in points.withIndex()) {
                    val value = valueExtractor(point) ?: continue
                    val x = leftPadding + stepX * i
                    val y = height - bottomPadding - (chartHeight * ((value - minVal) / range)).toFloat()

                    if (first) {
                        path.moveTo(x, y)
                        first = false
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                // Data point dots and tooltip
                for ((i, point) in points.withIndex()) {
                    val value = valueExtractor(point) ?: continue
                    val x = leftPadding + stepX * i
                    val y = height - bottomPadding - (chartHeight * ((value - minVal) / range)).toFloat()

                    // Dot
                    drawCircle(
                        color = lineColor,
                        radius = if (i == selectedIndex) 5.dp.toPx() else 2.dp.toPx(),
                        center = Offset(x, y),
                    )

                    // Tooltip
                    if (i == selectedIndex) {
                        val tooltipPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#333333")
                            textSize = with(density) { 12.sp.toPx() }
                            isAntiAlias = true
                        }
                        val tooltipText = "${point.date.format(DateTimeFormatter.ISO_LOCAL_DATE)}: %.1f $unit".format(value)
                        drawContext.canvas.nativeCanvas.drawText(
                            tooltipText,
                            (x - 40.dp.toPx()).coerceIn(0f, width - 100.dp.toPx()),
                            y - 12.dp.toPx(),
                            tooltipPaint,
                        )
                    }
                }

                // X-axis date labels (first, last, and middle)
                val dateLabels = listOf(0, points.size / 2, points.size - 1)
                for (i in dateLabels) {
                    if (i < 0 || i >= points.size) continue
                    val x = leftPadding + stepX * i
                    val label = points[i].date.format(DateTimeFormatter.ofPattern("MM/dd"))
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        (x - 16.dp.toPx()).coerceAtLeast(0f),
                        height - 2.dp.toPx(),
                        labelPaint,
                    )
                }
            }
        }
    }
}
