// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.compose.foundation.layout.Box

/**
 * 24-hour sleep-schedule dial with two knobs: bedtime (bed icon) and wake-up (bell).
 * The band between them is the sleep window. Drag either knob, or tap the ring to move the nearest one.
 */
@Composable
fun SleepDial(
    bedMinutes: Int,
    wakeMinutes: Int,
    onBedChange: (Int) -> Unit,
    onWakeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val bed by rememberUpdatedState(bedMinutes)
    val wake by rememberUpdatedState(wakeMinutes)
    val latestBedChange by rememberUpdatedState(onBedChange)
    val latestWakeChange by rememberUpdatedState(onWakeChange)
    val trackColor = scheme.surfaceVariant
    val bandColor = scheme.primary.copy(alpha = 0.40f)
    val tickColor = scheme.onSurfaceVariant.copy(alpha = 0.55f)

    BoxWithConstraints(
        modifier.fillMaxWidth().aspectRatio(1f)
            .background(scheme.surface, RoundedCornerShape(32.dp)).padding(12.dp)
    ) {
        val box = maxWidth
        val ringR = box / 2 - RingWidth / 2 - RingInset
        val innerR = ringR - RingWidth / 2
        val bedAngle = dialAngleRad(bedMinutes)
        val wakeAngle = dialAngleRad(wakeMinutes)

        Box(Modifier.align(Alignment.Center).size(innerR * 2).background(scheme.background, RoundedCornerShape(50)))

        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { pos ->
                        if (nearRing(pos, size)) {
                            val m = minutesAt(pos, size)
                            if (nearestKnob(m, bed, wake) == 1) latestBedChange(m) else latestWakeChange(m)
                        }
                    })
                }
                .pointerInput(Unit) {
                    var active = 0 // 0 = none, 1 = bedtime knob, 2 = wake-up knob
                    detectDragGestures(
                        onDragStart = { pos ->
                            active = if (nearRing(pos, size)) nearestKnob(minutesAt(pos, size), bed, wake) else 0
                            val m = minutesAt(pos, size)
                            if (active == 1) latestBedChange(m) else if (active == 2) latestWakeChange(m)
                        },
                        onDragEnd = { active = 0 },
                        onDragCancel = { active = 0 },
                    ) { change, _ ->
                        if (active != 0) {
                            change.consume()
                            val m = minutesAt(change.position, size)
                            if (active == 1) latestBedChange(m) else latestWakeChange(m)
                        }
                    }
                }
        ) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val ringPx = RingWidth.toPx()
            val r = size.minDimension / 2f - ringPx / 2f - RingInset.toPx()

            drawCircle(trackColor, radius = r, center = c, style = Stroke(width = ringPx))

            // Sleep window: from bedtime clockwise to wake-up
            val sweep = (((wakeMinutes - bedMinutes) % 1440 + 1440) % 1440) / 1440f * 360f
            drawArc(bandColor, startAngle = dialAngleDeg(bedMinutes) - 90f, sweepAngle = sweep, useCenter = false,
                topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2f, r * 2f), style = Stroke(width = ringPx))

            for (i in 0 until 96) {
                val a = Math.toRadians(i * 3.75 - 90.0)
                val half = if (i % 4 == 0) ringPx * 0.28f else ringPx * 0.12f
                drawLine(tickColor,
                    Offset(c.x + (r - half) * cos(a).toFloat(), c.y + (r - half) * sin(a).toFloat()),
                    Offset(c.x + (r + half) * cos(a).toFloat(), c.y + (r + half) * sin(a).toFloat()),
                    strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
            }

            for (angle in floatArrayOf(bedAngle, wakeAngle)) {
                val kx = c.x + r * sin(angle)
                val ky = c.y - r * cos(angle)
                drawCircle(Color.Black.copy(alpha = 0.18f), radius = ringPx * 0.62f + 2.dp.toPx(), center = Offset(kx, ky + 2.dp.toPx()))
                drawCircle(Color.White, radius = ringPx * 0.62f, center = Offset(kx, ky))
            }
        }

        Icon(Icons.Filled.Bed, contentDescription = "Bedtime", tint = Color(0xFF6E6AE8),
            modifier = Modifier.size(22.dp).offset(
                x = box / 2 + ringR * sin(bedAngle) - 11.dp,
                y = box / 2 - ringR * cos(bedAngle) - 11.dp))
        Icon(Icons.Filled.Notifications, contentDescription = "Wake up", tint = scheme.primary,
            modifier = Modifier.size(22.dp).offset(
                x = box / 2 + ringR * sin(wakeAngle) - 11.dp,
                y = box / 2 - ringR * cos(wakeAngle) - 11.dp))

        DialLabels(box, innerR)
    }
}

/** 1 if [m] is closer (around the clock) to the bedtime knob, otherwise 2 for the wake-up knob. */
private fun nearestKnob(m: Int, bed: Int, wake: Int): Int {
    fun dist(a: Int, b: Int): Int { val d = abs(a - b); return min(d, 1440 - d) }
    return if (dist(m, bed) <= dist(m, wake)) 1 else 2
}
