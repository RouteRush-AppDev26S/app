package com.example.appdevproject26s.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

/**
 * Single-series bar chart: thin bars with rounded tops anchored to the baseline,
 * y-axis tick labels in a reserved left gutter, recessive gridlines, a dashed
 * average line, a direct label on the max bar only, and sparse x tick labels.
 */
@Composable
fun StepsBarChart(
    values: List<Int>,
    xLabels: List<Pair<Int, String>>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 160.dp,
    averageValue: Int? = null
) {
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val baselineColor = MaterialTheme.colorScheme.outlineVariant
    val averageColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelBackdropColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f)
    val valueColor = MaterialTheme.colorScheme.onSurface

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    val numberFormat = NumberFormat.getIntegerInstance(Locale.GERMANY)
    val maxValue = values.maxOrNull() ?: 0

    Canvas(modifier = modifier.fillMaxWidth().height(chartHeight)) {
        if (values.isEmpty()) return@Canvas

        val topPad = 16.dp.toPx()
        val bottomPad = 16.dp.toPx()
        val plotHeight = size.height - topPad - bottomPad
        val baselineY = topPad + plotHeight

        // Measure tick labels first: their widest entry defines the left gutter.
        val tickStep = if (maxValue > 0) niceTickStep(maxValue) else 0
        val ticks = if (tickStep > 0) {
            generateSequence(tickStep) { it + tickStep }.takeWhile { it <= maxValue }
                .map { it to textMeasurer.measure(numberFormat.format(it), labelStyle) }
                .toList()
        } else emptyList()
        val gutter = if (ticks.isEmpty()) 0f
        else ticks.maxOf { it.second.size.width } + 6.dp.toPx()

        val plotLeft = gutter
        val plotWidth = size.width - plotLeft
        val gap = 2.dp.toPx()
        val barWidth = ((plotWidth - gap * (values.size - 1)) / values.size).coerceAtLeast(1f)
        val corner = minOf(4.dp.toPx(), barWidth / 2f)

        // Dotted gridlines with right-aligned tick labels in the gutter
        val dottedEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
        ticks.forEach { (tick, measured) ->
            val y = baselineY - plotHeight * (tick.toFloat() / maxValue)
            drawLine(
                color = gridColor,
                start = Offset(plotLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dottedEffect
            )
            drawText(
                measured,
                labelColor,
                Offset(gutter - 6.dp.toPx() - measured.size.width, y - measured.size.height / 2f)
            )
        }
        drawLine(baselineColor, Offset(plotLeft, baselineY), Offset(size.width, baselineY), strokeWidth = 1.dp.toPx())

        values.forEachIndexed { index, value ->
            if (value <= 0 || maxValue == 0) return@forEachIndexed
            val barHeight = plotHeight * (value.toFloat() / maxValue)
            val left = plotLeft + index * (barWidth + gap)
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, baselineY - barHeight, left + barWidth, baselineY),
                        topLeft = CornerRadius(corner),
                        topRight = CornerRadius(corner)
                    )
                )
            }
            drawPath(path, barColor)
        }

        // Dashed line at the overall average with a right-aligned label
        if (averageValue != null && averageValue > 0 && maxValue > 0) {
            val y = baselineY - plotHeight * (averageValue.toFloat() / maxValue).coerceAtMost(1f)
            drawLine(
                color = averageColor,
                start = Offset(plotLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
            val measured = textMeasurer.measure("Ø ${numberFormat.format(averageValue)}", labelStyle)
            val labelOffset = Offset(
                plotLeft + 4.dp.toPx(),
                y - measured.size.height - 3.dp.toPx()
            )
            drawRoundRect(
                color = labelBackdropColor,
                topLeft = labelOffset - Offset(3.dp.toPx(), 1.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(
                    measured.size.width + 6.dp.toPx(),
                    measured.size.height + 2.dp.toPx()
                ),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawText(measured, averageColor, labelOffset)
        }

        // Sparse x tick labels
        xLabels.forEach { (index, label) ->
            if (index !in values.indices) return@forEach
            val measured = textMeasurer.measure(label, labelStyle)
            val barCenter = plotLeft + index * (barWidth + gap) + barWidth / 2f
            val x = (barCenter - measured.size.width / 2f)
                .coerceIn(plotLeft, size.width - measured.size.width)
            drawText(measured, labelColor, Offset(x, baselineY + 3.dp.toPx()))
        }
    }
}

/** Rounded tick step (1/2/2.5/5 × 10^n) giving at most 4 gridlines below the max. */
private fun niceTickStep(maxValue: Int): Int {
    val rough = maxValue / 3.0
    val power = Math.pow(10.0, Math.floor(Math.log10(rough)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0)
        .map { it * power }
        .first { maxValue / it <= 4.0 }
    return step.toInt().coerceAtLeast(1)
}
