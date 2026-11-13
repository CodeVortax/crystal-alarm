// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.sin

/**
 * Classic round clock: numbers, minute ticks, white hour/minute hands and a blue sweeping second hand.
 * [nowMillis] is read only while drawing, so a ticking value redraws the hands without recomposing.
 */
@Composable
fun AnalogClock(
    nowMillis: () -> Long,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val scheme = MaterialTheme.colorScheme
    val faceColor = scheme.surface
    val tickColor = scheme.onSurfaceVariant
    val handColor = scheme.onSurface
    val accent = scheme.primary

    BoxWithConstraints(modifier.aspectRatio(1f).background(faceColor, CircleShape), contentAlignment = Alignment.Center) {
        val box = maxWidth
        val numberR = box / 2 * 0.74f

        // Numbers 1..12
        for (n in 1..12) {
            val a = n * Math.PI.toFloat() / 6f
            // Children are centred in the face, so the offset is relative to the centre.
            Box(Modifier.size(36.dp).offset(x = numberR * sin(a), y = -(numberR * cos(a))),
                contentAlignment = Alignment.Center) {
                Text(n.toString(), fontSize = (box.value * 0.075f).sp, fontWeight = FontWeight.Normal,
                    color = scheme.onSurface)
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            // 60 ticks, longer at every 5th
            for (i in 0 until 60) {
                val a = Math.toRadians(i * 6.0 - 90.0)
                val len = if (i % 5 == 0) radius * 0.055f else radius * 0.03f
                val outer = radius * 0.95f
                drawLine(tickColor.copy(alpha = if (i % 5 == 0) 0.9f else 0.5f),
                    Offset(c.x + (outer - len) * cos(a).toFloat(), c.y + (outer - len) * sin(a).toFloat()),
                    Offset(c.x + outer * cos(a).toFloat(), c.y + outer * sin(a).toFloat()),
                    strokeWidth = if (i % 5 == 0) 2.dp.toPx() else 1.dp.toPx(), cap = StrokeCap.Round)
            }

            val t = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis()), zone)
            val sec = t.second + t.nano / 1_000_000_000f
            val min = t.minute + sec / 60f
            val hr = (t.hour % 12) + min / 60f

            fun hand(angleDeg: Float, length: Float, tail: Float, width: Float, color: androidx.compose.ui.graphics.Color) {
                val a = Math.toRadians(angleDeg - 90.0)
                val dx = cos(a).toFloat(); val dy = sin(a).toFloat()
                drawLine(color, Offset(c.x - dx * tail, c.y - dy * tail),
                    Offset(c.x + dx * length, c.y + dy * length), strokeWidth = width, cap = StrokeCap.Round)
            }
            hand(hr / 12f * 360f, radius * 0.5f, 0f, 7.dp.toPx(), handColor)
            hand(min / 60f * 360f, radius * 0.78f, 0f, 5.dp.toPx(), handColor)
            hand(sec / 60f * 360f, radius * 0.86f, radius * 0.2f, 2.5.dp.toPx(), accent)

            drawCircle(faceColor, radius = 6.dp.toPx(), center = c)
            drawCircle(accent, radius = 6.dp.toPx(), center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx()))
        }
    }
}
