package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.NokiaDeviceEntity
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarDarkBorder
import com.example.ui.theme.RadarDarkSurface
import com.example.ui.theme.RadarGreen
import com.example.ui.theme.RadarThreatRed
import com.example.ui.theme.RadarThreatRedGlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun RadarScopeView(
    devices: List<NokiaDeviceEntity>,
    isScanning: Boolean,
    onDeviceClick: (NokiaDeviceEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepRotation"
    )

    val threatPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ThreatPulse"
    )

    Box(
        modifier = modifier
            .testTag("radar_scope_container"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("radar_scope_canvas")
                .pointerInput(devices) {
                    detectTapGestures { tapOffset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = minOf(size.width, size.height) / 2f - 16f

                        // Find closest device to tap
                        var closest: NokiaDeviceEntity? = null
                        var minDistance = 48.dp.toPx()

                        devices.forEach { dev ->
                            val rNorm = ((dev.lastRssi.coerceIn(-95, -30) - (-30f)) / (-65f))
                                .coerceIn(0.18f, 0.92f)
                            val r = rNorm * maxRadius
                            val rad = Math.toRadians(dev.radarAngleDeg.toDouble())
                            val x = center.x + (r * cos(rad)).toFloat()
                            val y = center.y + (r * sin(rad)).toFloat()

                            val dist = sqrt((tapOffset.x - x) * (tapOffset.x - x) + (tapOffset.y - y) * (tapOffset.y - y))
                            if (dist < minDistance) {
                                minDistance = dist
                                closest = dev
                            }
                        }

                        closest?.let { onDeviceClick(it) }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = minOf(size.width, size.height) / 2f - 16f
            if (maxRadius <= 10f) return@Canvas

            // Scope Background Circle
            drawCircle(
                color = RadarDarkSurface,
                radius = maxRadius,
                center = center
            )

            // Concentric Range Rings
            val ringIntervals = listOf(0.33f, 0.66f, 1.0f)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

            ringIntervals.forEachIndexed { index, fraction ->
                val r = maxRadius * fraction
                drawCircle(
                    color = RadarDarkBorder,
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.5f, pathEffect = if (index < 2) dashEffect else null)
                )
            }

            // Crosshair lines
            drawLine(
                color = RadarDarkBorder,
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.2f
            )
            drawLine(
                color = RadarDarkBorder,
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.2f
            )

            // Diagonal reference ticks
            val diagRadius = maxRadius * 0.95f
            for (angleDeg in listOf(45, 135, 225, 315)) {
                val rad = Math.toRadians(angleDeg.toDouble())
                val start = Offset(
                    center.x + (diagRadius * 0.92f * cos(rad)).toFloat(),
                    center.y + (diagRadius * 0.92f * sin(rad)).toFloat()
                )
                val end = Offset(
                    center.x + (diagRadius * cos(rad)).toFloat(),
                    center.y + (diagRadius * sin(rad)).toFloat()
                )
                drawLine(
                    color = RadarCyan.copy(alpha = 0.35f),
                    start = start,
                    end = end,
                    strokeWidth = 1.5f
                )
            }

            // Sweeping Beam Wedge / Radar Line
            if (isScanning) {
                val sweepRad = Math.toRadians(sweepAngle.toDouble())
                val beamEndPoint = Offset(
                    center.x + (maxRadius * cos(sweepRad)).toFloat(),
                    center.y + (maxRadius * sin(sweepRad)).toFloat()
                )

                // Radar beam line
                drawLine(
                    color = RadarCyan,
                    start = center,
                    end = beamEndPoint,
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )

                // Sweep arc glow
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.90f to Color.Transparent,
                        1.0f to RadarCyan.copy(alpha = 0.28f),
                        center = center
                    ),
                    startAngle = sweepAngle - 45f,
                    sweepAngle = 45f,
                    useCenter = true
                )
            }

            // Draw Detected Wi-Fi Blips
            devices.forEach { dev ->
                val rNorm = ((dev.lastRssi.coerceIn(-95, -30) - (-30f)) / (-65f))
                    .coerceIn(0.18f, 0.92f)
                val r = rNorm * maxRadius
                val rad = Math.toRadians(dev.radarAngleDeg.toDouble())
                val x = center.x + (r * cos(rad)).toFloat()
                val y = center.y + (r * sin(rad)).toFloat()
                val blipCenter = Offset(x, y)

                if (dev.isNokiaTarget) {
                    // NOKIA Threat Target - Critical Red High-Visibility Blip with Pulsing Aura
                    drawCircle(
                        color = RadarThreatRedGlow.copy(alpha = 0.45f),
                        radius = 16f * threatPulse,
                        center = blipCenter
                    )
                    drawCircle(
                        color = RadarThreatRed,
                        radius = 8.5f,
                        center = blipCenter
                    )
                    // Threat Target Reticle ring
                    drawCircle(
                        color = RadarThreatRed,
                        radius = 13f,
                        center = blipCenter,
                        style = Stroke(width = 1.8f)
                    )
                } else {
                    // Standard Ambient Wi-Fi Blip (Cyan / Green)
                    drawCircle(
                        color = RadarCyan.copy(alpha = 0.3f),
                        radius = 7.5f,
                        center = blipCenter
                    )
                    drawCircle(
                        color = RadarGreen,
                        radius = 4.5f,
                        center = blipCenter
                    )
                }
            }

            // Radar Center Pivot Indicator
            drawCircle(
                color = RadarCyan,
                radius = 4.5f,
                center = center
            )
            drawCircle(
                color = RadarCyan.copy(alpha = 0.35f),
                radius = 8f,
                center = center,
                style = Stroke(width = 1.5f)
            )
        }
    }
}
