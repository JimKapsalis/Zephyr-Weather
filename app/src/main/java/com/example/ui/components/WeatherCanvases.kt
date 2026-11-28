package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.CityWeatherBundle
import com.example.model.HourlyChartMode
import com.example.model.HourlyForecastItem
import com.example.model.RadarLayerMode
import com.example.model.SkyParticleEffect
import com.example.model.WeatherVisualSpec
import com.example.ui.theme.AmberSol
import com.example.ui.theme.CoralWarmth
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanZephyr
import com.example.ui.theme.EmeraldBreeze
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.example.R
import com.example.util.rememberTownSatelliteImageBitmap

/**
 * Immutable pre-computed particle descriptor so zero allocations occur during Canvas draw frames.
 */
data class WeatherParticleSpec(
    val id: Int,
    val baseX: Float,
    val baseY: Float,
    val speed: Float,
    val radiusFactor: Float,
    val baseAlpha: Float,
    val driftAmplitude: Float,
    val phaseOffset: Float,
    val layerIndex: Int
)

/**
 * Deterministically builds a list of particles tailored to the active [SkyParticleEffect].
 */
fun buildParticlesForEffect(
    effect: SkyParticleEffect,
    isCompactOverlay: Boolean
): List<WeatherParticleSpec> {
    val count = when (effect) {
        SkyParticleEffect.RAIN -> if (isCompactOverlay) 18 else 38
        SkyParticleEffect.THUNDER -> if (isCompactOverlay) 22 else 44
        SkyParticleEffect.SNOW -> if (isCompactOverlay) 18 else 36
        SkyParticleEffect.SNOW_STORM -> if (isCompactOverlay) 26 else 52
        SkyParticleEffect.CLOUDS -> if (isCompactOverlay) 6 else 10
        SkyParticleEffect.FOG -> if (isCompactOverlay) 8 else 14
        SkyParticleEffect.STARFIELD -> if (isCompactOverlay) 16 else 38
        SkyParticleEffect.SUNSET_DUSK -> if (isCompactOverlay) 10 else 20
        SkyParticleEffect.SUN_RAYS -> if (isCompactOverlay) 10 else 18
    }

    return List(count) { i ->
        val seed1 = ((i * 73 + 19) % 100) / 100f
        val seed2 = ((i * 41 + 11) % 100) / 100f
        val seed3 = ((i * 59 + 29) % 100) / 100f
        val layer = i % 3 // 0 = background, 1 = midground, 2 = foreground

        when (effect) {
            SkyParticleEffect.RAIN, SkyParticleEffect.THUNDER -> WeatherParticleSpec(
                id = i,
                baseX = seed1,
                baseY = seed2,
                speed = 2.0f + layer * 1.0f,
                radiusFactor = 1.05f + layer * 0.45f,
                baseAlpha = 0.42f + layer * 0.22f,
                driftAmplitude = 0.08f + layer * 0.025f,
                phaseOffset = seed3,
                layerIndex = layer
            )

            SkyParticleEffect.SNOW -> WeatherParticleSpec(
                id = i,
                baseX = seed1,
                baseY = seed2,
                speed = 1.0f + (i % 2),
                radiusFactor = 2.2f + layer * 1.35f,
                baseAlpha = 0.45f + layer * 0.20f,
                driftAmplitude = 16f + layer * 8f,
                phaseOffset = seed3 * (2f * PI.toFloat()),
                layerIndex = layer
            )

            SkyParticleEffect.SNOW_STORM -> WeatherParticleSpec(
                id = i,
                baseX = seed1,
                baseY = seed2,
                speed = 1.5f + layer * 0.5f,
                radiusFactor = 2.0f + layer * 1.4f,
                baseAlpha = 0.50f + layer * 0.22f,
                driftAmplitude = 0.18f + layer * 0.06f,
                phaseOffset = seed3 * (2f * PI.toFloat()),
                layerIndex = layer
            )

            SkyParticleEffect.CLOUDS -> WeatherParticleSpec(
                id = i,
                baseX = (i.toFloat() / count.coerceAtLeast(1)),
                baseY = 0.08f + (i % 5) * 0.10f,
                speed = if (i % 2 == 0) 1.0f else 2.0f,
                radiusFactor = 0.11f + (i % 3) * 0.035f,
                baseAlpha = 0.21f,
                driftAmplitude = 6f + (i % 3) * 4f,
                phaseOffset = seed3 * (2f * PI.toFloat()),
                layerIndex = layer
            )

            SkyParticleEffect.FOG -> WeatherParticleSpec(
                id = i,
                baseX = seed1,
                baseY = 0.12f + (i.toFloat() / count.coerceAtLeast(1)) * 0.78f,
                speed = if (i % 2 == 0) 1.0f else 1.5f,
                radiusFactor = 0.16f + (i % 3) * 0.05f,
                baseAlpha = 0.12f + (i % 3) * 0.04f,
                driftAmplitude = 8f + (i % 3) * 5f,
                phaseOffset = seed3 * (2f * PI.toFloat()),
                layerIndex = layer
            )

            SkyParticleEffect.STARFIELD -> WeatherParticleSpec(
                id = i,
                baseX = seed1,
                baseY = seed2 * 0.76f,
                speed = 1.0f + (i % 3),
                radiusFactor = when {
                    i % 7 == 0 -> 3.2f
                    i % 3 == 0 -> 2.3f
                    else -> 1.5f
                },
                baseAlpha = 0.38f + (i % 4) * 0.14f,
                driftAmplitude = 0.32f,
                phaseOffset = seed3 * (2f * PI.toFloat()),
                layerIndex = layer
            )

            SkyParticleEffect.SUNSET_DUSK -> WeatherParticleSpec(
                id = i,
                baseX = seed1,
                baseY = 0.06f + seed2 * 0.55f,
                speed = 1.0f,
                radiusFactor = if (i < 5) 0.13f + (i % 3) * 0.03f else 2.2f,
                baseAlpha = if (i < 5) 0.14f else 0.50f,
                driftAmplitude = 0.25f,
                phaseOffset = seed3 * (2f * PI.toFloat()),
                layerIndex = layer
            )

            SkyParticleEffect.SUN_RAYS -> WeatherParticleSpec(
                id = i,
                baseX = seed1,
                baseY = seed2 * 0.80f,
                speed = 1.0f,
                radiusFactor = 2.2f + (i % 3) * 1.4f,
                baseAlpha = 0.16f + (i % 3) * 0.08f,
                driftAmplitude = 10f + (i % 3) * 6f,
                phaseOffset = seed3 * (2f * PI.toFloat()),
                layerIndex = layer
            )
        }
    }
}

/**
 * Jetpack Compose Weather Particle System that renders subtle, condition-aware animations
 * (falling raindrops with splash ripples, drifting volumetric clouds, gently fluttering snowflakes,
 * twinkling night stars, sunset dusk clouds, and sunlit floating motes).
 *
 * State reads of [cyclePhaseState] happen strictly inside the [Canvas] DrawScope so zero
 * Composable recompositions are triggered during animation frames.
 */
@Composable
fun rememberIsAppResumed(): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            isResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return isResumed
}

@Composable
fun WeatherParticleSystem(
    visualSpec: WeatherVisualSpec,
    isCompactOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    val particles = remember(visualSpec.particleEffect, isCompactOverlay) {
        buildParticlesForEffect(visualSpec.particleEffect, isCompactOverlay)
    }

    val isResumed = rememberIsAppResumed()
    val cyclePhaseAnim = remember { Animatable(0f) }
    LaunchedEffect(visualSpec.particleEffect, isCompactOverlay, isResumed) {
        if (isResumed) {
            val cycleDurationMs = when (visualSpec.particleEffect) {
                SkyParticleEffect.RAIN, SkyParticleEffect.THUNDER -> if (isCompactOverlay) 5500 else 4200
                SkyParticleEffect.SNOW_STORM -> if (isCompactOverlay) 7000 else 5800
                else -> if (isCompactOverlay) 12000 else 10000
            }
            while (isResumed) {
                cyclePhaseAnim.snapTo(0f)
                cyclePhaseAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = cycleDurationMs, easing = LinearEasing)
                )
            }
        } else {
            cyclePhaseAnim.snapTo(0.25f)
        }
    }
    val cyclePhaseState: State<Float> = cyclePhaseAnim.asState()

    val boltPath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag(if (isCompactOverlay) "hero_weather_particle_system" else "sky_weather_particle_system")
    ) {
        val w = size.width
        val h = size.height
        val phase = cyclePhaseState.value
        val twoPi = (2.0 * PI).toFloat()

        when (visualSpec.particleEffect) {
            SkyParticleEffect.RAIN, SkyParticleEffect.THUNDER -> {
                val isThunder = visualSpec.particleEffect == SkyParticleEffect.THUNDER
                val isNightRain = visualSpec.isNight
                val flashWindow = (phase * 2f) % 1f
                val lightningIntensity = if (isThunder) {
                    when {
                        flashWindow in 0.10f..0.16f -> 0.85f
                        flashWindow in 0.19f..0.23f -> 0.50f
                        else -> 0f
                    }
                } else {
                    0f
                }

                // When raining at night, draw twinkling upper-sky nocturnal stars peeking between storm clouds
                if (isNightRain) {
                    for (s in 0 until 18) {
                        val sx = ((s * 67 + 13) % 100) / 100f * w
                        val sy = ((s * 43 + 7) % 100) / 100f * (h * 0.42f)
                        val twinkle = 0.45f + 0.55f * sin(phase * twoPi * (1 + (s % 2)) + s)
                        val starColor = if (s % 4 == 0) Color(0xFFFEF08A) else Color(0xFFE0F2FE)
                        drawCircle(
                            color = starColor.copy(alpha = (0.28f + 0.42f * twinkle).coerceIn(0.15f, 0.72f)),
                            radius = if (s % 5 == 0) 2.2f else 1.4f,
                            center = Offset(sx, sy)
                        )
                    }
                }

                // Multi-layered brooding volumetric cumulonimbus storm/rain cloud canopy (moonlit at night)
                val stormCloudCount = if (isCompactOverlay) 3 else 4
                for (c in 0 until stormCloudCount) {
                    val cloudDrift = ((c * 0.30f + phase * (0.14f + (c % 2) * 0.06f)) % 1.38f - 0.19f) * w
                    val cloudY = h * (if (isCompactOverlay) (0.10f + (c % 2) * 0.07f) else (0.06f + (c % 3) * 0.055f))
                    val cloudR = w * (if (isCompactOverlay) (0.18f + (c % 2) * 0.04f) else (0.22f + (c % 2) * 0.045f))
                    drawRealisticVolumetricCloud(
                        centerX = cloudDrift,
                        centerY = cloudY,
                        baseRadius = cloudR,
                        crestColor = when {
                            lightningIntensity > 0f && c % 2 == 0 ->
                                Color(0xFFFEF08A).copy(alpha = 0.36f * lightningIntensity + 0.20f)
                            isNightRain ->
                                Color(0xFFBAE6FD).copy(alpha = if (isCompactOverlay) 0.28f else 0.38f)
                            else ->
                                Color(0xFFCBD5E1).copy(alpha = if (isCompactOverlay) 0.24f else 0.32f)
                        },
                        bodyColor = if (isNightRain) {
                            Color(0xFF1E293B).copy(alpha = if (isCompactOverlay) 0.38f else 0.50f)
                        } else {
                            Color(0xFF475569).copy(alpha = if (isCompactOverlay) 0.28f else 0.38f)
                        },
                        shadowColor = if (isNightRain) {
                            Color(0xFF020617).copy(alpha = if (isCompactOverlay) 0.46f else 0.60f)
                        } else {
                            Color(0xFF0F172A).copy(alpha = if (isCompactOverlay) 0.34f else 0.48f)
                        },
                        rimLightColor = when {
                            lightningIntensity > 0f ->
                                Color(0xFFFDE047).copy(alpha = 0.45f * lightningIntensity)
                            isNightRain ->
                                Color(0xFFFEF9C3).copy(alpha = 0.34f)
                            else ->
                                Color(0xFF94A3B8).copy(alpha = 0.22f)
                        }
                    )
                }

                // Falling slanted rain streaks with crisp two-layer core + splash rings
                val maxDropY = if (isCompactOverlay) h * 0.95f else h * 0.90f
                particles.forEach { p ->
                    val progress = (p.baseY + phase * p.speed + p.phaseOffset) % 1f
                    val dropY = progress * maxDropY
                    val windShift = progress * w * p.driftAmplitude
                    val dropX = ((p.baseX * w - windShift) + w) % w
                    val streakLen = (if (isCompactOverlay) 24f else 44f) * p.radiusFactor
                    val slantX = streakLen * 0.24f
                    val startPt = Offset(dropX, dropY)
                    val midPt = Offset(dropX - slantX * 0.38f, dropY + streakLen * 0.38f)
                    val endPt = Offset(dropX - slantX, dropY + streakLen)

                    // Outer cyan/moonlit rain trail
                    drawLine(
                        color = (if (isNightRain) CyanGlow else CyanZephyr).copy(
                            alpha = (p.baseAlpha * (if (isNightRain) 0.85f else 0.74f)).coerceIn(0.26f, 0.84f)
                        ),
                        start = startPt,
                        end = endPt,
                        strokeWidth = 1.7f + p.layerIndex * 0.65f,
                        cap = StrokeCap.Round
                    )
                    // Bright silver-white raindrop tip
                    drawLine(
                        color = Color(0xFFE0F2FE).copy(alpha = (p.baseAlpha * 1.20f).coerceIn(0.42f, 0.95f)),
                        start = midPt,
                        end = endPt,
                        strokeWidth = 1.35f + p.layerIndex * 0.45f,
                        cap = StrokeCap.Round
                    )

                    // Subtle splash ripple when raindrop nears the bottom horizon
                    if (progress > 0.86f && p.layerIndex >= 1) {
                        val rippleProgress = (progress - 0.86f) / 0.14f
                        drawCircle(
                            color = Color(0xFFBAE6FD).copy(alpha = (1f - rippleProgress) * 0.52f),
                            radius = 2.5f + rippleProgress * (if (isCompactOverlay) 9f else 16f),
                            center = Offset(dropX - slantX, maxDropY),
                            style = Stroke(width = 1.3f)
                        )
                    }
                }

                // Periodic branched lightning flash & bolt during thunderstorm
                if (isThunder) {
                    val boltAlpha = if (lightningIntensity > 0f) lightningIntensity else 0.16f
                    val boltStartX = w * (if (isCompactOverlay) 0.76f else 0.72f)
                    val boltTopY = h * (if (isCompactOverlay) 0.08f else 0.05f)
                    val boltSpanY = h * (if (isCompactOverlay) 0.36f else 0.24f)
                    boltPath.reset()
                    boltPath.moveTo(boltStartX, boltTopY)
                    boltPath.lineTo(boltStartX - 18f, boltTopY + boltSpanY * 0.38f)
                    boltPath.lineTo(boltStartX + 8f, boltTopY + boltSpanY * 0.46f)
                    boltPath.lineTo(boltStartX - 24f, boltTopY + boltSpanY)
                    drawPath(
                        path = boltPath,
                        color = Color(0xFFFDE047).copy(alpha = boltAlpha * 0.40f),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = boltPath,
                        color = Color(0xFFFEF9C3).copy(alpha = boltAlpha),
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            SkyParticleEffect.CLOUDS -> {
                val isNightClouds = visualSpec.isNight
                val lightPeekCenter = Offset(w * 0.78f, h * 0.15f)
                val pulse = 0.94f + 0.06f * sin(phase * twoPi)
                if (isNightClouds) {
                    // Twinkling nocturnal stars behind the drifting night cloud deck
                    for (s in 0 until 22) {
                        val sx = ((s * 61 + 17) % 100) / 100f * w
                        val sy = ((s * 37 + 9) % 100) / 100f * (h * 0.56f)
                        val twinkle = 0.45f + 0.55f * sin(phase * twoPi * (1 + (s % 2)) + s)
                        drawCircle(
                            color = (if (s % 4 == 0) Color(0xFFFEF08A) else Color.White).copy(alpha = 0.30f + 0.50f * twinkle),
                            radius = if (s % 5 == 0) 2.2f else 1.4f,
                            center = Offset(sx, sy)
                        )
                    }
                    // Soft silver-blue lunar aura behind clouds
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFEF9C3).copy(alpha = 0.28f),
                                Color(0xFF93C5FD).copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = lightPeekCenter,
                            radius = w * 0.36f * pulse
                        ),
                        center = lightPeekCenter,
                        radius = w * 0.36f * pulse
                    )
                } else {
                    // Daytime warm golden crepuscular sunbeams
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFFBEB).copy(alpha = 0.34f),
                                AmberSol.copy(alpha = 0.22f),
                                AmberSol.copy(alpha = 0.07f),
                                Color.Transparent
                            ),
                            center = lightPeekCenter,
                            radius = w * 0.38f * pulse
                        ),
                        center = lightPeekCenter,
                        radius = w * 0.38f * pulse
                    )
                }

                particles.forEach { p ->
                    val parallaxSpeed = 0.28f + p.layerIndex * 0.18f
                    val wrappedX = ((p.baseX + phase * p.speed * parallaxSpeed) % 1.44f - 0.22f) * w
                    val bobbingY = p.baseY * h + sin(phase * twoPi + p.phaseOffset) * p.driftAmplitude
                    val baseR = w * (p.radiusFactor * (0.95f + p.layerIndex * 0.16f))
                    val layerAlphaScale = when (p.layerIndex) {
                        0 -> 0.72f
                        1 -> 0.94f
                        else -> 1.18f
                    }
                    val effectiveAlpha = (p.baseAlpha * layerAlphaScale).coerceIn(0.14f, 0.52f)

                    drawRealisticVolumetricCloud(
                        centerX = wrappedX,
                        centerY = bobbingY,
                        baseRadius = baseR,
                        crestColor = if (isNightClouds) {
                            Color(0xFFE0F2FE).copy(alpha = (effectiveAlpha * 1.45f).coerceAtMost(0.56f))
                        } else {
                            Color.White.copy(alpha = (effectiveAlpha * 1.45f).coerceAtMost(0.58f))
                        },
                        bodyColor = if (isNightClouds) {
                            Color(0xFF475569).copy(alpha = effectiveAlpha * 1.10f)
                        } else {
                            Color(0xFFE2E8F0).copy(alpha = effectiveAlpha)
                        },
                        shadowColor = if (isNightClouds) {
                            Color(0xFF091326).copy(alpha = effectiveAlpha * 1.15f)
                        } else {
                            Color(0xFF334155).copy(alpha = effectiveAlpha * 0.78f)
                        },
                        rimLightColor = if (isNightClouds) {
                            Color(0xFFFEF9C3).copy(alpha = effectiveAlpha * 1.05f)
                        } else {
                            Color(0xFFFEF3C7).copy(alpha = effectiveAlpha * 0.95f)
                        }
                    )
                }
            }

            SkyParticleEffect.FOG -> {
                // Foggy: Soft volumetric mist pillows and rolling low-visibility atmospheric fog banks
                particles.forEach { p ->
                    val dir = if (p.id % 2 == 0) 1f else -1f
                    val swayX = dir * sin(phase * twoPi * p.speed + p.phaseOffset) * (w * 0.10f)
                    val bandCenterX = (p.baseX * w + swayX).coerceIn(-w * 0.25f, w * 1.25f)
                    val bandY = p.baseY * h + cos(phase * twoPi + p.phaseOffset) * (p.driftAmplitude * 0.45f)
                    val bandWidth = w * (0.55f + p.radiusFactor * 1.2f)
                    val bandHeight = (if (isCompactOverlay) 22f else 38f) + p.layerIndex * 10f

                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFCBD5E1).copy(alpha = p.baseAlpha * 0.85f),
                                Color(0xFFF1F5F9).copy(alpha = p.baseAlpha * 1.30f),
                                Color(0xFFCBD5E1).copy(alpha = p.baseAlpha * 0.85f),
                                Color.Transparent
                            ),
                            startX = bandCenterX - bandWidth / 2f,
                            endX = bandCenterX + bandWidth / 2f
                        ),
                        topLeft = Offset(bandCenterX - bandWidth / 2f, bandY),
                        size = Size(bandWidth, bandHeight),
                        cornerRadius = CornerRadius(bandHeight / 2f, bandHeight / 2f)
                    )

                    if (p.id % 2 == 0) {
                        drawRealisticVolumetricCloud(
                            centerX = bandCenterX,
                            centerY = bandY + bandHeight * 0.2f,
                            baseRadius = w * (0.14f + (p.id % 3) * 0.03f),
                            crestColor = Color(0xFFF8FAFC).copy(alpha = p.baseAlpha * 0.90f),
                            bodyColor = Color(0xFFCBD5E1).copy(alpha = p.baseAlpha * 0.70f),
                            shadowColor = Color(0xFF475569).copy(alpha = p.baseAlpha * 0.45f),
                            rimLightColor = Color.White.copy(alpha = p.baseAlpha * 0.60f)
                        )
                    }
                }
            }

            SkyParticleEffect.SNOW -> {
                // Volumetric frosted winter snow clouds + 6-point crystalline snowflakes
                val snowCloudCount = if (isCompactOverlay) 2 else 4
                for (c in 0 until snowCloudCount) {
                    val cloudX = ((c * 0.32f + phase * (0.16f + (c % 2) * 0.05f)) % 1.38f - 0.19f) * w
                    val cloudY = h * (if (isCompactOverlay) (0.10f + (c % 2) * 0.06f) else (0.06f + (c % 2) * 0.05f))
                    drawRealisticVolumetricCloud(
                        centerX = cloudX,
                        centerY = cloudY,
                        baseRadius = w * (if (isCompactOverlay) 0.16f else 0.19f),
                        crestColor = Color.White.copy(alpha = if (isCompactOverlay) 0.24f else 0.30f),
                        bodyColor = Color(0xFFE0F2FE).copy(alpha = if (isCompactOverlay) 0.18f else 0.24f),
                        shadowColor = Color(0xFF475569).copy(alpha = 0.20f),
                        rimLightColor = Color(0xFFBAE6FD).copy(alpha = 0.28f)
                    )
                }

                val maxSnowY = if (isCompactOverlay) h * 0.96f else h * 0.88f
                particles.forEach { p ->
                    val progress = (p.baseY + phase * p.speed + p.phaseOffset / twoPi) % 1f
                    val snowY = progress * maxSnowY
                    val swayX = sin(progress * twoPi + p.phaseOffset) * p.driftAmplitude
                    val snowX = ((p.baseX * w + swayX) + w) % w
                    val centerPt = Offset(snowX, snowY)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = p.baseAlpha),
                                Color(0xFFBAE6FD).copy(alpha = p.baseAlpha * 0.60f),
                                Color.Transparent
                            ),
                            center = centerPt,
                            radius = p.radiusFactor * 1.7f
                        ),
                        radius = p.radiusFactor * 1.7f,
                        center = centerPt
                    )

                    if (p.layerIndex >= 1 && p.id % 2 == 0) {
                        val arm = p.radiusFactor * 1.45f
                        val diagX = arm * 0.866f
                        val diagY = arm * 0.5f
                        val crystalColor = Color.White.copy(alpha = (p.baseAlpha * 1.1f).coerceAtMost(0.92f))
                        drawLine(
                            color = crystalColor,
                            start = Offset(snowX, snowY - arm),
                            end = Offset(snowX, snowY + arm),
                            strokeWidth = 1.2f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = crystalColor,
                            start = Offset(snowX - diagX, snowY - diagY),
                            end = Offset(snowX + diagX, snowY + diagY),
                            strokeWidth = 1.2f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = crystalColor,
                            start = Offset(snowX + diagX, snowY - diagY),
                            end = Offset(snowX - diagX, snowY + diagY),
                            strokeWidth = 1.2f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            SkyParticleEffect.SNOW_STORM -> {
                // Intense Greek Winter Snow Storm / Blizzard: Brooding frost-rimmed storm clouds,
                // swirling icy gale wind ribbons, and high-velocity diagonal snow crystals
                val stormCloudCount = if (isCompactOverlay) 3 else 5
                for (c in 0 until stormCloudCount) {
                    val cloudX = ((c * 0.26f + phase * (0.22f + (c % 2) * 0.08f)) % 1.38f - 0.19f) * w
                    val cloudY = h * (if (isCompactOverlay) (0.09f + (c % 2) * 0.06f) else (0.05f + (c % 3) * 0.045f))
                    drawRealisticVolumetricCloud(
                        centerX = cloudX,
                        centerY = cloudY,
                        baseRadius = w * (if (isCompactOverlay) 0.18f else 0.21f),
                        crestColor = Color(0xFFF1F5F9).copy(alpha = if (isCompactOverlay) 0.26f else 0.34f),
                        bodyColor = Color(0xFF94A3B8).copy(alpha = if (isCompactOverlay) 0.24f else 0.32f),
                        shadowColor = Color(0xFF1E293B).copy(alpha = 0.38f),
                        rimLightColor = Color(0xFF7DD3FC).copy(alpha = 0.32f)
                    )
                }

                // Swirling blizzard wind gale streaks
                val galeCount = if (isCompactOverlay) 3 else 5
                for (g in 0 until galeCount) {
                    val galeProgress = (phase + g * 0.21f) % 1f
                    val galeX = (1.15f - galeProgress * 1.45f) * w
                    val galeY = h * (0.20f + g * 0.14f) + sin(galeProgress * twoPi) * 10f
                    val galeLen = w * (0.26f + (g % 2) * 0.10f)
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFBAE6FD).copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.32f),
                                Color.Transparent
                            ),
                            startX = galeX - galeLen,
                            endX = galeX + galeLen
                        ),
                        start = Offset(galeX - galeLen, galeY + 12f),
                        end = Offset(galeX + galeLen, galeY - 8f),
                        strokeWidth = 2.4f,
                        cap = StrokeCap.Round
                    )
                }

                // Wind-driven diagonal snowflakes & blowing ice pellets
                val maxSnowY = if (isCompactOverlay) h * 0.96f else h * 0.90f
                particles.forEach { p ->
                    val progress = (p.baseY + phase * p.speed + p.phaseOffset / twoPi) % 1f
                    val snowY = progress * maxSnowY
                    val windDrift = progress * w * p.driftAmplitude
                    val gustSway = sin(progress * twoPi + p.phaseOffset) * 14f
                    val snowX = ((p.baseX * w - windDrift + gustSway) + w * 2f) % w
                    val centerPt = Offset(snowX, snowY)

                    // Diagonal motion-blurred snow streak + crystal
                    drawLine(
                        color = Color(0xFFE0F2FE).copy(alpha = p.baseAlpha * 0.65f),
                        start = Offset(snowX + p.radiusFactor * 2.2f, snowY - p.radiusFactor * 2.8f),
                        end = centerPt,
                        strokeWidth = p.radiusFactor * 0.75f,
                        cap = StrokeCap.Round
                    )

                    drawCircle(
                        color = Color.White.copy(alpha = p.baseAlpha),
                        radius = p.radiusFactor * 1.15f,
                        center = centerPt
                    )

                    if (p.id % 3 == 0) {
                        val arm = p.radiusFactor * 1.55f
                        val diagX = arm * 0.866f
                        val diagY = arm * 0.5f
                        val crystalColor = Color.White.copy(alpha = (p.baseAlpha * 1.15f).coerceAtMost(0.95f))
                        drawLine(
                            color = crystalColor,
                            start = Offset(snowX, snowY - arm),
                            end = Offset(snowX, snowY + arm),
                            strokeWidth = 1.3f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = crystalColor,
                            start = Offset(snowX - diagX, snowY - diagY),
                            end = Offset(snowX + diagX, snowY + diagY),
                            strokeWidth = 1.3f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = crystalColor,
                            start = Offset(snowX + diagX, snowY - diagY),
                            end = Offset(snowX - diagX, snowY + diagY),
                            strokeWidth = 1.3f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            SkyParticleEffect.STARFIELD -> {
                // Twinkling night sky stars + rich moonlit volumetric nocturnal clouds
                val isCloudyNight = visualSpec.code >= 2
                particles.forEach { p ->
                    val twinkle = 0.5f + 0.5f * sin(phase * twoPi * p.speed + p.phaseOffset)
                    val alpha = (p.baseAlpha * (0.45f + 0.55f * twinkle)).coerceIn(0.12f, 0.95f)
                    val starColor = if (p.id % 5 == 0) Color(0xFFFEF08A) else Color.White
                    val centerPt = Offset(p.baseX * w, p.baseY * h)
                    drawCircle(
                        color = starColor.copy(alpha = alpha * 0.32f),
                        radius = p.radiusFactor * (1.8f + 0.4f * twinkle),
                        center = centerPt
                    )
                    drawCircle(
                        color = starColor.copy(alpha = alpha),
                        radius = p.radiusFactor * (0.85f + 0.20f * twinkle),
                        center = centerPt
                    )
                }

                // Multi-layered moonlit silver-blue nocturnal clouds drifting across the night background
                val nightCloudCount = if (isCompactOverlay) 3 else if (isCloudyNight) 6 else 4
                for (c in 0 until nightCloudCount) {
                    val cloudX = ((0.12f + c * 0.24f + phase * (0.18f + (c % 3) * 0.05f)) % 1.42f - 0.21f) * w
                    val cloudY = h * (0.09f + (c % 3) * 0.085f) + sin(phase * twoPi + c) * 5f
                    val cloudR = w * (if (isCompactOverlay) (0.15f + (c % 2) * 0.03f) else (0.18f + (c % 3) * 0.035f))
                    drawRealisticVolumetricCloud(
                        centerX = cloudX,
                        centerY = cloudY,
                        baseRadius = cloudR,
                        crestColor = Color(0xFFE0F2FE).copy(alpha = if (isCloudyNight) 0.44f else 0.30f),
                        bodyColor = Color(0xFF475569).copy(alpha = if (isCloudyNight) 0.36f else 0.22f),
                        shadowColor = Color(0xFF091326).copy(alpha = if (isCloudyNight) 0.48f else 0.32f),
                        rimLightColor = Color(0xFFFEF9C3).copy(alpha = if (isCloudyNight) 0.38f else 0.26f)
                    )
                }

                // Subtle nocturnal rain/mist streaks when night sky is overcast (code >= 3)
                if (visualSpec.code >= 3) {
                    val mistDropCount = if (isCompactOverlay) 8 else 14
                    for (d in 0 until mistDropCount) {
                        val p = ((d * 0.17f) + phase * (1.5f + (d % 2) * 0.5f)) % 1f
                        val dx = ((((d * 71 + 19) % 100) / 100f * w - p * w * 0.06f) + w) % w
                        val dy = h * 0.14f + p * (h * 0.72f)
                        val len = if (isCompactOverlay) 18f else 30f
                        drawLine(
                            color = CyanGlow.copy(alpha = 0.28f),
                            start = Offset(dx, dy),
                            end = Offset(dx - len * 0.22f, dy + len),
                            strokeWidth = 1.4f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            SkyParticleEffect.SUNSET_DUSK -> {
                // Drifting golden-rose volumetric twilight clouds & emerging evening stars
                particles.forEach { p ->
                    if (p.id < 5) {
                        val cloudX = ((p.baseX + phase * (0.28f + (p.id % 2) * 0.10f)) % 1.38f - 0.19f) * w
                        val cloudY = p.baseY * h
                        val baseR = w * (p.radiusFactor * 1.15f)
                        drawRealisticVolumetricCloud(
                            centerX = cloudX,
                            centerY = cloudY,
                            baseRadius = baseR,
                            crestColor = Color(0xFFFDE68A).copy(alpha = (p.baseAlpha * 1.6f).coerceAtMost(0.36f)),
                            bodyColor = Color(0xFFFB923C).copy(alpha = (p.baseAlpha * 1.3f).coerceAtMost(0.28f)),
                            shadowColor = Color(0xFF881337).copy(alpha = (p.baseAlpha * 1.1f).coerceAtMost(0.24f)),
                            rimLightColor = Color(0xFFFEF08A).copy(alpha = (p.baseAlpha * 1.8f).coerceAtMost(0.42f))
                        )
                    } else {
                        val twinkle = 0.5f + 0.5f * sin(phase * twoPi + p.phaseOffset)
                        drawCircle(
                            color = Color(0xFFFFF7ED).copy(alpha = p.baseAlpha * (0.5f + 0.5f * twinkle)),
                            radius = p.radiusFactor,
                            center = Offset(p.baseX * w, (p.baseY * 0.55f) * h)
                        )
                    }
                }
            }

            SkyParticleEffect.SUN_RAYS -> {
                // Rotating warm sunbeams, gentle fair-weather cirrus wisps & rising golden motes
                if (!isCompactOverlay) {
                    val sunCenter = Offset(w * 0.82f, h * 0.12f)
                    val rayCount = 8
                    val rotationOffset = phase * 45f // Seamless 45-degree symmetry step across 8 rays
                    for (r in 0 until rayCount) {
                        val angleDeg = r * (360f / rayCount) + rotationOffset
                        rotate(degrees = angleDeg, pivot = sunCenter) {
                            drawLine(
                                color = AmberSol.copy(alpha = 0.11f),
                                start = sunCenter,
                                end = Offset(sunCenter.x, sunCenter.y + h * 0.46f),
                                strokeWidth = w * 0.052f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Subtle fair-weather sunlit cumulus wisp drifting gently
                for (c in 0 until 2) {
                    val cloudX = ((0.18f + c * 0.48f + phase * 0.24f) % 1.38f - 0.19f) * w
                    val cloudY = h * (0.14f + c * 0.08f)
                    drawRealisticVolumetricCloud(
                        centerX = cloudX,
                        centerY = cloudY,
                        baseRadius = w * (if (isCompactOverlay) 0.12f else 0.15f),
                        crestColor = Color.White.copy(alpha = 0.18f),
                        bodyColor = Color(0xFFFFFBEB).copy(alpha = 0.12f),
                        shadowColor = Color(0xFF94A3B8).copy(alpha = 0.08f),
                        rimLightColor = Color(0xFFFEF08A).copy(alpha = 0.20f)
                    )
                }

                // Floating sunlit pollen/light motes
                particles.forEach { p ->
                    val floatY = ((p.baseY - phase * 0.35f + 1f) % 1f) * (h * 0.78f)
                    val floatX = p.baseX * w + sin(phase * twoPi + p.phaseOffset) * p.driftAmplitude
                    val shimmer = 0.6f + 0.4f * sin(phase * twoPi + p.phaseOffset)
                    drawCircle(
                        color = Color(0xFFFEF08A).copy(alpha = p.baseAlpha * shimmer),
                        radius = p.radiusFactor,
                        center = Offset(floatX, floatY)
                    )
                }
            }
        }
    }
}

/**
 * Renders a realistic multi-lobe volumetric cloud bank with feathered radial gradients,
 * an atmospheric flat condensation underbelly shadow, and sunlit/moonlit upper crest highlights.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRealisticVolumetricCloud(
    centerX: Float,
    centerY: Float,
    baseRadius: Float,
    crestColor: Color,
    bodyColor: Color,
    shadowColor: Color,
    rimLightColor: Color
) {
    if (baseRadius <= 1f) return

    // 1. Flat condensation base & soft underbelly shadow
    val baseWidth = baseRadius * 3.1f
    val baseHeight = baseRadius * 0.72f
    val baseTopY = centerY + baseRadius * 0.08f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                bodyColor.copy(alpha = bodyColor.alpha * 0.75f),
                shadowColor,
                Color.Transparent
            ),
            startY = baseTopY - baseHeight * 0.25f,
            endY = baseTopY + baseHeight * 1.15f
        ),
        topLeft = Offset(centerX - baseWidth / 2f, baseTopY),
        size = Size(baseWidth, baseHeight),
        cornerRadius = CornerRadius(baseHeight / 2f, baseHeight / 2f)
    )

    // 2. Three billowing cumulus lobes with soft feathered radial edges (fast GPU draw with zero hard edges)
    val lobeOffsetsX = floatArrayOf(-0.82f, -0.06f, 0.78f)
    val lobeOffsetsY = floatArrayOf(0.08f, -0.22f, 0.10f)
    val lobeRadii = floatArrayOf(0.88f, 1.22f, 0.84f)

    for (i in lobeOffsetsX.indices) {
        val lx = centerX + lobeOffsetsX[i] * baseRadius
        val ly = centerY + lobeOffsetsY[i] * baseRadius
        val lr = baseRadius * lobeRadii[i] * 1.25f
        val lightCenter = Offset(lx + lr * 0.14f, ly - lr * 0.18f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    if (i == 1) rimLightColor else crestColor,
                    crestColor,
                    bodyColor,
                    shadowColor.copy(alpha = shadowColor.alpha * 0.50f),
                    Color.Transparent
                ),
                center = lightCenter,
                radius = lr
            ),
            center = Offset(lx, ly),
            radius = lr
        )
    }
}

@Composable
fun WeatherSkyBackdrop(
    visualSpec: WeatherVisualSpec,
    modifier: Modifier = Modifier
) {
    val topColor by animateColorAsState(
        targetValue = visualSpec.gradientTop,
        animationSpec = tween(durationMillis = 600),
        label = "sky_top_color"
    )
    val midColor by animateColorAsState(
        targetValue = visualSpec.gradientMid,
        animationSpec = tween(durationMillis = 600),
        label = "sky_mid_color"
    )
    val bottomColor by animateColorAsState(
        targetValue = visualSpec.gradientBottom,
        animationSpec = tween(durationMillis = 600),
        label = "sky_bottom_color"
    )
    val accentTint by animateColorAsState(
        targetValue = visualSpec.iconTint,
        animationSpec = tween(durationMillis = 600),
        label = "sky_accent_tint"
    )

    val farAlpineRidgePath = remember { Path() }
    val distantRidgePath = remember { Path() }
    val horizonPath = remember { Path() }
    val sunBeamPath = remember { Path() }

    val isNightScene = visualSpec.isNight || visualSpec.particleEffect == SkyParticleEffect.STARFIELD
    val textureRes = when {
        visualSpec.particleEffect == SkyParticleEffect.SUNSET_DUSK -> R.drawable.img_sunset_dusk
        isNightScene -> R.drawable.img_night_moon
        else -> R.drawable.img_atmosphere_hero
    }
    val textureAlpha = when {
        visualSpec.particleEffect == SkyParticleEffect.SUNSET_DUSK -> 0.56f
        isNightScene && (visualSpec.particleEffect == SkyParticleEffect.RAIN || visualSpec.particleEffect == SkyParticleEffect.THUNDER) -> 0.52f
        isNightScene -> 0.60f
        visualSpec.particleEffect == SkyParticleEffect.SUN_RAYS -> 0.24f
        visualSpec.particleEffect == SkyParticleEffect.CLOUDS -> 0.42f
        visualSpec.particleEffect == SkyParticleEffect.SNOW || visualSpec.particleEffect == SkyParticleEffect.SNOW_STORM -> 0.36f
        else -> 0.34f
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Rich multi-stop Rayleigh/Mie atmospheric sky gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to topColor,
                        0.28f to topColor.copy(alpha = 0.96f),
                        0.56f to midColor,
                        0.82f to midColor.copy(alpha = 0.94f),
                        1.0f to bottomColor
                    )
                )
            )

            // 1b. Upper atmospheric zenith light scattering bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentTint.copy(alpha = 0.26f),
                        midColor.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.78f, h * 0.12f),
                    radius = w * 0.85f
                ),
                center = Offset(w * 0.78f, h * 0.12f),
                radius = w * 0.85f
            )

            // 1c. Warm Mie horizon scattering belt
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        accentTint.copy(alpha = 0.12f),
                        CyanZephyr.copy(alpha = 0.10f),
                        bottomColor.copy(alpha = 0.45f)
                    ),
                    startY = h * 0.48f,
                    endY = h
                )
            )
        }

        // 2. High-detail photographic sky atmosphere layer
        Image(
            painter = painterResource(id = textureRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = textureAlpha,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Realistic Celestial Bodies, Crepuscular Light Shafts & 3-Layer Mountain Horizon
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            if (isNightScene) {
                val moonCenter = Offset(w * 0.80f, h * 0.13f)
                val isStormyNight = visualSpec.particleEffect == SkyParticleEffect.RAIN ||
                    visualSpec.particleEffect == SkyParticleEffect.THUNDER ||
                    visualSpec.particleEffect == SkyParticleEffect.SNOW ||
                    visualSpec.particleEffect == SkyParticleEffect.SNOW_STORM
                val moonVisibility = if (isStormyNight) 0.76f else 1.0f

                // Lunar atmospheric ice-crystal halo shining behind night clouds & rain
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFEF9C3).copy(alpha = 0.32f * moonVisibility),
                            Color(0xFFBAE6FD).copy(alpha = 0.18f * moonVisibility),
                            Color(0xFF818CF8).copy(alpha = 0.08f * moonVisibility),
                            Color.Transparent
                        ),
                        center = moonCenter,
                        radius = w * 0.40f
                    ),
                    center = moonCenter,
                    radius = w * 0.40f
                )
                drawCircle(
                    color = Color(0xFFE0F2FE).copy(alpha = 0.25f * moonVisibility),
                    radius = w * 0.15f,
                    center = moonCenter,
                    style = Stroke(width = 1.2.dp.toPx())
                )
                // Subtle earthshine full lunar disc
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.14f * moonVisibility),
                    radius = w * 0.070f,
                    center = moonCenter
                )
                // Sunlit lunar crescent
                drawCircle(
                    color = Color(0xFFFEFCE8).copy(alpha = 0.94f * moonVisibility),
                    radius = w * 0.070f,
                    center = moonCenter
                )
                drawCircle(
                    color = topColor.copy(alpha = 0.90f),
                    radius = w * 0.058f,
                    center = Offset(moonCenter.x - w * 0.025f, moonCenter.y - w * 0.016f)
                )
            } else {
                when (visualSpec.particleEffect) {
                    SkyParticleEffect.SUN_RAYS -> {
                        val sunCenter = Offset(w * 0.82f, h * 0.11f)
                        // Crepuscular atmospheric light shafts (God rays) fanning diagonally downward
                        val beamAngles = floatArrayOf(-0.62f, -0.38f, -0.16f, 0.08f)
                        val beamWidths = floatArrayOf(0.14f, 0.20f, 0.16f, 0.12f)
                        for (i in beamAngles.indices) {
                            val baseX = sunCenter.x + beamAngles[i] * w
                            val span = beamWidths[i] * w
                            sunBeamPath.reset()
                            sunBeamPath.moveTo(sunCenter.x - w * 0.02f, sunCenter.y)
                            sunBeamPath.lineTo(sunCenter.x + w * 0.02f, sunCenter.y)
                            sunBeamPath.lineTo(baseX + span, h * 0.88f)
                            sunBeamPath.lineTo(baseX - span, h * 0.88f)
                            sunBeamPath.close()
                            drawPath(
                                path = sunBeamPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFEF08A).copy(alpha = 0.16f - i * 0.025f),
                                        AmberSol.copy(alpha = 0.06f),
                                        Color.Transparent
                                    ),
                                    startY = sunCenter.y,
                                    endY = h * 0.88f
                                )
                            )
                        }
                        // Outer atmospheric solar corona
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFEF9C3).copy(alpha = 0.55f),
                                    AmberSol.copy(alpha = 0.34f),
                                    Color(0xFFF97316).copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                center = sunCenter,
                                radius = w * 0.52f
                            ),
                            center = sunCenter,
                            radius = w * 0.52f
                        )
                        // Optical atmospheric halo ring
                        drawCircle(
                            color = Color(0xFFFEF08A).copy(alpha = 0.24f),
                            radius = w * 0.21f,
                            center = sunCenter,
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                        // Blinding photosphere core
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFFEF9C3),
                                    AmberSol.copy(alpha = 0.85f)
                                ),
                                center = sunCenter,
                                radius = w * 0.095f
                            ),
                            center = sunCenter,
                            radius = w * 0.095f
                        )
                    }

                    SkyParticleEffect.SUNSET_DUSK -> {
                        val sunsetSunCenter = Offset(w * 0.78f, h * 0.21f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFEF08A).copy(alpha = 0.52f),
                                    Color(0xFFFB923C).copy(alpha = 0.34f),
                                    Color(0xFFE11D48).copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                center = sunsetSunCenter,
                                radius = w * 0.58f
                            ),
                            center = sunsetSunCenter,
                            radius = w * 0.58f
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFFBEB),
                                    Color(0xFFFDE047),
                                    Color(0xFFFB923C)
                                ),
                                center = sunsetSunCenter,
                                radius = w * 0.085f
                            ),
                            center = sunsetSunCenter,
                            radius = w * 0.085f
                        )
                    }

                    SkyParticleEffect.SNOW, SkyParticleEffect.SNOW_STORM -> {
                        val frostCenter = Offset(w * 0.55f, h * 0.14f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE0F2FE).copy(alpha = 0.30f),
                                    Color(0xFF7DD3FC).copy(alpha = 0.14f),
                                    Color.Transparent
                                ),
                                center = frostCenter,
                                radius = w * 0.62f
                            ),
                            center = frostCenter,
                            radius = w * 0.62f
                        )
                    }

                    else -> {}
                }
            }

            // 4a. Far Alpine / Pindus mountain range silhouette with aerial perspective haze
            val isSnowScene = visualSpec.particleEffect == SkyParticleEffect.SNOW ||
                visualSpec.particleEffect == SkyParticleEffect.SNOW_STORM
            farAlpineRidgePath.reset()
            farAlpineRidgePath.moveTo(0f, h * 0.78f)
            farAlpineRidgePath.cubicTo(
                w * 0.18f, h * 0.72f,
                w * 0.36f, h * 0.79f,
                w * 0.58f, h * 0.73f
            )
            farAlpineRidgePath.cubicTo(
                w * 0.76f, h * 0.69f,
                w * 0.90f, h * 0.76f,
                w, h * 0.74f
            )
            farAlpineRidgePath.lineTo(w, h)
            farAlpineRidgePath.lineTo(0f, h)
            farAlpineRidgePath.close()
            drawPath(
                path = farAlpineRidgePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (isSnowScene) Color(0xFFE0F2FE).copy(alpha = 0.26f) else midColor.copy(alpha = 0.32f),
                        bottomColor.copy(alpha = 0.42f)
                    ),
                    startY = h * 0.70f,
                    endY = h
                )
            )

            // 4b. Mid-distance mountain ridge with valley mist cushion
            distantRidgePath.reset()
            distantRidgePath.moveTo(0f, h * 0.83f)
            distantRidgePath.cubicTo(
                w * 0.22f, h * 0.78f,
                w * 0.48f, h * 0.85f,
                w * 0.74f, h * 0.79f
            )
            distantRidgePath.cubicTo(
                w * 0.88f, h * 0.77f,
                w * 0.95f, h * 0.81f,
                w, h * 0.80f
            )
            distantRidgePath.lineTo(w, h)
            distantRidgePath.lineTo(0f, h)
            distantRidgePath.close()
            drawPath(
                path = distantRidgePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (isSnowScene) Color(0xFFBAE6FD).copy(alpha = 0.32f) else midColor.copy(alpha = 0.48f),
                        bottomColor.copy(alpha = 0.68f)
                    ),
                    startY = h * 0.77f,
                    endY = h
                )
            )

            // Soft valley fog band between mid and foreground ridges
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (isSnowScene) 0.14f else 0.07f),
                        Color.Transparent
                    ),
                    startY = h * 0.80f,
                    endY = h * 0.90f
                )
            )

            // 4c. Foreground coastal/valley horizon silhouette
            horizonPath.reset()
            horizonPath.moveTo(0f, h * 0.88f)
            horizonPath.cubicTo(
                w * 0.28f, h * 0.85f,
                w * 0.65f, h * 0.91f,
                w, h * 0.87f
            )
            horizonPath.lineTo(w, h)
            horizonPath.lineTo(0f, h)
            horizonPath.close()
            drawPath(
                path = horizonPath,
                color = bottomColor.copy(alpha = 0.84f)
            )
        }

        // 5. Animated Weather Particle System (falling rain, drifting clouds, snow, snow storm, stars, or sun motes)
        WeatherParticleSystem(
            visualSpec = visualSpec,
            isCompactOverlay = false,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Weather scene overlay rendered inside the Hero Weather Card with subtle animated weather particles.
 */
@Composable
fun HeroWeatherSceneOverlay(
    visualSpec: WeatherVisualSpec,
    modifier: Modifier = Modifier
) {
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f)) }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val focal = Offset(w * 0.84f, h * 0.32f)

            for (ring in 1..3) {
                drawCircle(
                    color = visualSpec.iconTint.copy(alpha = 0.12f / ring),
                    radius = w * 0.14f * ring,
                    center = focal,
                    style = Stroke(
                        width = 1.4.dp.toPx(),
                        pathEffect = if (ring == 2) dashEffect else null
                    )
                )
            }

            if (visualSpec.isNight || visualSpec.particleEffect == SkyParticleEffect.STARFIELD) {
                // Nocturnal lunar glow + soft moonlit clouds inside Hero Card
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFEF9C3).copy(alpha = 0.22f),
                            Color(0xFF38BDF8).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = focal,
                        radius = w * 0.34f
                    ),
                    center = focal,
                    radius = w * 0.34f
                )
                drawRealisticVolumetricCloud(
                    centerX = w * 0.72f,
                    centerY = h * 0.28f,
                    baseRadius = w * 0.13f,
                    crestColor = Color(0xFFE0F2FE).copy(alpha = 0.24f),
                    bodyColor = Color(0xFF475569).copy(alpha = 0.18f),
                    shadowColor = Color(0xFF091326).copy(alpha = 0.26f),
                    rimLightColor = Color(0xFFFEF9C3).copy(alpha = 0.24f)
                )
            }

            when (visualSpec.particleEffect) {
                SkyParticleEffect.SUN_RAYS -> {
                    drawCircle(
                        color = AmberSol.copy(alpha = 0.15f),
                        center = focal,
                        radius = w * 0.30f
                    )
                }

                SkyParticleEffect.SUNSET_DUSK -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFB923C).copy(alpha = 0.26f),
                                Color(0xFFE11D48).copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = focal,
                            radius = w * 0.36f
                        ),
                        center = focal,
                        radius = w * 0.36f
                    )
                }

                SkyParticleEffect.RAIN, SkyParticleEffect.THUNDER -> {
                    for (i in 0..7) {
                        val rx = w * (0.52f + i * 0.06f)
                        val ry = h * (0.14f + (i % 3) * 0.20f)
                        drawLine(
                            color = CyanGlow.copy(alpha = 0.32f),
                            start = Offset(rx, ry),
                            end = Offset(rx - 8f, ry + 24f),
                            strokeWidth = 1.6f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                else -> {}
            }
        }
    }
}

/**
 * Dynamic Compose animation composable that renders real-time weather condition animations
 * (such as rotating golden sun rays, falling rain streaks with splash ripples, forked thunderstorm
 * lightning, drifting volumetric cumulus clouds, 6-point snow crystals, rolling fog banks, or
 * twinkling nocturnal stars) based on a WMO [weatherCode] and day/night state.
 *
 * Automatically pauses when the app leaves [Lifecycle.State.RESUMED] and performs all animation
 * state reads strictly inside [Canvas] draw phase so zero Composable recompositions occur.
 */
@Composable
fun WeatherConditionAnimation(
    weatherCode: Int,
    isDay: Boolean = true,
    isSunset: Boolean = false,
    modifier: Modifier = Modifier
) {
    val visualSpec = remember(weatherCode, isDay, isSunset) {
        com.example.model.WmoWeatherMapper.resolve(
            code = weatherCode,
            isDay = isDay,
            isSunset = isSunset
        )
    }
    WeatherConditionAnimation(
        visualSpec = visualSpec,
        modifier = modifier
    )
}

/**
 * Dynamic Compose animation composable that renders real-time weather condition animations
 * based on the resolved [WeatherVisualSpec].
 */
@Composable
fun WeatherConditionAnimation(
    visualSpec: WeatherVisualSpec,
    modifier: Modifier = Modifier
) {
    val isResumed = rememberIsAppResumed()
    val phaseAnim = remember { Animatable(0f) }
    LaunchedEffect(visualSpec.particleEffect, isResumed) {
        if (isResumed) {
            while (isResumed) {
                phaseAnim.snapTo(0f)
                phaseAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 9000, easing = LinearEasing)
                )
            }
        } else {
            phaseAnim.snapTo(0.25f)
        }
    }
    val phaseState: State<Float> = phaseAnim.asState()

    val boltPath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("weather_condition_animation")
    ) {
        val w = size.width
        val h = size.height
        val phase = phaseState.value
        val twoPi = (2.0 * PI).toFloat()

        // Subtle atmospheric horizon glow backdrop
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    visualSpec.gradientTop.copy(alpha = 0.38f),
                    visualSpec.gradientBottom.copy(alpha = 0.52f),
                    visualSpec.iconTint.copy(alpha = 0.22f)
                )
            ),
            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
        )

        when (visualSpec.particleEffect) {
            SkyParticleEffect.SUN_RAYS -> {
                // Rotating golden sunbeams, pulsing solar corona, and drifting warm light motes
                val sunCenter = Offset(w * 0.16f, h * 0.50f)
                val pulse = 0.92f + 0.08f * sin(phase * twoPi)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFBEB).copy(alpha = 0.65f),
                            AmberSol.copy(alpha = 0.34f),
                            Color.Transparent
                        ),
                        center = sunCenter,
                        radius = h * 0.90f * pulse
                    ),
                    radius = h * 0.90f * pulse,
                    center = sunCenter
                )

                val rayCount = 12
                val rotationOffset = phase * 360f
                for (r in 0 until rayCount) {
                    val angleDeg = r * (360f / rayCount) + rotationOffset
                    rotate(degrees = angleDeg, pivot = sunCenter) {
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFEF08A).copy(alpha = 0.55f),
                                    AmberSol.copy(alpha = 0.20f),
                                    Color.Transparent
                                ),
                                start = sunCenter,
                                end = Offset(sunCenter.x + h * 0.85f, sunCenter.y)
                            ),
                            start = Offset(sunCenter.x + h * 0.20f, sunCenter.y),
                            end = Offset(sunCenter.x + h * 0.82f, sunCenter.y),
                            strokeWidth = 2.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Diagonal crepuscular sunbeams sweeping across the banner
                for (beam in 0..3) {
                    val beamShift = ((phase + beam * 0.25f) % 1f)
                    val startX = w * (0.18f + beam * 0.20f) + sin(beamShift * twoPi) * 14f
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                AmberSol.copy(alpha = 0.24f),
                                Color(0xFFFEF08A).copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            start = Offset(startX, 0f),
                            end = Offset(startX + w * 0.14f, h)
                        ),
                        start = Offset(startX, 0f),
                        end = Offset(startX + w * 0.14f, h),
                        strokeWidth = 14.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Bright solar core
                drawCircle(
                    color = Color(0xFFFFFBEB),
                    radius = h * 0.22f * pulse,
                    center = sunCenter
                )
            }

            SkyParticleEffect.RAIN, SkyParticleEffect.THUNDER -> {
                val isThunder = visualSpec.particleEffect == SkyParticleEffect.THUNDER
                val flashActive = isThunder && (phase in 0.12f..0.18f || phase in 0.56f..0.61f)

                if (flashActive) {
                    drawRoundRect(
                        color = Color(0xFFFEF08A).copy(alpha = 0.24f),
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    )
                }

                // Drifting storm cloud canopy along the top edge
                for (c in 0..3) {
                    val cx = ((c * 0.28f + phase * 0.18f) % 1.28f - 0.14f) * w
                    drawRealisticVolumetricCloud(
                        centerX = cx,
                        centerY = h * 0.18f,
                        baseRadius = h * 0.62f,
                        crestColor = if (flashActive) Color(0xFFFEF08A).copy(alpha = 0.45f) else Color(0xFFCBD5E1).copy(alpha = 0.28f),
                        bodyColor = Color(0xFF475569).copy(alpha = 0.36f),
                        shadowColor = Color(0xFF0F172A).copy(alpha = 0.44f),
                        rimLightColor = CyanGlow.copy(alpha = 0.22f)
                    )
                }

                // Falling slanted raindrops + water splash ripples at the bottom
                val dropCount = 18
                for (i in 0 until dropCount) {
                    val seedX = ((i * 0.058f) + (i % 3) * 0.017f) % 1f
                    val speed = 1f + (i % 2)
                    val p = ((i * 0.13f) + phase * speed) % 1f
                    val dropX = ((seedX * w - p * w * 0.08f) + w) % w
                    val dropY = p * (h * 0.86f)
                    val len = h * (0.24f + (i % 3) * 0.05f)
                    val slant = len * 0.28f

                    drawLine(
                        color = CyanZephyr.copy(alpha = 0.78f),
                        start = Offset(dropX, dropY),
                        end = Offset(dropX - slant, dropY + len),
                        strokeWidth = 1.8f + (i % 2) * 0.6f,
                        cap = StrokeCap.Round
                    )

                    if (p > 0.82f && i % 2 == 0) {
                        val ripple = (p - 0.82f) / 0.18f
                        drawCircle(
                            color = CyanGlow.copy(alpha = (1f - ripple) * 0.50f),
                            radius = 2f + ripple * 10f,
                            center = Offset(dropX - slant, h * 0.90f),
                            style = Stroke(width = 1.2f)
                        )
                    }
                }

                if (isThunder) {
                    val boltAlpha = if (flashActive) 0.95f else 0.25f
                    val bx = w * 0.76f
                    boltPath.reset()
                    boltPath.moveTo(bx, h * 0.08f)
                    boltPath.lineTo(bx - 10f, h * 0.44f)
                    boltPath.lineTo(bx + 4f, h * 0.48f)
                    boltPath.lineTo(bx - 14f, h * 0.88f)
                    drawPath(
                        path = boltPath,
                        color = Color(0xFFFDE047).copy(alpha = boltAlpha),
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            SkyParticleEffect.CLOUDS -> {
                // Warm golden sunrays peeking behind drifting volumetric 3D cumulus clouds
                val sunPos = Offset(w * 0.20f, h * 0.42f)
                val sunPulse = 0.94f + 0.06f * sin(phase * twoPi)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AmberSol.copy(alpha = 0.48f),
                            AmberSol.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = sunPos,
                        radius = h * 0.78f * sunPulse
                    ),
                    radius = h * 0.78f * sunPulse,
                    center = sunPos
                )

                for (c in 0..3) {
                    val cx = ((c * 0.30f + phase * (0.20f + (c % 2) * 0.08f)) % 1.32f - 0.16f) * w
                    val cy = h * (0.42f + (c % 2) * 0.16f) + sin(phase * twoPi + c) * 3f
                    drawRealisticVolumetricCloud(
                        centerX = cx,
                        centerY = cy,
                        baseRadius = h * (0.52f + (c % 2) * 0.12f),
                        crestColor = Color.White.copy(alpha = 0.52f),
                        bodyColor = Color(0xFFE2E8F0).copy(alpha = 0.36f),
                        shadowColor = Color(0xFF334155).copy(alpha = 0.28f),
                        rimLightColor = Color(0xFFFEF3C7).copy(alpha = 0.42f)
                    )
                }
            }

            SkyParticleEffect.SNOW, SkyParticleEffect.SNOW_STORM -> {
                val isStorm = visualSpec.particleEffect == SkyParticleEffect.SNOW_STORM
                val flakeCount = if (isStorm) 20 else 14
                for (i in 0 until flakeCount) {
                    val speed = if (isStorm) (1.5f + (i % 2)) else 1.0f
                    val p = ((i * 0.09f) + phase * speed) % 1f
                    val sway = sin(p * twoPi + i) * (if (isStorm) 14f else 7f)
                    val windShift = if (isStorm) p * w * 0.18f else 0f
                    val fx = (((i.toFloat() / flakeCount) * w - windShift + sway) + w) % w
                    val fy = p * (h * 0.90f)
                    val r = 2.2f + (i % 3) * 1.1f

                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = r,
                        center = Offset(fx, fy)
                    )
                    if (i % 2 == 0) {
                        val arm = r * 1.6f
                        drawLine(
                            color = Color(0xFFE0F2FE).copy(alpha = 0.90f),
                            start = Offset(fx - arm, fy),
                            end = Offset(fx + arm, fy),
                            strokeWidth = 1.1f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFFE0F2FE).copy(alpha = 0.90f),
                            start = Offset(fx, fy - arm),
                            end = Offset(fx, fy + arm),
                            strokeWidth = 1.1f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            SkyParticleEffect.FOG -> {
                // Smoothly swaying horizontal mist ribbons
                for (band in 0..3) {
                    val dir = if (band % 2 == 0) 1f else -1f
                    val swayX = dir * sin(phase * twoPi + band) * (w * 0.08f)
                    val by = h * (0.18f + band * 0.20f)
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFE2E8F0).copy(alpha = 0.32f),
                                Color.White.copy(alpha = 0.45f),
                                Color(0xFFE2E8F0).copy(alpha = 0.32f),
                                Color.Transparent
                            )
                        ),
                        topLeft = Offset(w * 0.08f + swayX, by),
                        size = Size(w * 0.84f, 8.dp.toPx()),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }

            SkyParticleEffect.SUNSET_DUSK -> {
                // Glowing golden-crimson setting sun on horizon + drifting twilight clouds
                val sunCenter = Offset(w * 0.22f, h * 0.68f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFEF08A).copy(alpha = 0.75f),
                            Color(0xFFFB923C).copy(alpha = 0.42f),
                            Color(0xFFE11D48).copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = sunCenter,
                        radius = h * 0.85f
                    ),
                    radius = h * 0.85f,
                    center = sunCenter
                )
                for (c in 0..2) {
                    val cx = ((0.25f + c * 0.32f + phase * 0.20f) % 1.30f - 0.15f) * w
                    drawRealisticVolumetricCloud(
                        centerX = cx,
                        centerY = h * (0.36f + (c % 2) * 0.18f),
                        baseRadius = h * 0.48f,
                        crestColor = Color(0xFFFDE68A).copy(alpha = 0.42f),
                        bodyColor = Color(0xFFFB923C).copy(alpha = 0.30f),
                        shadowColor = Color(0xFF881337).copy(alpha = 0.26f),
                        rimLightColor = Color(0xFFFEF08A).copy(alpha = 0.48f)
                    )
                }
            }

            SkyParticleEffect.STARFIELD -> {
                // Twinkling starfield + glowing silver crescent moon + nocturnal clouds
                val moonCenter = Offset(w * 0.16f, h * 0.48f)
                drawCircle(
                    color = Color(0xFFFEF9C3).copy(alpha = 0.25f),
                    radius = h * 0.46f,
                    center = moonCenter
                )
                drawCircle(
                    color = Color(0xFFFEF08A).copy(alpha = 0.90f),
                    radius = h * 0.24f,
                    center = moonCenter
                )
                for (s in 0..11) {
                    val sx = ((s * 0.083f + 0.26f) % 0.94f) * w
                    val sy = ((s * 0.19f + 0.14f) % 0.78f) * h
                    val twinkle = 0.45f + 0.55f * sin(phase * twoPi * (1 + (s % 2)) + s)
                    drawCircle(
                        color = if (s % 3 == 0) Color(0xFFFEF08A).copy(alpha = twinkle) else Color.White.copy(alpha = twinkle),
                        radius = if (s % 4 == 0) 2.4f else 1.6f,
                        center = Offset(sx, sy)
                    )
                }
                for (c in 0..2) {
                    val cx = ((0.22f + c * 0.32f + phase * 0.20f) % 1.32f - 0.16f) * w
                    drawRealisticVolumetricCloud(
                        centerX = cx,
                        centerY = h * (0.40f + (c % 2) * 0.18f),
                        baseRadius = h * 0.46f,
                        crestColor = Color(0xFFE0F2FE).copy(alpha = 0.38f),
                        bodyColor = Color(0xFF64748B).copy(alpha = 0.28f),
                        shadowColor = Color(0xFF0F172A).copy(alpha = 0.38f),
                        rimLightColor = Color(0xFFFEF9C3).copy(alpha = 0.34f)
                    )
                }
            }
        }
    }
}

/**
 * Shared CompositionLocal phase ticker (`0f..1f`) so all visible weather icons animate at a locked
 * 60fps from a single coroutine ticker without spawning duplicate animation loops.
 */
val LocalSharedWeatherIconPhase = androidx.compose.runtime.staticCompositionLocalOf<State<Float>?> { null }

@Composable
fun ProvideSharedWeatherIconAnimation(content: @Composable () -> Unit) {
    val isResumed = rememberIsAppResumed()
    val sharedPhaseAnim = remember { Animatable(0f) }
    LaunchedEffect(isResumed) {
        if (isResumed) {
            while (isResumed) {
                sharedPhaseAnim.snapTo(0f)
                sharedPhaseAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 3200, easing = LinearEasing)
                )
            }
        } else {
            sharedPhaseAnim.snapTo(0.20f)
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalSharedWeatherIconPhase provides sharedPhaseAnim.asState(),
        content = content
    )
}

/**
 * Dynamic Compose-based animated weather status icon (e.g., spinning sun, oscillating rain drops,
 * drifting clouds, flashing thunderstorm, swaying mist, and rocking night moon with twinkling stars).
 */
@Composable
fun AnimatedWeatherStatusIcon(
    visualSpec: WeatherVisualSpec,
    iconSize: Dp = 22.dp,
    contentDescription: String? = visualSpec.label,
    animateContinuous: Boolean = true,
    modifier: Modifier = Modifier
) {
    val sharedPhase = LocalSharedWeatherIconPhase.current
    val isResumed = rememberIsAppResumed()
    val localPhaseAnim = remember { Animatable(0.20f) }
    LaunchedEffect(animateContinuous, isResumed, sharedPhase == null) {
        if (sharedPhase == null && animateContinuous && isResumed) {
            while (isResumed) {
                localPhaseAnim.snapTo(0f)
                localPhaseAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 3200, easing = LinearEasing)
                )
            }
        } else if (!animateContinuous || !isResumed) {
            localPhaseAnim.snapTo(0.20f)
        }
    }
    val phaseState: State<Float> = if (animateContinuous && sharedPhase != null) {
        sharedPhase
    } else {
        localPhaseAnim.asState()
    }

    val twoPi = (2.0 * PI).toFloat()

    Box(
        modifier = modifier.size(iconSize * 1.25f),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic condition-specific particles & corona read phaseState.value inside draw scope only (0 recompositions)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val phase = phaseState.value
            val sinWave = sin(phase * twoPi)
            val cosWave = cos(phase * twoPi)
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)
            val baseR = size.minDimension / 2f

            when (visualSpec.particleEffect) {
                SkyParticleEffect.SUN_RAYS -> {
                    val pulseR = baseR * (0.80f + 0.16f * sinWave)
                    drawCircle(
                        color = AmberSol.copy(alpha = 0.24f + 0.10f * sinWave),
                        radius = pulseR,
                        center = center
                    )
                }

                SkyParticleEffect.RAIN, SkyParticleEffect.THUNDER -> {
                    val isThunder = visualSpec.particleEffect == SkyParticleEffect.THUNDER
                    if (visualSpec.isNight) {
                        // Glowing nocturnal crescent moon peeking behind the rain cloud at night
                        val moonPos = Offset(center.x - baseR * 0.34f, center.y - baseR * 0.30f)
                        drawCircle(
                            color = Color(0xFFFEF08A).copy(alpha = 0.78f),
                            radius = baseR * 0.28f,
                            center = moonPos
                        )
                        drawCircle(
                            color = Color(0xFF0A192F),
                            radius = baseR * 0.22f,
                            center = Offset(moonPos.x - baseR * 0.10f, moonPos.y - baseR * 0.06f)
                        )
                    }
                    if (isThunder && (phase in 0.14f..0.24f || phase in 0.58f..0.66f)) {
                        drawCircle(
                            color = Color(0xFFFDE047).copy(alpha = 0.42f),
                            radius = baseR * 0.94f,
                            center = center
                        )
                    }
                    val dropOffsetsX = floatArrayOf(-0.28f, 0.0f, 0.28f)
                    val dropPhases = floatArrayOf(0.0f, 0.35f, 0.68f)
                    for (i in 0..2) {
                        val p = (phase * 2f + dropPhases[i]) % 1f
                        val startY = h * 0.56f + p * (h * 0.36f)
                        val startX = center.x + dropOffsetsX[i] * w - p * (w * 0.06f)
                        val dropAlpha = (1f - abs(p - 0.5f) * 1.5f).coerceIn(0.30f, 0.96f)
                        drawLine(
                            color = CyanZephyr.copy(alpha = dropAlpha),
                            start = Offset(startX, startY),
                            end = Offset(startX - w * 0.045f, startY + h * 0.14f),
                            strokeWidth = (iconSize.toPx() * 0.085f).coerceAtLeast(1.8f),
                            cap = StrokeCap.Round
                        )
                    }
                }

                SkyParticleEffect.CLOUDS, SkyParticleEffect.SUNSET_DUSK -> {
                    val sunCenter = Offset(center.x - baseR * 0.24f, center.y - baseR * 0.20f)
                    drawCircle(
                        color = AmberSol.copy(alpha = 0.26f + 0.10f * sinWave),
                        radius = baseR * 0.44f,
                        center = sunCenter
                    )
                }

                SkyParticleEffect.STARFIELD -> {
                    val starAlpha1 = 0.4f + 0.6f * abs(sinWave)
                    val starAlpha2 = 0.4f + 0.6f * abs(cosWave)
                    drawCircle(
                        color = Color(0xFFFEF08A).copy(alpha = starAlpha1),
                        radius = (baseR * 0.12f).coerceAtLeast(1.6f),
                        center = Offset(w * 0.78f, h * 0.24f)
                    )
                    drawCircle(
                        color = Color(0xFFBAE6FD).copy(alpha = starAlpha2),
                        radius = (baseR * 0.10f).coerceAtLeast(1.3f),
                        center = Offset(w * 0.24f, h * 0.28f)
                    )
                    // When night is cloudy (codes 2..3), draw an animated drifting silver-white cloud puff across the moon
                    if (visualSpec.code >= 2) {
                        val cloudDriftX = center.x + baseR * 0.10f + sinWave * (baseR * 0.14f)
                        val cloudY = center.y + baseR * 0.30f
                        drawCircle(
                            color = Color(0xFFE0F2FE).copy(alpha = 0.82f),
                            radius = baseR * 0.26f,
                            center = Offset(cloudDriftX, cloudY)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.88f),
                            radius = baseR * 0.21f,
                            center = Offset(cloudDriftX - baseR * 0.18f, cloudY + baseR * 0.04f)
                        )
                        drawCircle(
                            color = Color(0xFFBAE6FD).copy(alpha = 0.78f),
                            radius = baseR * 0.19f,
                            center = Offset(cloudDriftX + baseR * 0.18f, cloudY + baseR * 0.05f)
                        )
                    }
                }

                SkyParticleEffect.SNOW, SkyParticleEffect.SNOW_STORM -> {
                    val isStorm = visualSpec.particleEffect == SkyParticleEffect.SNOW_STORM
                    drawCircle(
                        color = Color(0xFFBAE6FD).copy(alpha = if (isStorm) 0.26f else 0.18f),
                        radius = baseR * (0.82f + 0.10f * sinWave),
                        center = center
                    )
                    val flakeOffsetsX = floatArrayOf(-0.28f, 0.02f, 0.28f)
                    val flakePhases = floatArrayOf(0.0f, 0.33f, 0.66f)
                    for (i in 0..2) {
                        val p = (phase * 1.5f + flakePhases[i]) % 1f
                        val fy = h * 0.56f + p * (h * 0.34f)
                        val fx = center.x + flakeOffsetsX[i] * w + sin(p * twoPi) * (w * 0.05f)
                        val alpha = (1f - abs(p - 0.5f) * 1.6f).coerceIn(0.30f, 0.95f)
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = (iconSize.toPx() * 0.068f).coerceAtLeast(1.5f),
                            center = Offset(fx, fy)
                        )
                    }
                }

                else -> {}
            }
        }

        Icon(
            imageVector = visualSpec.icon,
            contentDescription = contentDescription,
            tint = visualSpec.resolvedIconTint(),
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    if (animateContinuous) {
                        val phase = phaseState.value
                        val sinWave = sin(phase * twoPi)
                        val cosWave = cos(phase * twoPi)
                        rotationZ = when (visualSpec.particleEffect) {
                            SkyParticleEffect.SUN_RAYS -> phase * 360f
                            SkyParticleEffect.STARFIELD -> sinWave * 10f
                            SkyParticleEffect.THUNDER, SkyParticleEffect.SNOW_STORM -> sinWave * 5f
                            else -> sinWave * 3.2f
                        }
                        val sf = when (visualSpec.particleEffect) {
                            SkyParticleEffect.SUN_RAYS -> 1f + 0.08f * sinWave
                            SkyParticleEffect.THUNDER -> if (phase in 0.14f..0.24f || phase in 0.58f..0.66f) 1.14f else 1f + 0.05f * cosWave
                            else -> 1f + 0.055f * cosWave
                        }
                        scaleX = sf
                        scaleY = sf
                        translationX = when (visualSpec.particleEffect) {
                            SkyParticleEffect.CLOUDS, SkyParticleEffect.FOG, SkyParticleEffect.SUNSET_DUSK -> sinWave * 2.8f
                            SkyParticleEffect.SNOW_STORM -> sinWave * 3.2f
                            SkyParticleEffect.RAIN, SkyParticleEffect.THUNDER -> cosWave * 1.6f
                            else -> 0f
                        }
                        translationY = when (visualSpec.particleEffect) {
                            SkyParticleEffect.RAIN, SkyParticleEffect.THUNDER -> sinWave * 2.2f
                            SkyParticleEffect.SNOW, SkyParticleEffect.SNOW_STORM -> cosWave * 2.4f
                            SkyParticleEffect.CLOUDS, SkyParticleEffect.STARFIELD -> cosWave * 1.8f
                            else -> 0f
                        }
                    }
                }
        )
    }
}

/**
 * Dynamic animated hero weather badge featuring an orbiting satellite ring,
 * condition-aware particle effects, and a lifecycle-paused animated weather status icon.
 */
@Composable
fun LottieStyleWeatherHeroBadge(
    visualSpec: WeatherVisualSpec,
    triggerKey: Int,
    modifier: Modifier = Modifier
) {
    val entranceScale = remember { Animatable(1f) }

    LaunchedEffect(triggerKey) {
        if (triggerKey > 1) {
            entranceScale.snapTo(0.82f)
            entranceScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val sharedPhase = LocalSharedWeatherIconPhase.current
    val isResumed = rememberIsAppResumed()
    val localOrbitPhase = remember { Animatable(0.125f) }
    LaunchedEffect(sharedPhase == null, isResumed) {
        if (sharedPhase == null && isResumed) {
            while (isResumed) {
                localOrbitPhase.snapTo(0f)
                localOrbitPhase.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 4800, easing = LinearEasing)
                )
            }
        }
    }
    val phaseState: State<Float> = sharedPhase ?: localOrbitPhase.asState()

    val orbitalDash = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 10f)) }
    val twoPi = (2.0 * PI).toFloat()

    Box(
        modifier = modifier
            .size(86.dp)
            .testTag("hero_weather_icon_badge"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val phase = phaseState.value
            val orbitAngleDeg = phase * 360f
            val auraPulse = 1.0f + 0.06f * sin(phase * twoPi)
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 2f - 6.dp.toPx()

            // Pulsing circular aura
            drawCircle(
                color = visualSpec.iconTint.copy(alpha = 0.22f),
                center = center,
                radius = baseRadius * 1.08f * auraPulse
            )

            // Rotating dashed orbital ring
            rotate(degrees = orbitAngleDeg * 0.5f, pivot = center) {
                drawCircle(
                    color = visualSpec.iconTint.copy(alpha = 0.48f),
                    radius = baseRadius * 0.92f,
                    center = center,
                    style = Stroke(
                        width = 1.6.dp.toPx(),
                        pathEffect = orbitalDash
                    )
                )
            }
            val satRad = Math.toRadians(orbitAngleDeg.toDouble())
            val satX = center.x + (baseRadius * 0.92f * cos(satRad)).toFloat()
            val satY = center.y + (baseRadius * 0.92f * sin(satRad)).toFloat()
            drawCircle(
                color = visualSpec.iconTint,
                radius = 3.5.dp.toPx(),
                center = Offset(satX, satY)
            )

            // Inner circular glass medallion
            drawCircle(
                color = Color(0xFF0A192F).copy(alpha = 0.68f),
                radius = baseRadius * 0.74f,
                center = center
            )
            drawCircle(
                color = visualSpec.iconTint.copy(alpha = 0.52f),
                radius = baseRadius * 0.74f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Condition-specific badge accents for Partly Cloudy and Foggy states
            when (visualSpec.particleEffect) {
                SkyParticleEffect.CLOUDS -> {
                    drawCircle(
                        color = AmberSol.copy(alpha = 0.28f),
                        radius = baseRadius * 0.34f * auraPulse,
                        center = Offset(center.x - baseRadius * 0.24f, center.y - baseRadius * 0.22f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.22f),
                        radius = baseRadius * 0.28f,
                        center = Offset(center.x + baseRadius * 0.26f, center.y + baseRadius * 0.22f)
                    )
                }

                SkyParticleEffect.FOG -> {
                    for (band in 0..2) {
                        val by = center.y + baseRadius * (0.18f + band * 0.16f)
                        val halfSpan = baseRadius * (0.52f - band * 0.08f)
                        drawLine(
                            color = Color(0xFFBAE6FD).copy(alpha = 0.35f - band * 0.07f),
                            start = Offset(center.x - halfSpan, by),
                            end = Offset(center.x + halfSpan, by),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                else -> {}
            }
        }

        AnimatedWeatherStatusIcon(
            visualSpec = visualSpec,
            iconSize = 42.dp,
            contentDescription = visualSpec.label,
            animateContinuous = true,
            modifier = Modifier.graphicsLayer {
                scaleX = entranceScale.value
                scaleY = entranceScale.value
            }
        )
    }
}

private val RadarLayerMode.accentColor: Color
    get() = when (this) {
        RadarLayerMode.RAIN -> CyanZephyr
        RadarLayerMode.WIND -> EmeraldBreeze
        RadarLayerMode.CLOUDS -> Color(0xFFCBD5E1)
        RadarLayerMode.TEMP -> AmberSol
    }

/**
 * Circular Doppler Weather Radar Canvas displaying a real satellite aerial photo of the active town,
 * live rotating radar sweep, concentric range rings (10 km, 25 km, 50 km, 100 km), cardinal axes, and
 * weather cells for Rain, Wind, Clouds, and Thermal layers.
 */
@Composable
fun LiveWeatherRadarCanvas(
    bundle: CityWeatherBundle,
    layerMode: RadarLayerMode,
    timelineIndex: Int,
    language: AppLanguage,
    showSatellitePhoto: Boolean = true,
    satelliteOnlyMode: Boolean = false,
    satelliteZoom: Int = 13,
    mapStyle: com.example.util.SatelliteMapStyle = com.example.util.SatelliteMapStyle.HYBRID_SATELLITE,
    zoomScale: Float = 1f,
    panOffset: Offset = Offset.Zero,
    onZoomPanChange: ((Float, Offset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isResumed = rememberIsAppResumed()
    val sweepAngleAnim = remember { Animatable(120f) }
    LaunchedEffect(bundle.cityId, layerMode, timelineIndex, satelliteZoom, isResumed) {
        if (isResumed) {
            sweepAngleAnim.snapTo(0f)
            sweepAngleAnim.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = 2800, easing = LinearEasing)
            )
            sweepAngleAnim.snapTo(120f)
        } else {
            sweepAngleAnim.snapTo(120f)
        }
    }
    val sweepAngleState: State<Float> = sweepAngleAnim.asState()
    val cellPulseState: State<Float> = rememberUpdatedState(1.0f)
    val sweepAngle = sweepAngleState.value
    val cellPulse = cellPulseState.value
    val currentScaleState = rememberUpdatedState(zoomScale)
    val currentPanState = rememberUpdatedState(panOffset)

    val liveSatelliteBitmap = if (showSatellitePhoto) {
        rememberTownSatelliteImageBitmap(
            latitude = bundle.latitude,
            longitude = bundle.longitude,
            zoom = satelliteZoom,
            mapStyle = mapStyle
        )
    } else {
        null
    }

    val selectedHour = bundle.hourly.getOrNull(timelineIndex) ?: bundle.hourly.firstOrNull()
    val precipProb = selectedHour?.precipProbability ?: 0
    val precipMm = selectedHour?.precipMm ?: (if (timelineIndex == 0) bundle.current.precipitationMm else 0.0)
    val windSpeed = selectedHour?.windSpeedKmh ?: bundle.current.windSpeedKmh
    val cloudCover = selectedHour?.cloudCoverPercent ?: bundle.current.cloudCoverPercent
    val tempC = selectedHour?.tempC ?: bundle.current.tempC

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF061527))
            .then(
                if (onZoomPanChange != null) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { tapOffset ->
                                    val curScale = currentScaleState.value
                                    val nextScale = when {
                                        curScale < 1.8f -> 2.2f
                                        curScale < 3.2f -> 3.6f
                                        else -> 1.0f
                                    }
                                    if (nextScale <= 1.01f) {
                                        onZoomPanChange(1.0f, Offset.Zero)
                                    } else {
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val minDim = minOf(size.width, size.height).toFloat()
                                        val maxPan = (minDim / 2f) * (nextScale - 1f) * 0.72f
                                        val focusShift = (center - tapOffset) * 0.45f
                                        val clampedPan = Offset(
                                            focusShift.x.coerceIn(-maxPan, maxPan),
                                            focusShift.y.coerceIn(-maxPan, maxPan)
                                        )
                                        onZoomPanChange(nextScale, clampedPan)
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val curScale = currentScaleState.value
                                val curPan = currentPanState.value
                                val nextScale = (curScale * zoom).coerceIn(1.0f, 5.0f)
                                if (nextScale <= 1.01f) {
                                    onZoomPanChange(1.0f, Offset.Zero)
                                } else {
                                    val minDim = minOf(size.width, size.height).toFloat()
                                    val maxPan = (minDim / 2f) * (nextScale - 1f) * 0.75f
                                    val rawPan = curPan + pan
                                    val clampedPan = Offset(
                                        rawPan.x.coerceIn(-maxPan, maxPan),
                                        rawPan.y.coerceIn(-maxPan, maxPan)
                                    )
                                    onZoomPanChange(nextScale, clampedPan)
                                }
                            }
                        }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffset.x
                    translationY = panOffset.y
                },
            contentAlignment = Alignment.Center
        ) {
            if (showSatellitePhoto) {
                if (liveSatelliteBitmap != null) {
                    Image(
                        bitmap = liveSatelliteBitmap,
                        contentDescription = "Satellite photo of ${bundle.cityName}",
                        contentScale = ContentScale.Crop,
                        alpha = if (satelliteOnlyMode) 0.98f else 0.88f,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_satellite_town),
                        contentDescription = "Satellite aerial photo of ${bundle.cityName}",
                        contentScale = ContentScale.Crop,
                        alpha = if (satelliteOnlyMode) 0.96f else 0.74f,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = (size.minDimension / 2f) - 3.dp.toPx()

            // Subtle atmospheric vignette over satellite photo or dark ocean radar base
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (showSatellitePhoto) {
                        if (satelliteOnlyMode) {
                            listOf(
                                Color.Transparent,
                                Color(0xFF040D1A).copy(alpha = 0.12f),
                                Color(0xFF040D1A).copy(alpha = 0.45f)
                            )
                        } else {
                            listOf(
                                Color(0xFF061527).copy(alpha = 0.14f),
                                Color(0xFF061527).copy(alpha = 0.32f),
                                Color(0xFF040D1A).copy(alpha = 0.58f)
                            )
                        }
                    } else {
                        listOf(
                            Color(0xFF0A2239),
                            Color(0xFF061527),
                            Color(0xFF040D1A)
                        )
                    },
                    center = center,
                    radius = maxRadius
                ),
                center = center,
                radius = maxRadius
            )

            // Concentric radar range rings (25%, 50%, 75%, 100%)
            val ringFractions = listOf(0.25f, 0.50f, 0.75f, 1.0f)
            ringFractions.forEach { frac ->
                drawCircle(
                    color = CyanZephyr.copy(
                        alpha = when {
                            frac == 1.0f -> 0.55f
                            satelliteOnlyMode -> 0.18f
                            else -> 0.26f
                        }
                    ),
                    radius = maxRadius * frac,
                    center = center,
                    style = Stroke(
                        width = if (frac == 1.0f) 2.dp.toPx() else 1.dp.toPx(),
                        pathEffect = if (frac < 1.0f) PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) else null
                    )
                )
            }

            // Crosshair axes
            drawLine(
                color = CyanZephyr.copy(alpha = if (satelliteOnlyMode) 0.20f else 0.28f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = CyanZephyr.copy(alpha = if (satelliteOnlyMode) 0.20f else 0.28f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
            )

            // Dynamic radar weather cells based on selected layer mode & timeline index
            if (!satelliteOnlyMode) {
                val timeShift = timelineIndex * 0.14f
                when (layerMode) {
                    RadarLayerMode.RAIN -> {
                        if (precipProb >= 15 || precipMm >= 0.05) {
                            val intensityFactor = (precipProb / 100f).coerceIn(0.25f, 1f)
                            val centralRainRadius = maxRadius * (0.32f + 0.24f * intensityFactor) * cellPulse
                            val coreIntensityColor = when {
                                precipMm >= 4.0 || precipProb >= 80 -> Color(0xFFF59E0B)
                                precipMm >= 1.2 || precipProb >= 55 -> Color(0xFF10B981)
                                else -> CyanZephyr
                            }
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        coreIntensityColor.copy(alpha = 0.54f * intensityFactor),
                                        CyanZephyr.copy(alpha = 0.34f * intensityFactor),
                                        EmeraldBreeze.copy(alpha = 0.16f * intensityFactor),
                                        Color.Transparent
                                    ),
                                    center = Offset(
                                        center.x + cos(timeShift) * maxRadius * 0.06f,
                                        center.y + sin(timeShift) * maxRadius * 0.06f
                                    ),
                                    radius = centralRainRadius
                                ),
                                center = center,
                                radius = centralRainRadius
                            )

                            val cellCount = when {
                                precipProb >= 70 -> 7
                                precipProb >= 40 -> 5
                                else -> 3
                            }
                            for (c in 0 until cellCount) {
                                val angle = (c * 1.15f + timeShift) % (2 * PI.toFloat())
                                val dist = maxRadius * (0.16f + ((c * 19 + timelineIndex * 7) % 48) / 100f)
                                val cx = center.x + cos(angle) * dist
                                val cy = center.y + sin(angle) * dist
                                val blobRadius = maxRadius * (0.15f + (c % 3) * 0.06f) * cellPulse
                                val coreColor = when {
                                    precipProb >= 80 && c % 3 == 0 -> Color(0xFFEF4444)
                                    precipProb >= 60 && c % 2 == 0 -> Color(0xFFF59E0B)
                                    precipProb >= 35 -> CyanZephyr
                                    else -> EmeraldBreeze
                                }
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            coreColor.copy(alpha = 0.50f * intensityFactor),
                                            CyanZephyr.copy(alpha = 0.22f * intensityFactor),
                                            Color.Transparent
                                        ),
                                        center = Offset(cx, cy),
                                        radius = blobRadius
                                    ),
                                    center = Offset(cx, cy),
                                    radius = blobRadius
                                )
                            }
                        }
                    }

                    RadarLayerMode.WIND -> {
                        val windAngleRad = Math.toRadians((bundle.current.windDirectionDeg - 90).toDouble())
                        for (s in -3..3) {
                            val perpX = (-sin(windAngleRad) * s * maxRadius * 0.22f).toFloat()
                            val perpY = (cos(windAngleRad) * s * maxRadius * 0.22f).toFloat()
                            val flowOffset = ((sweepAngle / 360f + abs(s) * 0.15f) % 1f) * 2f - 1f
                            val startX = center.x + perpX - (cos(windAngleRad) * maxRadius * 0.55f).toFloat()
                            val startY = center.y + perpY - (sin(windAngleRad) * maxRadius * 0.55f).toFloat()
                            val endX = center.x + perpX + (cos(windAngleRad) * maxRadius * 0.55f).toFloat()
                            val endY = center.y + perpY + (sin(windAngleRad) * maxRadius * 0.55f).toFloat()
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        EmeraldBreeze.copy(alpha = 0.55f),
                                        CyanZephyr.copy(alpha = 0.70f),
                                        Color.Transparent
                                    ),
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY)
                                ),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = (2.5f + (windSpeed / 18.0).toFloat()).dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            val particleX = (startX + (endX - startX) * ((flowOffset + 1f) / 2f))
                            val particleY = (startY + (endY - startY) * ((flowOffset + 1f) / 2f))
                            drawCircle(
                                color = Color.White,
                                radius = 3.5.dp.toPx(),
                                center = Offset(particleX, particleY)
                            )
                        }
                    }

                    RadarLayerMode.CLOUDS -> {
                        if (cloudCover >= 8) {
                            val cloudDensity = (cloudCover / 18).coerceIn(1, 6)
                            val cloudAlphaScale = (cloudCover / 100f).coerceIn(0.25f, 0.90f)
                            for (c in 0 until cloudDensity) {
                                val angle = (c * 1.3f + timeShift * 0.8f)
                                val dist = maxRadius * (0.16f + (c % 3) * 0.22f)
                                val cx = center.x + cos(angle) * dist
                                val cy = center.y + sin(angle) * dist
                                val r = maxRadius * (0.22f + (c % 2) * 0.08f) * cellPulse
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFE2E8F0).copy(alpha = 0.46f * cloudAlphaScale),
                                            Color(0xFF94A3B8).copy(alpha = 0.20f * cloudAlphaScale),
                                            Color.Transparent
                                        ),
                                        center = Offset(cx, cy),
                                        radius = r
                                    ),
                                    center = Offset(cx, cy),
                                    radius = r
                                )
                            }
                        }
                    }

                    RadarLayerMode.TEMP -> {
                        val thermalColor = when {
                            tempC >= 26.0 -> CoralWarmth
                            tempC >= 18.0 -> AmberSol
                            else -> CyanZephyr
                        }
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    thermalColor.copy(alpha = 0.45f),
                                    AmberSol.copy(alpha = 0.20f),
                                    CyanZephyr.copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                center = Offset(
                                    center.x + sin(timeShift) * maxRadius * 0.10f,
                                    center.y + cos(timeShift) * maxRadius * 0.10f
                                ),
                                radius = maxRadius * 0.80f * cellPulse
                            ),
                            center = center,
                            radius = maxRadius * 0.80f * cellPulse
                        )
                    }
                }

                // Rotating Doppler sweep beam
                rotate(degrees = sweepAngle, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.82f to Color.Transparent,
                            0.95f to layerMode.accentColor.copy(alpha = 0.18f),
                            1.0f to layerMode.accentColor.copy(alpha = 0.48f),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = true,
                        topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                        size = Size(maxRadius * 2f, maxRadius * 2f)
                    )
                    drawLine(
                        color = layerMode.accentColor.copy(alpha = 0.88f),
                        start = center,
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Center station beacon (Active GPS / Town pin at exact center)
            drawCircle(
                color = layerMode.accentColor.copy(alpha = 0.35f * cellPulse),
                radius = 14.dp.toPx() * cellPulse,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = center
            )
            drawCircle(
                color = layerMode.accentColor,
                radius = 3.5.dp.toPx(),
                center = center
            )
        }
        }

        // Cardinal compass labels around the circular radar perimeter
        Text(
            text = if (language == AppLanguage.GREEK) "Β" else "N",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = CoralWarmth,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
        )
        Text(
            text = if (language == AppLanguage.GREEK) "Ν" else "S",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.78f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        )
        Text(
            text = if (language == AppLanguage.GREEK) "Δ" else "W",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.78f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
        )
        Text(
            text = if (language == AppLanguage.GREEK) "Α" else "E",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.78f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )
    }
}

@Composable
fun InteractiveHourlyCurveChart(
    hourly: List<HourlyForecastItem>,
    mode: HourlyChartMode,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember(hourly) { hourly.take(24) }
    if (items.isEmpty()) return

    val values = remember(items, mode) {
        items.map { item ->
            when (mode) {
                HourlyChartMode.TEMPERATURE -> item.tempC.toFloat()
                HourlyChartMode.PRECIPITATION -> item.precipProbability.toFloat()
                HourlyChartMode.WIND_UV -> item.windSpeedKmh.toFloat()
            }
        }
    }
    val secondaryValues = remember(items, mode) {
        items.map { item ->
            when (mode) {
                HourlyChartMode.TEMPERATURE -> item.feelsLikeC.toFloat()
                HourlyChartMode.PRECIPITATION -> (item.precipMm * 10f).toFloat()
                HourlyChartMode.WIND_UV -> (item.uvIndex * 4f).toFloat()
            }
        }
    }

    val minVal = remember(values) { (values.minOrNull() ?: 0f) - 2f }
    val maxVal = remember(values) { (values.maxOrNull() ?: 30f) + 2f }
    val span = remember(minVal, maxVal) { (maxVal - minVal).coerceAtLeast(4f) }

    val lineColor = when (mode) {
        HourlyChartMode.TEMPERATURE -> AmberSol
        HourlyChartMode.PRECIPITATION -> CyanZephyr
        HourlyChartMode.WIND_UV -> EmeraldBreeze
    }

    val gridDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) }
    val secDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) }
    val activeDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 6f)) }
    val secPath = remember { Path() }
    val curvePath = remember { Path() }
    val fillPath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(155.dp)
            .pointerInput(items.size) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    val idx = (fraction * (items.size - 1)).roundToInt().coerceIn(0, items.size - 1)
                    onSelectIndex(idx)
                }
            }
            .pointerInput(items.size) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val fraction = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    val idx = (fraction * (items.size - 1)).roundToInt().coerceIn(0, items.size - 1)
                    onSelectIndex(idx)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val padTop = 22f
        val padBottom = 24f
        val usableH = (h - padTop - padBottom).coerceAtLeast(10f)
        val stepX = if (items.size > 1) w / (items.size - 1) else w

        for (g in 0..2) {
            val gy = padTop + usableH * (g / 2f)
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, gy),
                end = Offset(w, gy),
                strokeWidth = 1.dp.toPx(),
                pathEffect = gridDashEffect
            )
        }

        if (mode != HourlyChartMode.PRECIPITATION && secondaryValues.size == items.size) {
            secPath.reset()
            secondaryValues.forEachIndexed { idx, v ->
                val x = idx * stepX
                val normalized = ((v - minVal) / span).coerceIn(0f, 1f)
                val y = padTop + usableH * (1f - normalized)
                if (idx == 0) secPath.moveTo(x, y) else secPath.lineTo(x, y)
            }
            drawPath(
                path = secPath,
                color = Color.White.copy(alpha = 0.30f),
                style = Stroke(
                    width = 1.8.dp.toPx(),
                    pathEffect = secDashEffect
                )
            )
        }

        curvePath.reset()
        fillPath.reset()
        var prevX = 0f
        var prevY = 0f
        values.forEachIndexed { idx, v ->
            val x = idx * stepX
            val normalized = ((v - minVal) / span).coerceIn(0f, 1f)
            val y = padTop + usableH * (1f - normalized)
            if (idx == 0) {
                curvePath.moveTo(x, y)
                fillPath.moveTo(x, h - padBottom)
                fillPath.lineTo(x, y)
            } else {
                val cx = (prevX + x) / 2f
                curvePath.cubicTo(cx, prevY, cx, y, x, y)
                fillPath.cubicTo(cx, prevY, cx, y, x, y)
            }
            prevX = x
            prevY = y
        }
        fillPath.lineTo(prevX, h - padBottom)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.35f),
                    lineColor.copy(alpha = 0.03f)
                ),
                startY = padTop,
                endY = h - padBottom
            )
        )

        drawPath(
            path = curvePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        val safeSelected = selectedIndex.coerceIn(0, values.lastIndex)
        val activeX = safeSelected * stepX
        val activeNorm = ((values[safeSelected] - minVal) / span).coerceIn(0f, 1f)
        val activeY = padTop + usableH * (1f - activeNorm)
        val activePoint = Offset(activeX, activeY)

        drawLine(
            color = Color.White.copy(alpha = 0.45f),
            start = Offset(activePoint.x, padTop * 0.4f),
            end = Offset(activePoint.x, h - padBottom),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = activeDashEffect
        )

        drawCircle(
            color = lineColor.copy(alpha = 0.32f),
            radius = 11.dp.toPx(),
            center = activePoint
        )
        drawCircle(
            color = Color.White,
            radius = 5.5.dp.toPx(),
            center = activePoint
        )
        drawCircle(
            color = lineColor,
            radius = 3.5.dp.toPx(),
            center = activePoint
        )
    }
}

@Composable
fun TenDayRangeBar(
    dayMinC: Double,
    dayMaxC: Double,
    overallMinC: Double,
    overallMaxC: Double,
    currentTempC: Double? = null,
    modifier: Modifier = Modifier
) {
    val totalSpan = (overallMaxC - overallMinC).coerceAtLeast(1.0)
    val startFraction = ((dayMinC - overallMinC) / totalSpan).toFloat().coerceIn(0f, 1f)
    val endFraction = ((dayMaxC - overallMinC) / totalSpan).toFloat().coerceIn(startFraction + 0.06f, 1f)

    Canvas(modifier = modifier.height(8.dp)) {
        val w = size.width
        val h = size.height
        val radius = CornerRadius(h / 2f, h / 2f)

        drawRoundRect(
            color = Color.White.copy(alpha = 0.12f),
            size = Size(w, h),
            cornerRadius = radius
        )

        val barStart = w * startFraction
        val barWidth = (w * (endFraction - startFraction)).coerceAtLeast(h)

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(CyanZephyr, EmeraldBreeze, AmberSol, CoralWarmth),
                startX = 0f,
                endX = w
            ),
            topLeft = Offset(barStart, 0f),
            size = Size(barWidth, h),
            cornerRadius = radius
        )

        if (currentTempC != null) {
            val dotFrac = ((currentTempC - overallMinC) / totalSpan).toFloat().coerceIn(0f, 1f)
            val dotX = (w * dotFrac).coerceIn(h / 2f, w - h / 2f)
            drawCircle(
                color = Color(0xFF07111E),
                radius = h * 0.72f,
                center = Offset(dotX, h / 2f)
            )
            drawCircle(
                color = Color.White,
                radius = h * 0.48f,
                center = Offset(dotX, h / 2f)
            )
        }
    }
}

@Composable
fun WindCompassCanvas(
    windDirectionDeg: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(136.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 8.dp.toPx()

            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            for (deg in 0 until 360 step 15) {
                val rad = Math.toRadians((deg - 90).toDouble())
                val isCardinal = deg % 90 == 0
                val innerR = if (isCardinal) radius - 10.dp.toPx() else radius - 5.dp.toPx()
                val sx = center.x + (innerR * cos(rad)).toFloat()
                val sy = center.y + (innerR * sin(rad)).toFloat()
                val ex = center.x + (radius * cos(rad)).toFloat()
                val ey = center.y + (radius * sin(rad)).toFloat()
                drawLine(
                    color = if (deg == 0) CoralWarmth else Color.White.copy(alpha = if (isCardinal) 0.65f else 0.25f),
                    start = Offset(sx, sy),
                    end = Offset(ex, ey),
                    strokeWidth = if (isCardinal) 2.5.dp.toPx() else 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            rotate(degrees = windDirectionDeg.toFloat(), pivot = center) {
                val tipY = center.y - radius * 0.72f
                val tailY = center.y + radius * 0.62f
                drawLine(
                    color = CyanZephyr,
                    start = Offset(center.x, tailY),
                    end = Offset(center.x, tipY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                val arrowHead = Path().apply {
                    moveTo(center.x, tipY - 4.dp.toPx())
                    lineTo(center.x - 6.dp.toPx(), tipY + 10.dp.toPx())
                    lineTo(center.x + 6.dp.toPx(), tipY + 10.dp.toPx())
                    close()
                }
                drawPath(path = arrowHead, color = CyanZephyr)
            }

            drawCircle(
                color = Color(0xFF0B192C),
                radius = 18.dp.toPx(),
                center = center
            )
            drawCircle(
                color = CyanZephyr.copy(alpha = 0.5f),
                radius = 18.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

@Composable
fun SolarHorizonCanvas(
    sunProgress: Float,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        val w = size.width
        val h = size.height
        val horizonY = h * 0.78f

        drawLine(
            color = Color.White.copy(alpha = 0.22f),
            start = Offset(12.dp.toPx(), horizonY),
            end = Offset(w - 12.dp.toPx(), horizonY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        )

        val leftX = 24.dp.toPx()
        val rightX = w - 24.dp.toPx()
        val arcWidth = (rightX - leftX).coerceAtLeast(10f)
        val peakY = h * 0.16f

        val fullArc = Path().apply {
            val steps = 40
            for (s in 0..steps) {
                val t = s / steps.toFloat()
                val x = leftX + arcWidth * t
                val y = horizonY - sin(t * PI).toFloat() * (horizonY - peakY)
                if (s == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            path = fullArc,
            color = Color.White.copy(alpha = 0.16f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        val clamped = sunProgress.coerceIn(0.02f, 0.98f)
        val activeArc = Path().apply {
            val steps = 25
            for (s in 0..steps) {
                val t = (s / steps.toFloat()) * clamped
                val x = leftX + arcWidth * t
                val y = horizonY - sin(t * PI).toFloat() * (horizonY - peakY)
                if (s == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            path = activeArc,
            brush = Brush.horizontalGradient(listOf(AmberSol, CoralWarmth)),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        val sunX = leftX + arcWidth * clamped
        val sunY = horizonY - sin(clamped * PI).toFloat() * (horizonY - peakY)
        val orbColor = if (isDay) AmberSol else Color(0xFFA78BFA)

        drawCircle(
            color = orbColor.copy(alpha = 0.28f),
            radius = 16.dp.toPx(),
            center = Offset(sunX, sunY)
        )
        drawCircle(
            color = orbColor,
            radius = 7.dp.toPx(),
            center = Offset(sunX, sunY)
        )
    }
}

@Composable
fun AirQualityArcCanvas(
    usAqi: Int,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(116.dp)) {
        val stroke = 10.dp.toPx()
        val arcSize = Size(size.width - stroke * 2, size.height - stroke * 2)
        val topLeft = Offset(stroke, stroke)

        drawArc(
            color = Color.White.copy(alpha = 0.12f),
            startAngle = 140f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        val normalized = (usAqi / 220f).coerceIn(0.05f, 1f)
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(EmeraldBreeze, AmberSol, CoralWarmth, badgeColor)
            ),
            startAngle = 140f,
            sweepAngle = 260f * normalized,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}
