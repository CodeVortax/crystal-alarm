// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/** Minute of the day at the top of the dial: 12 PM, so day is up and night is down. */
internal const val DialTopMinute = 720
internal fun dialAngleRad(minuteOfDay: Int): Float = (minuteOfDay - DialTopMinute) / 1440f * 2f * Math.PI.toFloat()
internal fun dialAngleDeg(minuteOfDay: Int): Float = (minuteOfDay - DialTopMinute) / 1440f * 360f

internal val RingWidth = 44.dp
internal val RingInset = 4.dp

/**
 * A 24-hour dial like the Health sleep-schedule picker: 12 PM at the top, 12 AM at the bottom.
 * Drag the bell knob (or tap the ring) to choose a time to the minute. The tinted arc shows how
 * far away the alarm is from the current time.
 */
@Composable
fun CircularTimePicker(
    hour: Int,
    minute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val latestOnChange by rememberUpdatedState(onTimeChange)
    val selectedMin = hour * 60 + minute
    val nowMin = remember { java.time.LocalTime.now().let { it.hour * 60 + it.minute } }
    val trackColor = scheme.surfaceVariant
    val arcColor = scheme.primary.copy(alpha = 0.35f)
    val tickColor = scheme.onSurfaceVariant.copy(alpha = 0.55f)

    BoxWithConstraints(
        modifier.fillMaxWidth().aspectRatio(1f)
            .background(scheme.surface, RoundedCornerShape(32.dp)).padding(12.dp)
    ) {
        val box = maxWidth
        val ringR = box / 2 - RingWidth / 2 - RingInset
        val innerR = ringR - RingWidth / 2
        val knobAngle = dialAngleRad(selectedMin)

        // Face (inside the ring)
        Box(Modifier.align(Alignment.Center).size(innerR * 2).background(scheme.background, RoundedCornerShape(50)))

        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { pos ->
                        if (nearRing(pos, size)) pick(pos, size) { h, m -> latestOnChange(h, m) }
                    })
                }
                .pointerInput(Unit) {
                    var active = false
                    detectDragGestures(
                        onDragStart = { pos ->
                            active = nearRing(pos, size)
                            if (active) pick(pos, size) { h, m -> latestOnChange(h, m) }
                        },
                        onDragEnd = { active = false },
                        onDragCancel = { active = false },
                    ) { change, _ ->
                        if (active) {
                            change.consume()
                            pick(change.position, size) { h, m -> latestOnChange(h, m) }
                        }
                    }
                }
        ) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val ringPx = RingWidth.toPx()
            val r = size.minDimension / 2f - ringPx / 2f - RingInset.toPx()

            drawCircle(trackColor, radius = r, center = c, style = Stroke(width = ringPx))

            val sweep = (((selectedMin - nowMin) % 1440 + 1440) % 1440) / 1440f * 360f
            drawArc(arcColor, startAngle = dialAngleDeg(nowMin) - 90f, sweepAngle = sweep, useCenter = false,
                topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2f, r * 2f), style = Stroke(width = ringPx))

            // 96 ticks (every 15 min), longer on the hour
            for (i in 0 until 96) {
                val a = Math.toRadians(i * 3.75 - 90.0)
                val half = if (i % 4 == 0) ringPx * 0.28f else ringPx * 0.12f
                drawLine(tickColor,
                    Offset(c.x + (r - half) * cos(a).toFloat(), c.y + (r - half) * sin(a).toFloat()),
                    Offset(c.x + (r + half) * cos(a).toFloat(), c.y + (r + half) * sin(a).toFloat()),
                    strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
            }

            // Knob
            val kx = c.x + r * sin(knobAngle)
            val ky = c.y - r * cos(knobAngle)
            drawCircle(Color.Black.copy(alpha = 0.18f), radius = ringPx * 0.62f + 2.dp.toPx(), center = Offset(kx, ky + 2.dp.toPx()))
            drawCircle(Color.White, radius = ringPx * 0.62f, center = Offset(kx, ky))
        }

        // Bell on the knob
        Icon(Icons.Filled.Notifications, contentDescription = "Alarm time",
            tint = scheme.primary, modifier = Modifier.size(22.dp).offset(
                x = box / 2 + ringR * sin(knobAngle) - 11.dp,
                y = box / 2 - ringR * cos(knobAngle) - 11.dp))

        DialLabels(box, innerR)
    }
}

/** Hour labels (12AM, 2, 4, 6AM … 10) plus the night/day markers inside a 24h dial of width [box]. */
@Composable
internal fun BoxScope.DialLabels(box: androidx.compose.ui.unit.Dp, innerR: androidx.compose.ui.unit.Dp) {
    val scheme = MaterialTheme.colorScheme
        // Clockwise from the top: 12PM, 2, 4, 6PM, 8, 10, 12AM, 2, 4, 6AM, 8, 10
        val labelR = innerR * 0.74f
        for (i in 0 until 12) {
            val main = i % 3 == 0
            val text = when (i) { 0 -> "12PM"; 3 -> "6PM"; 6 -> "12AM"; 9 -> "6AM"; else -> ((i * 2) % 12).toString() }
            val a = i * Math.PI.toFloat() / 6f
            Box(Modifier.size(48.dp).offset(
                x = box / 2 + labelR * sin(a) - 24.dp,
                y = box / 2 - labelR * cos(a) - 24.dp), contentAlignment = Alignment.Center) {
                Text(text, fontSize = if (main) 15.sp else 14.sp,
                    fontWeight = if (main) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (main) scheme.onSurface else scheme.onSurfaceVariant)
            }
        }

        // Night / day markers
        Icon(Icons.Outlined.NightsStay, null, tint = Color(0xFF3AAFA9),
            modifier = Modifier.size(22.dp).offset(x = box / 2 - 11.dp, y = box / 2 + innerR * 0.38f - 11.dp))
        Icon(Icons.Outlined.WbSunny, null, tint = Color(0xFFFFB300),
            modifier = Modifier.size(22.dp).offset(x = box / 2 - 11.dp, y = box / 2 - innerR * 0.38f - 11.dp))
}

/** Minute of the day (0..1439, exact to the minute) at touch position [pos] on a 24h dial, 12 PM at the top. */
internal fun minutesAt(pos: Offset, size: IntSize): Int {
    val dx = pos.x - size.width / 2f
    val dy = pos.y - size.height / 2f
    var deg = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble()))
    if (deg < 0) deg += 360.0
    return ((deg / 360.0 * 1440.0).roundToInt() + DialTopMinute) % 1440
}

/** Is [pos] on the ring band (so scrolling over the dial face isn't hijacked)? */
internal fun androidx.compose.ui.unit.Density.nearRing(pos: Offset, size: IntSize): Boolean {
    val ringPx = RingWidth.toPx()
    val r = size.width.coerceAtMost(size.height) / 2f - ringPx / 2f - RingInset.toPx()
    val dist = hypot(pos.x - size.width / 2f, pos.y - size.height / 2f)
    return abs(dist - r) <= ringPx * 0.9f
}

/** Converts a touch position to a time on the 24h dial and reports it to the minute. */
private fun pick(pos: Offset, size: IntSize, onPick: (Int, Int) -> Unit) {
    val total = minutesAt(pos, size)
    onPick(total / 60, total % 60)
}
