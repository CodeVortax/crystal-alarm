// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.min

/**
 * A canvas-drawn clock face with a glass/glossy aesthetic:
 *  - Radial-gradient background (dark to deep accent)
 *  - Specular highlight sweep (animated slowly)
 *  - Blurred glass ring with inner shadow
 *  - Smooth, anti-aliased hands with rounded tips
 *  - Tick marks with varying opacity
 *  - Digital overlay faintly visible under the glass
 */
@Composable
fun GlassClockFace(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    showDigital: Boolean = true,
) {
    val textMeasurer = rememberTextMeasurer()
    var now by remember { mutableStateOf(java.time.LocalTime.now()) }

    // Tick every second
    LaunchedEffect(Unit) {
        while (true) {
            now = java.time.LocalTime.now()
            delay(1000L)
        }
    }

    // Blinking colon for digital — shows on even seconds
    val colon = (now.second % 2 == 0)

    val h = now.hour
    val m = now.minute
    val s = now.second
    val ms = now.nano / 1_000_000

    // Smooth second hand: use nano for smooth sweep
    val smoothSec = s + ms / 1000f
    val secAngle = smoothSec / 60f * 360f
    val minAngle = (m + s / 60f) / 60f * 360f
    val hourAngle = (h % 12 + m / 60f) / 12f * 360f

    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Canvas(modifier = modifier.fillMaxSize().padding(24.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy) * 0.85f
        val ringWidth = radius * 0.12f
        val innerR = radius - ringWidth

        // ── Background: radial gradient ──
        val bgColors = if (darkTheme)
            listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
        else
            listOf(Color(0xFFF8F9FA), Color(0xFFE8EAF6))
        drawCircle(
            brush = RadialGradient(
                colors = bgColors,
                center = Offset(cx, cy),
                radius = radius * 1.2f
            ),
            radius = radius + ringWidth + 20f
        )

        // ── Outer glass ring ──
        val ringColor = accentColor.copy(alpha = 0.30f)
        val glassHighlight = Color.White.copy(alpha = 0.18f)
        val glassShadow = Color.Black.copy(alpha = 0.12f)

        // Outer ring track
        drawCircle(
            color = ringColor,
            radius = radius,
            style = Stroke(width = ringWidth * 2f, cap = StrokeCap.Round)
        )

        // Glass highlight (top arc shimmer)
        drawArc(
            brush = SweepGradient(
                colors = listOf(
                    glassHighlight,
                    Color.Transparent,
                    Color.Transparent,
                    glassHighlight,
                    glassHighlight
                ),
                center = Offset(cx, cy),
                startAngle = -30f
            ),
            startAngle = 0f, sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = ringWidth * 2f, cap = StrokeCap.Round)
        )

        // ── Tick marks ──
        for (i in 0 until 60) {
            val angle = i * 6f - 90f
            val isHour = i % 5 == 0
            val tickLen = if (isHour) radius * 0.08f else radius * 0.04f
            val tickWidth = if (isHour) 3f else 1.5f
            val tickColor = if (isHour)
                accentColor.copy(alpha = 0.9f)
            else
                accentColor.copy(alpha = 0.35f)

            val inner = innerR - tickLen
            val outer = innerR
            val rad = angle * PI / 180f
            val x1 = cx + inner * cos(rad).toFloat()
            val y1 = cy + inner * sin(rad).toFloat()
            val x2 = cx + outer * cos(rad).toFloat()
            val y2 = cy + outer * sin(rad).toFloat()

            drawLine(
                color = tickColor,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = tickWidth,
                cap = StrokeCap.Round
            )
        }

        // ── Hour markers (large dots for 12, 3, 6, 9) ──
        for (i in 0 until 12) {
            if (i % 3 == 0) {
                val angle = i * 30f - 90f
                val rad = angle * PI / 180f
                val dotR = radius * 0.035f
                val dist = innerR - radius * 0.06f
                val dx = cx + dist * cos(rad).toFloat()
                val dy = cy + dist * sin(rad).toFloat()
                drawCircle(
                    color = accentColor.copy(alpha = 0.60f),
                    radius = dotR,
                    center = Offset(dx, dy),
                    alpha = 0.8f,
                    style = Stroke(width = 2f)
                )
                drawCircle(
                    color = accentColor.copy(alpha = 0.15f),
                    radius = dotR * 1.6f,
                    center = Offset(dx, dy),
                    alpha = 0.4f
                )
            }
        }

        // ── Hour hand ──
        val hLen = innerR * 0.55f
        val hAngleRad = hourAngle * PI / 180f
        drawHand(
            center = Offset(cx, cy),
            angleRad = hAngleRad.toFloat(),
            length = hLen,
            width = radius * 0.045f,
            color = accentColor.copy(alpha = 0.95f),
            glowColor = accentColor.copy(alpha = 0.25f)
        )

        // ── Minute hand ──
        val mLen = innerR * 0.72f
        val mAngleRad = minAngle * PI / 180f
        drawHand(
            center = Offset(cx, cy),
            angleRad = mAngleRad.toFloat(),
            length = mLen,
            width = radius * 0.035f,
            color = accentColor.copy(alpha = 0.95f),
            glowColor = accentColor.copy(alpha = 0.25f)
        )

        // ── Second hand ──
        val sLen = innerR * 0.80f
        val sAngleRad = secAngle * PI / 180f
        val secColor = accentColor.copy(alpha = 0.85f)
        drawLine(
            color = secColor,
            start = Offset(cx, cy),
            end = Offset(
                cx + sLen * cos(sAngleRad).toFloat(),
                cy + sLen * sin(sAngleRad).toFloat()
            ),
            strokeWidth = radius * 0.018f,
            cap = StrokeCap.Round
        )
        // Counterweight tail
        val tailLen = innerR * 0.12f
        drawLine(
            color = secColor.copy(alpha = 0.4f),
            start = Offset(cx, cy),
            end = Offset(
                cx - tailLen * cos(sAngleRad).toFloat(),
                cy - tailLen * sin(sAngleRad).toFloat()
            ),
            strokeWidth = radius * 0.015f,
            cap = StrokeCap.Round
        )

        // ── Center cap ──
        drawCircle(
            color = accentColor,
            radius = radius * 0.045f
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.6f),
            radius = radius * 0.025f
        )

        // ── Glass reflection (specular highlight) ──
        // A moving arc of white that sweeps slowly around the face
        val sweep = (System.currentTimeMillis() % 8000) / 8000f * 360f
        drawArc(
            brush = SweepGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.0f),
                    Color.White.copy(alpha = 0.06f),
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.06f),
                    Color.White.copy(alpha = 0.0f),
                ),
                center = Offset(cx, cy),
                startAngle = sweep
            ),
            startAngle = 0f, sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(cx - innerR, cy - innerR),
            size = androidx.compose.ui.geometry.Size(innerR * 2, innerR * 2),
            style = Stroke(width = ringWidth * 0.6f, cap = StrokeCap.Butt)
        )

        // ── Digital clock overlay ──
        if (showDigital) {
            val digitalText = buildString {
                append(String.format("%02d", h % 12.let { if (it == 0) 12 else it }))
                append(if (colon) ":" else " ")
                append(String.format("%02d", m))
                append(if (h < 12) " AM" else " PM")
            }
            val style = TextStyle(
                color = accentColor.copy(alpha = 0.70f),
                fontSize = (radius * 0.13f).sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp
            )
            val result = textMeasurer.measure(
                text = digitalText,
                style = style,
            )
            val textX = cx - result.size.width / 2f
            val textY = cy + radius * 0.55f
            drawText(
                textLayoutResult = result,
                topLeft = Offset(textX, textY),
            )
        }

        // ── Subtle outer glow ──
        drawCircle(
            brush = RadialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.0f),
                    accentColor.copy(alpha = 0.06f),
                    accentColor.copy(alpha = 0.0f),
                ),
                center = Offset(cx, cy),
                radius = radius * 1.3f
            ),
            radius = radius * 1.3f,
            alpha = 0.6f
        )
    }
}

/** Draw a tapered hand with a soft glow for the glass effect. */
private fun DrawScope.drawHand(
    center: Offset,
    angleRad: Float,
    length: Float,
    width: Float,
    color: Color,
    glowColor: Color,
) {
    // Glow
    drawLine(
        color = glowColor,
        start = center,
        end = Offset(
            center.x + length * cos(angleRad).toFloat(),
            center.y + length * sin(angleRad).toFloat()
        ),
        strokeWidth = width * 3f,
        cap = StrokeCap.Round
    )
    // Main hand
    drawLine(
        color = color,
        start = center,
        end = Offset(
            center.x + length * cos(angleRad).toFloat(),
            center.y + length * sin(angleRad).toFloat()
        ),
        strokeWidth = width,
        cap = StrokeCap.Round
    )
    // Inner bright line (glass highlight on the hand)
    drawLine(
        color = Color.White.copy(alpha = 0.25f),
        start = center,
        end = Offset(
            center.x + length * 0.7f * cos(angleRad).toFloat(),
            center.y + length * 0.7f * sin(angleRad).toFloat()
        ),
        strokeWidth = width * 0.3f,
        cap = StrokeCap.Round
    )
}

/** Helper: approximate luminance of a color for dark-theme detection. */
private fun Color.luminance(): Double {
    val r = red * 255
    val g = green * 255
    val b = blue * 255
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
}