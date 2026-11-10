package com.crylo.currency.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.crylo.currency.core.RatePoint
import kotlin.math.roundToInt

/**
 * A plain line chart drawn on a Canvas, so no charting library is needed. Touching or
 * dragging selects the nearest point ([onSelect] with its index, null on release).
 */
@Composable
fun LineChart(points: List<RatePoint>, selected: Int?, onSelect: (Int?) -> Unit, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val guide = MaterialTheme.colorScheme.outlineVariant
    val marker = MaterialTheme.colorScheme.tertiary

    fun indexAt(x: Float, width: Float): Int =
        if (points.size < 2) 0 else (x / width * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)

    Canvas(
        modifier
            .pointerInput(points) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onSelect(indexAt(down.position.x, size.width.toFloat()))
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        change.consume()
                        onSelect(indexAt(change.position.x, size.width.toFloat()))
                    }
                    onSelect(null)
                }
            },
    ) {
        if (points.isEmpty()) return@Canvas
        val min = points.minOf { it.rate }
        val max = points.maxOf { it.rate }
        val pad = 8.dp.toPx()
        val h = size.height - pad * 2
        val stepX = if (points.size > 1) size.width / (points.size - 1) else 0f
        fun x(i: Int) = i * stepX
        // A pegged pair (USD/AED) is a flat line: draw it mid-height instead of dividing by 0.
        fun y(v: Double): Float = if (max == min) pad + h / 2 else pad + (h - (v - min) / (max - min) * h).toFloat()

        val path = Path()
        points.forEachIndexed { i, p -> if (i == 0) path.moveTo(x(i), y(p.rate)) else path.lineTo(x(i), y(p.rate)) }
        val fill = Path().apply {
            addPath(path)
            lineTo(x(points.lastIndex), size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(line.copy(alpha = 0.25f), line.copy(alpha = 0f))))
        val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
        drawLine(guide, Offset(0f, y(max)), Offset(size.width, y(max)), 1.dp.toPx(), pathEffect = dash)
        drawLine(guide, Offset(0f, y(min)), Offset(size.width, y(min)), 1.dp.toPx(), pathEffect = dash)
        drawPath(path, line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        val sel = selected?.takeIf { it in points.indices } ?: return@Canvas
        val sx = x(sel)
        val sy = y(points[sel].rate)
        drawLine(marker, Offset(sx, 0f), Offset(sx, size.height), 1.dp.toPx())
        drawCircle(marker, 5.dp.toPx(), Offset(sx, sy))
    }
}
