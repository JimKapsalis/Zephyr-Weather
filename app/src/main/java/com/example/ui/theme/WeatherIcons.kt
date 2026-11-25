package com.example.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object WeatherIcons {
    /**
     * 1. SUN ONLY (Μόνο Ήλιος) — Multi-layered Radiant Golden Sun with 8 tapered rays and bright solar core.
     */
    val WbSunny: ImageVector by lazy {
        ImageVector.Builder(
            name = "WbSunny",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer warm amber sun rays
            path(
                stroke = SolidColor(Color(0xFFF59E0B)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 1.8f); lineTo(12f, 4.2f)
                moveTo(12f, 19.8f); lineTo(12f, 22.2f)
                moveTo(1.8f, 12f); lineTo(4.2f, 12f)
                moveTo(19.8f, 12f); lineTo(22.2f, 12f)
                moveTo(4.78f, 4.78f); lineTo(6.48f, 6.48f)
                moveTo(17.52f, 17.52f); lineTo(19.22f, 19.22f)
                moveTo(19.22f, 4.78f); lineTo(17.52f, 6.48f)
                moveTo(6.48f, 17.52f); lineTo(4.78f, 19.22f)
            }
            // Golden solar body
            path(fill = SolidColor(Color(0xFFFBBF24))) {
                moveTo(12f, 6.4f)
                curveTo(8.91f, 6.4f, 6.4f, 8.91f, 6.4f, 12f)
                curveTo(6.4f, 15.09f, 8.91f, 17.6f, 12f, 17.6f)
                curveTo(15.09f, 17.6f, 17.6f, 15.09f, 17.6f, 12f)
                curveTo(17.6f, 8.91f, 15.09f, 6.4f, 12f, 6.4f)
                close()
            }
            // Luminous inner highlight core
            path(fill = SolidColor(Color(0xFFFEF08A))) {
                moveTo(10.8f, 8.2f)
                curveTo(9.1f, 8.2f, 7.8f, 9.5f, 7.8f, 11.2f)
                curveTo(7.8f, 12.9f, 9.1f, 14.2f, 10.8f, 14.2f)
                curveTo(12.5f, 14.2f, 13.8f, 12.9f, 13.8f, 11.2f)
                curveTo(13.8f, 9.5f, 12.5f, 8.2f, 10.8f, 8.2f)
                close()
            }
        }.build()
    }

    /**
     * 5. MOON & STARS (Φεγγάρι Νύχτας) — Sculpted Golden-Silver Crescent Moon + Twinkling Stars for night hours.
     */
    val NightsStay: ImageVector by lazy {
        ImageVector.Builder(
            name = "NightsStay",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer warm golden crescent body
            path(fill = SolidColor(Color(0xFFFACC15))) {
                moveTo(12.6f, 3.0f)
                curveTo(8.0f, 3.6f, 4.5f, 7.5f, 4.5f, 12.2f)
                curveTo(4.5f, 17.3f, 8.7f, 21.5f, 13.8f, 21.5f)
                curveTo(17.3f, 21.5f, 20.3f, 19.6f, 21.8f, 16.7f)
                curveTo(21.0f, 17.0f, 20.1f, 17.1f, 19.2f, 17.1f)
                curveTo(14.5f, 17.1f, 10.7f, 13.3f, 10.7f, 8.6f)
                curveTo(10.7f, 6.5f, 11.4f, 4.6f, 12.6f, 3.0f)
                close()
            }
            // Inner pale lemon-silver lunar rim highlight
            path(fill = SolidColor(Color(0xFFFEF9C3))) {
                moveTo(10.8f, 5.2f)
                curveTo(7.9f, 6.4f, 6.0f, 9.1f, 6.0f, 12.2f)
                curveTo(6.0f, 16.3f, 9.3f, 19.6f, 13.4f, 19.6f)
                curveTo(15.4f, 19.6f, 17.2f, 18.8f, 18.5f, 17.5f)
                curveTo(14.2f, 17.0f, 10.5f, 13.4f, 10.5f, 8.8f)
                curveTo(10.5f, 7.5f, 10.6f, 6.3f, 10.8f, 5.2f)
                close()
            }
            // Primary 4-point star (top-right)
            path(fill = SolidColor(Color.White)) {
                moveTo(18.0f, 2.5f)
                lineTo(18.65f, 4.35f)
                lineTo(20.5f, 5.0f)
                lineTo(18.65f, 5.65f)
                lineTo(18.0f, 7.5f)
                lineTo(17.35f, 5.65f)
                lineTo(15.5f, 5.0f)
                lineTo(17.35f, 4.35f)
                close()
            }
            // Secondary cyan-white star (middle-right)
            path(fill = SolidColor(Color(0xFFBAE6FD))) {
                moveTo(14.8f, 8.4f)
                lineTo(15.25f, 9.55f)
                lineTo(16.4f, 10.0f)
                lineTo(15.25f, 10.45f)
                lineTo(14.8f, 11.6f)
                lineTo(14.35f, 10.45f)
                lineTo(13.2f, 10.0f)
                lineTo(14.35f, 9.55f)
                close()
            }
        }.build()
    }

    /**
     * 2. SUN WITH CLOUDS (Ήλιος με Σύννεφα) — Radiant 3D Golden Sun with Outer Corona + Sculpted 3-Layer Volumetric Cloud.
     */
    val PartlySunny: ImageVector by lazy {
        ImageVector.Builder(
            name = "PartlySunny",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer warm solar corona glow
            path(fill = SolidColor(Color(0xFFF59E0B).copy(alpha = 0.28f))) {
                moveTo(8.4f, 2.2f)
                curveTo(5.09f, 2.2f, 2.4f, 4.89f, 2.4f, 8.2f)
                curveTo(2.4f, 11.51f, 5.09f, 14.2f, 8.4f, 14.2f)
                curveTo(11.71f, 14.2f, 14.4f, 11.51f, 14.4f, 8.2f)
                curveTo(14.4f, 4.89f, 11.71f, 2.2f, 8.4f, 2.2f)
                close()
            }
            // Sun rays
            path(
                stroke = SolidColor(Color(0xFFF59E0B)),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(8.4f, 1.3f); lineTo(8.4f, 3.0f)
                moveTo(1.3f, 8.2f); lineTo(3.0f, 8.2f)
                moveTo(3.35f, 3.25f); lineTo(4.65f, 4.55f)
                moveTo(13.45f, 3.25f); lineTo(12.15f, 4.55f)
                moveTo(3.35f, 13.15f); lineTo(4.65f, 11.85f)
            }
            // Golden Sun outer sphere
            path(fill = SolidColor(Color(0xFFF59E0B))) {
                moveTo(8.4f, 4.1f)
                curveTo(6.14f, 4.1f, 4.3f, 5.94f, 4.3f, 8.2f)
                curveTo(4.3f, 10.46f, 6.14f, 12.3f, 8.4f, 12.3f)
                curveTo(10.66f, 12.3f, 12.5f, 10.46f, 12.5f, 8.2f)
                curveTo(12.5f, 5.94f, 10.66f, 4.1f, 8.4f, 4.1f)
                close()
            }
            // Bright yellow-white sun core highlight
            path(fill = SolidColor(Color(0xFFFEF08A))) {
                moveTo(8.0f, 5.1f)
                curveTo(6.56f, 5.1f, 5.4f, 6.26f, 5.4f, 7.7f)
                curveTo(5.4f, 9.14f, 6.56f, 10.3f, 8.0f, 10.3f)
                curveTo(9.44f, 10.3f, 10.6f, 9.14f, 10.6f, 7.7f)
                curveTo(10.6f, 6.26f, 9.44f, 5.1f, 8.0f, 5.1f)
                close()
            }
            // Deep atmospheric cloud underside shadow
            path(fill = SolidColor(Color(0xFF94A3B8))) {
                moveTo(19.5f, 13.8f)
                curveTo(18.9f, 10.8f, 16.4f, 8.7f, 13.3f, 8.7f)
                curveTo(10.8f, 8.7f, 8.6f, 10.1f, 7.5f, 12.2f)
                curveTo(5.1f, 12.5f, 3.3f, 14.5f, 3.3f, 17.0f)
                curveTo(3.3f, 19.8f, 5.5f, 22.0f, 8.3f, 22.0f)
                horizontalLineTo(19.2f)
                curveTo(21.6f, 22.0f, 23.5f, 20.1f, 23.5f, 17.7f)
                curveTo(23.5f, 15.5f, 21.7f, 13.9f, 19.5f, 13.8f)
                close()
            }
            // Soft silver-blue mid cloud volume
            path(fill = SolidColor(Color(0xFFE2E8F0))) {
                moveTo(19.3f, 13.3f)
                curveTo(18.7f, 10.4f, 16.3f, 8.4f, 13.3f, 8.4f)
                curveTo(10.8f, 8.4f, 8.7f, 9.8f, 7.6f, 11.9f)
                curveTo(5.2f, 12.2f, 3.5f, 14.1f, 3.5f, 16.6f)
                curveTo(3.5f, 19.2f, 5.6f, 21.2f, 8.3f, 21.2f)
                horizontalLineTo(19.0f)
                curveTo(21.4f, 21.2f, 23.2f, 19.4f, 23.2f, 17.1f)
                curveTo(23.2f, 14.9f, 21.5f, 13.4f, 19.3f, 13.3f)
                close()
            }
            // Pure snow-white sunlit cloud top dome
            path(fill = SolidColor(Color.White)) {
                moveTo(19.0f, 12.8f)
                curveTo(18.4f, 10.0f, 16.1f, 8.1f, 13.3f, 8.1f)
                curveTo(10.9f, 8.1f, 8.8f, 9.4f, 7.8f, 11.4f)
                curveTo(5.6f, 11.7f, 3.9f, 13.5f, 3.9f, 15.9f)
                curveTo(3.9f, 18.3f, 5.8f, 20.1f, 8.3f, 20.1f)
                horizontalLineTo(18.8f)
                curveTo(21.0f, 20.1f, 22.8f, 18.4f, 22.8f, 16.3f)
                curveTo(22.8f, 14.3f, 21.1f, 12.9f, 19.0f, 12.8f)
                close()
            }
        }.build()
    }

    /**
     * 2b. FOGGY & MIST (Ομίχλη & Πάχνη) — Soft Diffused Cloud Dome + Layered Horizontal Mist Ribbons.
     */
    val Foggy: ImageVector by lazy {
        ImageVector.Builder(
            name = "Foggy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Soft silver-blue upper cloud shadow
            path(fill = SolidColor(Color(0xFF94A3B8))) {
                moveTo(18.6f, 8.4f)
                curveTo(17.9f, 5.5f, 15.3f, 3.4f, 12.1f, 3.4f)
                curveTo(9.5f, 3.4f, 7.2f, 4.8f, 6.1f, 7.0f)
                curveTo(3.8f, 7.3f, 2.1f, 9.2f, 2.1f, 11.5f)
                curveTo(2.1f, 13.0f, 3.3f, 14.2f, 4.9f, 14.2f)
                horizontalLineTo(18.8f)
                curveTo(20.6f, 14.2f, 22.0f, 12.8f, 22.0f, 11.1f)
                curveTo(22.0f, 9.5f, 20.5f, 8.5f, 18.6f, 8.4f)
                close()
            }
            // Crisp misty-white cloud top
            path(fill = SolidColor(Color(0xFFE2E8F0))) {
                moveTo(18.4f, 7.8f)
                curveTo(17.7f, 5.0f, 15.2f, 3.0f, 12.1f, 3.0f)
                curveTo(9.6f, 3.0f, 7.4f, 4.3f, 6.3f, 6.4f)
                curveTo(4.1f, 6.7f, 2.5f, 8.5f, 2.5f, 10.7f)
                curveTo(2.5f, 12.1f, 3.6f, 13.2f, 5.1f, 13.2f)
                horizontalLineTo(18.5f)
                curveTo(20.2f, 13.2f, 21.5f, 11.9f, 21.5f, 10.3f)
                curveTo(21.5f, 8.8f, 20.1f, 7.9f, 18.4f, 7.8f)
                close()
            }
            // Three layered horizontal fog/mist ribbons below the cloud
            path(
                stroke = SolidColor(Color(0xFFBAE6FD)),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3.0f, 16.2f)
                curveTo(6.0f, 15.4f, 9.5f, 17.0f, 13.0f, 16.2f)
                curveTo(16.2f, 15.5f, 18.8f, 16.8f, 21.0f, 16.2f)
            }
            path(
                stroke = SolidColor(Color(0xFF7DD3FC)),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4.5f, 19.0f)
                curveTo(8.0f, 18.2f, 12.0f, 19.8f, 16.0f, 19.0f)
                curveTo(18.0f, 18.6f, 19.5f, 19.3f, 20.5f, 19.0f)
            }
            path(
                stroke = SolidColor(Color(0xFF94A3B8)),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6.0f, 21.6f)
                horizontalLineTo(18.0f)
            }
        }.build()
    }

    val FilterDrama: ImageVector by lazy { PartlySunny }
    val Cloud: ImageVector by lazy { PartlySunny }

    /**
     * 3. CLOUDS WITH RAIN (Σύννεφα με Βροχή) — Sculpted Cloud + Glowing Cyan Raindrops.
     */
    val Grain: ImageVector by lazy {
        ImageVector.Builder(
            name = "CloudWithRain",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Cloud base
            path(fill = SolidColor(Color(0xFFCBD5E1))) {
                moveTo(18.8f, 8.8f)
                curveTo(18.1f, 5.7f, 15.3f, 3.4f, 12f, 3.4f)
                curveTo(9.3f, 3.4f, 7.0f, 4.9f, 5.9f, 7.2f)
                curveTo(3.4f, 7.5f, 1.5f, 9.6f, 1.5f, 12.2f)
                curveTo(1.5f, 15.0f, 3.7f, 17.2f, 6.5f, 17.2f)
                horizontalLineTo(18.5f)
                curveTo(21.0f, 17.2f, 23.0f, 15.2f, 23.0f, 12.7f)
                curveTo(23.0f, 10.4f, 21.1f, 8.9f, 18.8f, 8.8f)
                close()
            }
            // Crisp white upper cloud dome
            path(fill = SolidColor(Color.White)) {
                moveTo(18.6f, 8.2f)
                curveTo(17.9f, 5.2f, 15.2f, 3.0f, 12f, 3.0f)
                curveTo(9.4f, 3.0f, 7.1f, 4.5f, 6.0f, 6.7f)
                curveTo(3.6f, 7.0f, 1.8f, 9.0f, 1.8f, 11.5f)
                curveTo(1.8f, 14.1f, 3.9f, 16.1f, 6.5f, 16.1f)
                horizontalLineTo(18.3f)
                curveTo(20.7f, 16.1f, 22.6f, 14.3f, 22.6f, 12.0f)
                curveTo(22.6f, 9.8f, 20.8f, 8.3f, 18.6f, 8.2f)
                close()
            }
            // Slanted rounded raindrops
            path(
                stroke = SolidColor(Color(0xFF38BDF8)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7.8f, 18.5f); lineTo(6.3f, 22.2f)
                moveTo(12.5f, 18.5f); lineTo(11.0f, 22.2f)
                moveTo(17.2f, 18.5f); lineTo(15.7f, 22.2f)
            }
        }.build()
    }

    val WaterDrop: ImageVector by lazy {
        ImageVector.Builder(
            name = "WaterDrop",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF38BDF8))) {
                moveTo(12f, 2.5f)
                curveTo(12f, 2.5f, 5.5f, 9.8f, 5.5f, 14.5f)
                curveTo(5.5f, 18.09f, 8.41f, 21.0f, 12f, 21.0f)
                curveTo(15.59f, 21.0f, 18.5f, 18.09f, 18.5f, 14.5f)
                curveTo(18.5f, 9.8f, 12f, 2.5f, 12f, 2.5f)
                close()
            }
            path(fill = SolidColor(Color(0xFFBAE6FD))) {
                moveTo(9.5f, 12.5f)
                curveTo(8.7f, 12.5f, 8.0f, 13.5f, 8.0f, 14.8f)
                curveTo(8.0f, 16.1f, 8.7f, 17.0f, 9.5f, 17.0f)
                curveTo(10.1f, 17.0f, 10.5f, 16.2f, 10.5f, 14.8f)
                curveTo(10.5f, 13.5f, 10.1f, 12.5f, 9.5f, 12.5f)
                close()
            }
        }.build()
    }

    /**
     * 3b. SNOW CLOUDS & SNOWFALL (Σύννεφα Χιονιού & Χιονόπτωση) — Frosted Ice-Blue & Pure White Cloud + 3 Hexagonal Snowflakes.
     */
    val SnowCloud: ImageVector by lazy {
        ImageVector.Builder(
            name = "SnowCloud",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Frosted ice-blue cloud base shadow
            path(fill = SolidColor(Color(0xFFBAE6FD))) {
                moveTo(18.8f, 8.6f)
                curveTo(18.1f, 5.5f, 15.3f, 3.2f, 12f, 3.2f)
                curveTo(9.3f, 3.2f, 7.0f, 4.7f, 5.9f, 7.0f)
                curveTo(3.4f, 7.3f, 1.5f, 9.4f, 1.5f, 12.0f)
                curveTo(1.5f, 14.7f, 3.7f, 16.8f, 6.5f, 16.8f)
                horizontalLineTo(18.5f)
                curveTo(21.0f, 16.8f, 23.0f, 14.9f, 23.0f, 12.4f)
                curveTo(23.0f, 10.1f, 21.1f, 8.7f, 18.8f, 8.6f)
                close()
            }
            // Crisp snow-white upper cloud dome
            path(fill = SolidColor(Color.White)) {
                moveTo(18.6f, 8.0f)
                curveTo(17.9f, 5.0f, 15.2f, 2.8f, 12f, 2.8f)
                curveTo(9.4f, 2.8f, 7.1f, 4.3f, 6.0f, 6.5f)
                curveTo(3.6f, 6.8f, 1.8f, 8.8f, 1.8f, 11.3f)
                curveTo(1.8f, 13.8f, 3.9f, 15.7f, 6.5f, 15.7f)
                horizontalLineTo(18.3f)
                curveTo(20.7f, 15.7f, 22.6f, 14.0f, 22.6f, 11.7f)
                curveTo(22.6f, 9.6f, 20.8f, 8.1f, 18.6f, 8.0f)
                close()
            }
            // Three 6-point crystalline snowflakes below the snow cloud
            path(
                stroke = SolidColor(Color(0xFF7DD3FC)),
                strokeLineWidth = 1.65f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Left snowflake (center 6.8, 19.6)
                moveTo(6.8f, 17.7f); lineTo(6.8f, 21.5f)
                moveTo(5.15f, 18.65f); lineTo(8.45f, 20.55f)
                moveTo(8.45f, 18.65f); lineTo(5.15f, 20.55f)
                // Center larger snowflake (center 12.0, 20.6)
                moveTo(12.0f, 18.3f); lineTo(12.0f, 22.9f)
                moveTo(10.0f, 19.45f); lineTo(14.0f, 21.75f)
                moveTo(14.0f, 19.45f); lineTo(10.0f, 21.75f)
                // Right snowflake (center 17.2, 19.6)
                moveTo(17.2f, 17.7f); lineTo(17.2f, 21.5f)
                moveTo(15.55f, 18.65f); lineTo(18.85f, 20.55f)
                moveTo(18.85f, 18.65f); lineTo(15.55f, 20.55f)
            }
            // Bright white crystal cores
            path(fill = SolidColor(Color.White)) {
                moveTo(6.8f, 18.9f)
                curveTo(6.41f, 18.9f, 6.1f, 19.21f, 6.1f, 19.6f)
                curveTo(6.1f, 19.99f, 6.41f, 20.3f, 6.8f, 20.3f)
                curveTo(7.19f, 20.3f, 7.5f, 19.99f, 7.5f, 19.6f)
                curveTo(7.5f, 19.21f, 7.19f, 18.9f, 6.8f, 18.9f)
                close()
                moveTo(12.0f, 19.8f)
                curveTo(11.56f, 19.8f, 11.2f, 20.16f, 11.2f, 20.6f)
                curveTo(11.2f, 21.04f, 11.56f, 21.4f, 12.0f, 21.4f)
                curveTo(12.44f, 21.4f, 12.8f, 21.04f, 12.8f, 20.6f)
                curveTo(12.8f, 20.16f, 12.44f, 19.8f, 12.0f, 19.8f)
                close()
                moveTo(17.2f, 18.9f)
                curveTo(16.81f, 18.9f, 16.5f, 19.21f, 16.5f, 19.6f)
                curveTo(16.5f, 19.99f, 16.81f, 20.3f, 17.2f, 20.3f)
                curveTo(17.59f, 20.3f, 17.9f, 19.99f, 17.9f, 19.6f)
                curveTo(17.9f, 19.21f, 17.59f, 18.9f, 17.2f, 18.9f)
                close()
            }
        }.build()
    }

    /**
     * 3c. SNOW STORM & BLIZZARD (Χιονοθύελλα) — Winter Storm Cloud + Swirling Icy Gale Gusts + Heavy Snowflakes.
     */
    val SnowStorm: ImageVector by lazy {
        ImageVector.Builder(
            name = "SnowStorm",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Brooding winter slate-blue storm cloud base
            path(fill = SolidColor(Color(0xFF94A3B8))) {
                moveTo(18.6f, 7.8f)
                curveTo(17.9f, 4.8f, 15.2f, 2.5f, 12.0f, 2.5f)
                curveTo(9.4f, 2.5f, 7.2f, 3.9f, 6.1f, 6.0f)
                curveTo(3.6f, 6.3f, 1.7f, 8.3f, 1.7f, 10.9f)
                curveTo(1.7f, 13.5f, 3.9f, 15.6f, 6.6f, 15.6f)
                horizontalLineTo(18.3f)
                curveTo(20.7f, 15.6f, 22.6f, 13.8f, 22.6f, 11.4f)
                curveTo(22.6f, 9.3f, 20.8f, 7.9f, 18.6f, 7.8f)
                close()
            }
            // Frosted ice-white storm cloud crest
            path(fill = SolidColor(Color(0xFFF1F5F9))) {
                moveTo(18.4f, 7.2f)
                curveTo(17.7f, 4.3f, 15.1f, 2.1f, 12.0f, 2.1f)
                curveTo(9.5f, 2.1f, 7.3f, 3.5f, 6.2f, 5.5f)
                curveTo(3.8f, 5.8f, 2.0f, 7.7f, 2.0f, 10.2f)
                curveTo(2.0f, 12.7f, 4.1f, 14.6f, 6.7f, 14.6f)
                horizontalLineTo(18.1f)
                curveTo(20.4f, 14.6f, 22.2f, 12.9f, 22.2f, 10.7f)
                curveTo(22.2f, 8.7f, 20.5f, 7.3f, 18.4f, 7.2f)
                close()
            }
            // Swirling blizzard gale wind ribbons
            path(
                stroke = SolidColor(Color(0xFF38BDF8)),
                strokeLineWidth = 1.85f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(2.4f, 17.2f)
                horizontalLineTo(13.8f)
                curveTo(15.2f, 17.2f, 16.2f, 16.3f, 16.2f, 15.1f)
                curveTo(16.2f, 14.0f, 15.3f, 13.2f, 14.2f, 13.2f)
                moveTo(4.6f, 20.6f)
                horizontalLineTo(15.6f)
                curveTo(17.0f, 20.6f, 18.0f, 21.4f, 18.0f, 22.4f)
            }
            // Driven diagonal snow crystals in the gale
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.65f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Snowflake 1 (top-right gale)
                moveTo(19.6f, 15.8f); lineTo(19.6f, 19.4f)
                moveTo(18.0f, 16.7f); lineTo(21.2f, 18.5f)
                moveTo(21.2f, 16.7f); lineTo(18.0f, 18.5f)
                // Snowflake 2 (bottom-left gale)
                moveTo(8.2f, 18.4f); lineTo(8.2f, 22.0f)
                moveTo(6.6f, 19.3f); lineTo(9.8f, 21.1f)
                moveTo(9.8f, 19.3f); lineTo(6.6f, 21.1f)
            }
            // Blowing snow pellets
            path(fill = SolidColor(Color(0xFFE0F2FE))) {
                moveTo(3.6f, 19.0f)
                curveTo(3.05f, 19.0f, 2.6f, 19.45f, 2.6f, 20.0f)
                curveTo(2.6f, 20.55f, 3.05f, 21.0f, 3.6f, 21.0f)
                curveTo(4.15f, 21.0f, 4.6f, 20.55f, 4.6f, 20.0f)
                curveTo(4.6f, 19.45f, 4.15f, 19.0f, 3.6f, 19.0f)
                close()
                moveTo(21.0f, 20.4f)
                curveTo(20.45f, 20.4f, 20.0f, 20.85f, 20.0f, 21.4f)
                curveTo(20.0f, 21.95f, 20.45f, 22.4f, 21.0f, 22.4f)
                curveTo(21.55f, 22.4f, 22.0f, 21.95f, 22.0f, 21.4f)
                curveTo(22.0f, 20.85f, 21.55f, 20.4f, 21.0f, 20.4f)
                close()
            }
        }.build()
    }

    /**
     * 3d. CRYSTALLINE SNOWFLAKE (Νιφάδα Χιονιού) — 6-Axis Hexagonal Ice Crystal.
     */
    val Snowflake: ImageVector by lazy {
        ImageVector.Builder(
            name = "Snowflake",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF7DD3FC)),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2.5f); lineTo(12f, 21.5f)
                moveTo(3.8f, 7.25f); lineTo(20.2f, 16.75f)
                moveTo(20.2f, 7.25f); lineTo(3.8f, 16.75f)
                // Crystal branches
                moveTo(10f, 4.5f); lineTo(12f, 6.5f); lineTo(14f, 4.5f)
                moveTo(10f, 19.5f); lineTo(12f, 17.5f); lineTo(14f, 19.5f)
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 9.8f)
                curveTo(10.78f, 9.8f, 9.8f, 10.78f, 9.8f, 12f)
                curveTo(9.8f, 13.22f, 10.78f, 14.2f, 12f, 14.2f)
                curveTo(13.22f, 14.2f, 14.2f, 13.22f, 14.2f, 12f)
                curveTo(14.2f, 10.78f, 13.22f, 9.8f, 12f, 9.8f)
                close()
            }
        }.build()
    }

    val AcUnit: ImageVector by lazy { SnowCloud }

    /**
     * 4. THUNDERSTORM (Καταιγίδα) — Storm Cloud + Golden Lightning Bolt + Cyan Rain Streaks.
     */
    val Thunderstorm: ImageVector by lazy {
        ImageVector.Builder(
            name = "Thunderstorm",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Storm cloud base
            path(fill = SolidColor(Color(0xFFCBD5E1))) {
                moveTo(18.5f, 7.8f)
                curveTo(17.9f, 4.8f, 15.2f, 2.6f, 12f, 2.6f)
                curveTo(9.5f, 2.6f, 7.3f, 4.0f, 6.2f, 6.1f)
                curveTo(3.7f, 6.4f, 1.8f, 8.5f, 1.8f, 11.1f)
                curveTo(1.8f, 13.9f, 4.1f, 16.2f, 6.9f, 16.2f)
                horizontalLineTo(18.2f)
                curveTo(20.6f, 16.2f, 22.5f, 14.3f, 22.5f, 11.9f)
                curveTo(22.5f, 9.7f, 20.7f, 7.9f, 18.5f, 7.8f)
                close()
            }
            // Crisp white cloud top
            path(fill = SolidColor(Color.White)) {
                moveTo(18.3f, 7.2f)
                curveTo(17.7f, 4.3f, 15.1f, 2.2f, 12f, 2.2f)
                curveTo(9.5f, 2.2f, 7.4f, 3.6f, 6.3f, 5.6f)
                curveTo(3.9f, 5.9f, 2.1f, 7.9f, 2.1f, 10.4f)
                curveTo(2.1f, 13.0f, 4.2f, 15.1f, 6.8f, 15.1f)
                horizontalLineTo(18.0f)
                curveTo(20.3f, 15.1f, 22.1f, 13.3f, 22.1f, 11.1f)
                curveTo(22.1f, 9.0f, 20.4f, 7.3f, 18.3f, 7.2f)
                close()
            }
            // Left & right rain streaks
            path(
                stroke = SolidColor(Color(0xFF38BDF8)),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6.8f, 17.5f); lineTo(5.5f, 21.0f)
                moveTo(17.8f, 17.5f); lineTo(16.5f, 21.0f)
            }
            // Electric golden lightning bolt
            path(fill = SolidColor(Color(0xFFFDE047))) {
                moveTo(13.4f, 13.8f)
                lineTo(9.2f, 19.2f)
                horizontalLineTo(12.1f)
                lineTo(10.6f, 23.6f)
                lineTo(15.8f, 17.8f)
                horizontalLineTo(12.9f)
                lineTo(13.4f, 13.8f)
                close()
            }
        }.build()
    }

    val WbTwilight: ImageVector by lazy {
        ImageVector.Builder(
            name = "WbTwilight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFFFB923C))) {
                moveTo(2f, 18f)
                horizontalLineTo(22f)
                verticalLineTo(20f)
                horizontalLineTo(2f)
                close()
                moveTo(12f, 9f)
                curveTo(9.24f, 9f, 7f, 11.24f, 7f, 14f)
                verticalLineTo(16f)
                horizontalLineTo(17f)
                verticalLineTo(14f)
                curveTo(17f, 11.24f, 14.76f, 9f, 12f, 9f)
                close()
            }
        }.build()
    }

    val Air: ImageVector by lazy {
        ImageVector.Builder(
            name = "Air",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 8f)
                horizontalLineTo(16.5f)
                curveTo(18f, 8f, 19f, 7f, 19f, 5.5f)
                curveTo(19f, 4f, 18f, 3f, 16.5f, 3f)
                curveTo(15.2f, 3f, 14.2f, 3.8f, 14f, 5f)
                moveTo(3f, 12f)
                horizontalLineTo(19.5f)
                curveTo(21f, 12f, 22f, 13f, 22f, 14.5f)
                curveTo(22f, 16f, 21f, 17f, 19.5f, 17f)
                curveTo(18.2f, 17f, 17.2f, 16.2f, 17f, 15f)
                moveTo(3f, 16f)
                horizontalLineTo(12.5f)
                curveTo(14f, 16f, 15f, 17f, 15f, 18.5f)
                curveTo(15f, 20f, 14f, 21f, 12.5f, 21f)
            }
        }.build()
    }

    val Speed: ImageVector by lazy {
        ImageVector.Builder(
            name = "Speed",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4.93f, 19.07f)
                curveTo(1.02f, 15.16f, 1.02f, 8.84f, 4.93f, 4.93f)
                curveTo(8.84f, 1.02f, 15.16f, 1.02f, 19.07f, 4.93f)
                curveTo(22.98f, 8.84f, 22.98f, 15.16f, 19.07f, 19.07f)
                moveTo(12f, 13f)
                lineTo(16f, 8f)
            }
        }.build()
    }

    /**
     * Navigation Bar Tab 1: Forecast Emblem (Sun & Horizon Cloud)
     */
    val NavForecast: ImageVector by lazy {
        ImageVector.Builder(
            name = "NavForecast",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Sun rays top-left
                moveTo(9f, 2.5f); lineTo(9f, 4.2f)
                moveTo(2.5f, 9f); lineTo(4.2f, 9f)
                moveTo(4.4f, 4.4f); lineTo(5.6f, 5.6f)
                moveTo(13.6f, 4.4f); lineTo(12.4f, 5.6f)
                // Sun arc
                moveTo(5.8f, 10.5f)
                curveTo(5.3f, 9.6f, 5.2f, 8.4f, 5.7f, 7.3f)
                curveTo(6.5f, 5.5f, 8.6f, 4.7f, 10.4f, 5.5f)
                curveTo(11.4f, 5.9f, 12.1f, 6.8f, 12.4f, 7.8f)
                // Cloud silhouette
                moveTo(18.2f, 12.2f)
                curveTo(17.6f, 9.8f, 15.4f, 8.0f, 12.8f, 8.0f)
                curveTo(10.5f, 8.0f, 8.6f, 9.3f, 7.7f, 11.2f)
                curveTo(5.6f, 11.5f, 4.0f, 13.3f, 4.0f, 15.5f)
                curveTo(4.0f, 17.9f, 5.9f, 19.8f, 8.3f, 19.8f)
                horizontalLineTo(18.0f)
                curveTo(20.1f, 19.8f, 21.8f, 18.1f, 21.8f, 16.0f)
                curveTo(21.8f, 14.0f, 20.2f, 12.3f, 18.2f, 12.2f)
                close()
            }
        }.build()
    }

    /**
     * Navigation Bar Tab 2 (Active): Realistic 3D Doppler Weather Radar Console with Glowing Sweep,
     * Deep Ocean CRT Glass Dome, Concentric Range Rings, and Multi-Tone Precipitation Storm Cells.
     */
    val NavRadarActive: ImageVector by lazy {
        ImageVector.Builder(
            name = "NavRadarActive",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer metallic titanium bezel shadow
            path(fill = SolidColor(Color(0xFF0F2942))) {
                moveTo(22.0f, 12f)
                curveTo(22.0f, 17.52f, 17.52f, 22.0f, 12f, 22.0f)
                curveTo(6.48f, 22.0f, 2.0f, 17.52f, 2.0f, 12f)
                curveTo(2.0f, 6.48f, 6.48f, 2.0f, 12f, 2.0f)
                curveTo(17.52f, 2.0f, 22.0f, 6.48f, 22.0f, 12f)
                close()
            }
            // Deep Doppler radar screen base
            path(fill = SolidColor(Color(0xFF06162A))) {
                moveTo(20.6f, 12f)
                curveTo(20.6f, 16.75f, 16.75f, 20.6f, 12f, 20.6f)
                curveTo(7.25f, 20.6f, 3.4f, 16.75f, 3.4f, 12f)
                curveTo(3.4f, 7.25f, 7.25f, 3.4f, 12f, 3.4f)
                curveTo(16.75f, 3.4f, 20.6f, 7.25f, 20.6f, 12f)
                close()
            }
            // Wide phosphor emerald-cyan radar sweep trail (top-right sector)
            path(fill = SolidColor(Color(0xFF10B981).copy(alpha = 0.25f))) {
                moveTo(12f, 12f)
                lineTo(12f, 3.4f)
                curveTo(16.75f, 3.4f, 20.6f, 7.25f, 20.6f, 12f)
                close()
            }
            // Bright leading edge of sweep cone
            path(fill = SolidColor(Color(0xFF34D399).copy(alpha = 0.48f))) {
                moveTo(12f, 12f)
                lineTo(15.6f, 4.2f)
                curveTo(17.8f, 5.2f, 19.4f, 7.1f, 20.1f, 9.4f)
                close()
            }
            // Concentric radar range rings + cardinal crosshairs
            path(
                stroke = SolidColor(Color(0xFF38BDF8).copy(alpha = 0.72f)),
                strokeLineWidth = 1.15f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Middle range ring
                moveTo(17.4f, 12f)
                curveTo(17.4f, 14.98f, 14.98f, 17.4f, 12f, 17.4f)
                curveTo(9.02f, 17.4f, 6.6f, 14.98f, 6.6f, 12f)
                curveTo(6.6f, 9.02f, 9.02f, 6.6f, 12f, 6.6f)
                curveTo(14.98f, 6.6f, 17.4f, 9.02f, 17.4f, 12f)
                close()
                // Inner range ring
                moveTo(14.6f, 12f)
                curveTo(14.6f, 13.44f, 13.44f, 14.6f, 12f, 14.6f)
                curveTo(10.56f, 14.6f, 9.4f, 13.44f, 9.4f, 12f)
                curveTo(9.4f, 10.56f, 10.56f, 9.4f, 12f, 9.4f)
                curveTo(13.44f, 9.4f, 14.6f, 10.56f, 14.6f, 12f)
                close()
                // Crosshair grid lines
                moveTo(12f, 3.6f); lineTo(12f, 20.4f)
                moveTo(3.6f, 12f); lineTo(20.4f, 12f)
            }
            // Outer illuminated glass rim
            path(
                stroke = SolidColor(Color(0xFF7DD3FC)),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(20.8f, 12f)
                curveTo(20.8f, 16.86f, 16.86f, 20.8f, 12f, 20.8f)
                curveTo(7.14f, 20.8f, 3.2f, 16.86f, 3.2f, 12f)
                curveTo(3.2f, 7.14f, 7.14f, 3.2f, 12f, 3.2f)
                curveTo(16.86f, 3.2f, 20.8f, 7.14f, 20.8f, 12f)
                close()
            }
            // Multi-layered Doppler storm cell echo (outer emerald + amber core + red eye)
            path(fill = SolidColor(Color(0xFF10B981).copy(alpha = 0.65f))) {
                moveTo(16.0f, 6.2f)
                curveTo(14.6f, 6.2f, 13.5f, 7.2f, 13.5f, 8.5f)
                curveTo(13.5f, 9.8f, 14.6f, 10.8f, 16.0f, 10.8f)
                curveTo(17.4f, 10.8f, 18.5f, 9.8f, 18.5f, 8.5f)
                curveTo(18.5f, 7.2f, 17.4f, 6.2f, 16.0f, 6.2f)
                close()
            }
            path(fill = SolidColor(Color(0xFFFBBF24))) {
                moveTo(16.1f, 7.1f)
                curveTo(15.27f, 7.1f, 14.6f, 7.77f, 14.6f, 8.6f)
                curveTo(14.6f, 9.43f, 15.27f, 10.1f, 16.1f, 10.1f)
                curveTo(16.93f, 10.1f, 17.6f, 9.43f, 17.6f, 8.6f)
                curveTo(17.6f, 7.77f, 16.93f, 7.1f, 16.1f, 7.1f)
                close()
            }
            path(fill = SolidColor(Color(0xFFEF4444))) {
                moveTo(16.1f, 7.8f)
                curveTo(15.66f, 7.8f, 15.3f, 8.16f, 15.3f, 8.6f)
                curveTo(15.3f, 9.04f, 15.66f, 9.4f, 16.1f, 9.4f)
                curveTo(16.54f, 9.4f, 16.9f, 9.04f, 16.9f, 8.6f)
                curveTo(16.9f, 8.16f, 16.54f, 7.8f, 16.1f, 7.8f)
                close()
            }
            // Secondary rain cell in lower-left quadrant
            path(fill = SolidColor(Color(0xFF38BDF8).copy(alpha = 0.78f))) {
                moveTo(8.2f, 14.4f)
                curveTo(7.43f, 14.4f, 6.8f, 15.03f, 6.8f, 15.8f)
                curveTo(6.8f, 16.57f, 7.43f, 17.2f, 8.2f, 17.2f)
                curveTo(8.97f, 17.2f, 9.6f, 16.57f, 9.6f, 15.8f)
                curveTo(9.6f, 15.03f, 8.97f, 14.4f, 8.2f, 14.4f)
                close()
            }
            // Glowing emerald sweep laser beam
            path(
                stroke = SolidColor(Color(0xFF6EE7B7)),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 12f)
                lineTo(18.6f, 6.2f)
            }
            // Center radar transmitter hub
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 10.3f)
                curveTo(11.06f, 10.3f, 10.3f, 11.06f, 10.3f, 12f)
                curveTo(10.3f, 12.94f, 11.06f, 13.7f, 12f, 13.7f)
                curveTo(12.94f, 13.7f, 13.7f, 12.94f, 13.7f, 12f)
                curveTo(13.7f, 11.06f, 12.94f, 10.3f, 12f, 10.3f)
                close()
            }
        }.build()
    }

    /**
     * Navigation Bar Tab 2 (Inactive): Realistic Multi-Tone Doppler Radar Console
     */
    val CompassCalibration: ImageVector by lazy { NavRadarActive }

    /**
     * Navigation Bar Tab 3 (Active): Realistic 3D Earth Globe with Deep Blue Ocean,
     * Sculpted Emerald Continents, Atmospheric Rim Highlight, and Golden 3D Location Pin.
     */
    val NavCitiesActive: ImageVector by lazy {
        ImageVector.Builder(
            name = "NavCitiesActive",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Deep ocean sphere base
            path(fill = SolidColor(Color(0xFF0284C7))) {
                moveTo(20.6f, 12.2f)
                curveTo(20.6f, 17.06f, 16.66f, 21.0f, 11.8f, 21.0f)
                curveTo(6.94f, 21.0f, 3.0f, 17.06f, 3.0f, 12.2f)
                curveTo(3.0f, 7.34f, 6.94f, 3.4f, 11.8f, 3.4f)
                curveTo(16.66f, 3.4f, 20.6f, 7.34f, 20.6f, 12.2f)
                close()
            }
            // sunlit upper-left ocean hemisphere glow
            path(fill = SolidColor(Color(0xFF38BDF8).copy(alpha = 0.55f))) {
                moveTo(11.8f, 3.4f)
                curveTo(7.3f, 3.4f, 3.5f, 6.8f, 3.1f, 11.2f)
                curveTo(5.6f, 13.0f, 9.5f, 13.2f, 13.2f, 11.0f)
                curveTo(15.6f, 9.6f, 17.0f, 7.0f, 16.8f, 4.9f)
                curveTo(15.3f, 3.9f, 13.6f, 3.4f, 11.8f, 3.4f)
                close()
            }
            // Sculpted emerald continent landmass 1 (Europe / Africa silhouette)
            path(fill = SolidColor(Color(0xFF34D399))) {
                moveTo(11.2f, 5.4f)
                curveTo(12.8f, 5.2f, 14.5f, 6.0f, 15.2f, 7.4f)
                curveTo(15.8f, 8.6f, 15.0f, 10.2f, 16.2f, 11.4f)
                curveTo(17.1f, 12.3f, 16.8f, 14.8f, 15.2f, 16.8f)
                curveTo(14.1f, 18.1f, 12.6f, 17.8f, 12.1f, 16.2f)
                curveTo(11.6f, 14.6f, 12.4f, 13.0f, 11.0f, 11.8f)
                curveTo(9.8f, 10.8f, 9.6f, 9.2f, 10.3f, 7.6f)
                curveTo(10.6f, 6.8f, 10.5f, 5.5f, 11.2f, 5.4f)
                close()
            }
            // Sculpted emerald continent landmass 2 (Americas western landmass)
            path(fill = SolidColor(Color(0xFF10B981))) {
                moveTo(5.2f, 7.6f)
                curveTo(6.6f, 7.2f, 7.8f, 8.2f, 8.1f, 9.6f)
                curveTo(8.4f, 11.0f, 7.4f, 12.2f, 8.2f, 13.6f)
                curveTo(8.9f, 14.8f, 8.7f, 16.9f, 7.5f, 18.2f)
                curveTo(6.4f, 17.4f, 5.2f, 15.8f, 4.6f, 13.8f)
                curveTo(4.0f, 11.6f, 4.3f, 9.2f, 5.2f, 7.6f)
                close()
            }
            // Subtle latitude/longitude atmospheric grid & glowing outer atmosphere ring
            path(
                stroke = SolidColor(Color(0xFFBAE6FD).copy(alpha = 0.65f)),
                strokeLineWidth = 1.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(20.6f, 12.2f)
                curveTo(20.6f, 17.06f, 16.66f, 21.0f, 11.8f, 21.0f)
                curveTo(6.94f, 21.0f, 3.0f, 17.06f, 3.0f, 12.2f)
                curveTo(3.0f, 7.34f, 6.94f, 3.4f, 11.8f, 3.4f)
                curveTo(16.66f, 3.4f, 20.6f, 7.34f, 20.6f, 12.2f)
                close()
                // Equator arc
                moveTo(3.2f, 12.5f)
                curveTo(6.0f, 14.0f, 17.6f, 14.0f, 20.4f, 12.5f)
            }
            // 3D Golden-Amber Location Pin Badge (top-right) with drop shadow
            path(fill = SolidColor(Color(0xFFD97706))) {
                moveTo(18.5f, 1.8f)
                curveTo(16.3f, 1.8f, 14.5f, 3.6f, 14.5f, 5.8f)
                curveTo(14.5f, 8.6f, 18.5f, 12.2f, 18.5f, 12.2f)
                curveTo(18.5f, 12.2f, 22.5f, 8.6f, 22.5f, 5.8f)
                curveTo(22.5f, 3.6f, 20.7f, 1.8f, 18.5f, 1.8f)
                close()
            }
            path(fill = SolidColor(Color(0xFFFBBF24))) {
                moveTo(18.3f, 1.6f)
                curveTo(16.2f, 1.6f, 14.5f, 3.3f, 14.5f, 5.4f)
                curveTo(14.5f, 8.0f, 18.3f, 11.4f, 18.3f, 11.4f)
                curveTo(18.3f, 11.4f, 22.1f, 8.0f, 22.1f, 5.4f)
                curveTo(22.1f, 3.3f, 20.4f, 1.6f, 18.3f, 1.6f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(18.3f, 3.8f)
                curveTo(17.42f, 3.8f, 16.7f, 4.52f, 16.7f, 5.4f)
                curveTo(16.7f, 6.28f, 17.42f, 7.0f, 18.3f, 7.0f)
                curveTo(19.18f, 7.0f, 19.9f, 6.28f, 19.9f, 5.4f)
                curveTo(19.9f, 4.52f, 19.18f, 3.8f, 18.3f, 3.8f)
                close()
            }
        }.build()
    }

    /**
     * Navigation Bar Tab 3 (Inactive): Realistic 3D Earth Globe Icon
     */
    val Public: ImageVector by lazy { NavCitiesActive }

    val PublicOutlined: ImageVector by lazy { NavCitiesActive }

    /**
     * Navigation Bar Tab 4 (Active): Realistic 3D Precision Metallic Control Deck
     * with Illuminated Cyan/Amber Equalizer Tracks and Tactile 3D Knobs.
     */
    val NavSettingsActive: ImageVector by lazy {
        ImageVector.Builder(
            name = "NavSettingsActive",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Frosted dark glass control console plate
            path(fill = SolidColor(Color(0xFF0F2942))) {
                moveTo(5.0f, 2.5f)
                horizontalLineTo(19.0f)
                curveTo(20.38f, 2.5f, 21.5f, 3.62f, 21.5f, 5.0f)
                verticalLineTo(19.0f)
                curveTo(21.5f, 20.38f, 20.38f, 21.5f, 19.0f, 21.5f)
                horizontalLineTo(5.0f)
                curveTo(3.62f, 21.5f, 2.5f, 20.38f, 2.5f, 19.0f)
                verticalLineTo(5.0f)
                curveTo(2.5f, 3.62f, 3.62f, 2.5f, 5.0f, 2.5f)
                close()
            }
            // Outer glass bezel border
            path(
                stroke = SolidColor(Color(0xFF38BDF8).copy(alpha = 0.55f)),
                strokeLineWidth = 1.2f
            ) {
                moveTo(5.0f, 2.5f)
                horizontalLineTo(19.0f)
                curveTo(20.38f, 2.5f, 21.5f, 3.62f, 21.5f, 5.0f)
                verticalLineTo(19.0f)
                curveTo(21.5f, 20.38f, 20.38f, 21.5f, 19.0f, 21.5f)
                horizontalLineTo(5.0f)
                curveTo(3.62f, 21.5f, 2.5f, 20.38f, 2.5f, 19.0f)
                verticalLineTo(5.0f)
                curveTo(2.5f, 3.62f, 3.62f, 2.5f, 5.0f, 2.5f)
                close()
            }
            // Recessed unlit right-side slider grooves
            path(
                stroke = SolidColor(Color(0xFF1E3A5F)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(15.0f, 6.8f); horizontalLineTo(18.8f)
                moveTo(9.2f, 12.0f); horizontalLineTo(18.8f)
                moveTo(15.6f, 17.2f); horizontalLineTo(18.8f)
            }
            // Active illuminated cyan & emerald left-side slider tracks
            path(
                stroke = SolidColor(Color(0xFF38BDF8)),
                strokeLineWidth = 2.3f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5.2f, 6.8f); horizontalLineTo(14.5f)
                moveTo(5.2f, 17.2f); horizontalLineTo(15.2f)
            }
            path(
                stroke = SolidColor(Color(0xFFFBBF24)),
                strokeLineWidth = 2.3f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5.2f, 12.0f); horizontalLineTo(9.2f)
            }
            // Top 3D Metallic Slider Knob (outer ring + bright core)
            path(fill = SolidColor(Color(0xFFBAE6FD))) {
                moveTo(14.8f, 4.5f)
                curveTo(13.53f, 4.5f, 12.5f, 5.53f, 12.5f, 6.8f)
                curveTo(12.5f, 8.07f, 13.53f, 9.1f, 14.8f, 9.1f)
                curveTo(16.07f, 9.1f, 17.1f, 8.07f, 17.1f, 6.8f)
                curveTo(17.1f, 5.53f, 16.07f, 4.5f, 14.8f, 4.5f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(14.8f, 5.5f)
                curveTo(14.08f, 5.5f, 13.5f, 6.08f, 13.5f, 6.8f)
                curveTo(13.5f, 7.52f, 14.08f, 8.1f, 14.8f, 8.1f)
                curveTo(15.52f, 8.1f, 16.1f, 7.52f, 16.1f, 6.8f)
                curveTo(16.1f, 6.08f, 15.52f, 5.5f, 14.8f, 5.5f)
                close()
            }
            // Middle 3D Golden Amber Slider Knob
            path(fill = SolidColor(Color(0xFFF59E0B))) {
                moveTo(9.2f, 9.7f)
                curveTo(7.93f, 9.7f, 6.9f, 10.73f, 6.9f, 12.0f)
                curveTo(6.9f, 13.27f, 7.93f, 14.3f, 9.2f, 14.3f)
                curveTo(10.47f, 14.3f, 11.5f, 13.27f, 11.5f, 12.0f)
                curveTo(11.5f, 10.73f, 10.47f, 9.7f, 9.2f, 9.7f)
                close()
            }
            path(fill = SolidColor(Color(0xFFFEF08A))) {
                moveTo(9.2f, 10.7f)
                curveTo(8.48f, 10.7f, 7.9f, 11.28f, 7.9f, 12.0f)
                curveTo(7.9f, 12.72f, 8.48f, 13.3f, 9.2f, 13.3f)
                curveTo(9.92f, 13.3f, 10.5f, 12.72f, 10.5f, 12.0f)
                curveTo(10.5f, 11.28f, 9.92f, 10.7f, 9.2f, 10.7f)
                close()
            }
            // Bottom 3D Emerald-Cyan Slider Knob
            path(fill = SolidColor(Color(0xFF34D399))) {
                moveTo(15.4f, 14.9f)
                curveTo(14.13f, 14.9f, 13.1f, 15.93f, 13.1f, 17.2f)
                curveTo(13.1f, 18.47f, 14.13f, 19.5f, 15.4f, 19.5f)
                curveTo(16.67f, 19.5f, 17.7f, 18.47f, 17.7f, 17.2f)
                curveTo(17.7f, 15.93f, 16.67f, 14.9f, 15.4f, 14.9f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(15.4f, 15.9f)
                curveTo(14.68f, 15.9f, 14.1f, 16.48f, 14.1f, 17.2f)
                curveTo(14.1f, 17.92f, 14.68f, 18.5f, 15.4f, 18.5f)
                curveTo(16.12f, 18.5f, 16.7f, 17.92f, 16.7f, 17.2f)
                curveTo(16.7f, 16.48f, 16.12f, 15.9f, 15.4f, 15.9f)
                close()
            }
        }.build()
    }

    /**
     * Navigation Bar Tab 4 (Inactive): Realistic 3D Precision Control Console Icon
     */
    val Settings: ImageVector by lazy { NavSettingsActive }

    val SettingsOutlined: ImageVector by lazy { NavSettingsActive }

    /**
     * GPS Crosshair Target Icon (`MyLocation` / `GpsFixed` / `LocationSearching`)
     */
    val MyLocation: ImageVector by lazy {
        ImageVector.Builder(
            name = "GpsCrosshair",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(19f, 12f)
                curveTo(19f, 15.87f, 15.87f, 19f, 12f, 19f)
                curveTo(8.13f, 19f, 5f, 15.87f, 5f, 12f)
                curveTo(5f, 8.13f, 8.13f, 5f, 12f, 5f)
                curveTo(15.87f, 5f, 19f, 8.13f, 19f, 12f)
                close()
                moveTo(12f, 2f); lineTo(12f, 5f)
                moveTo(12f, 19f); lineTo(12f, 22f)
                moveTo(2f, 12f); lineTo(5f, 12f)
                moveTo(19f, 12f); lineTo(22f, 12f)
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 9.2f)
                curveTo(10.45f, 9.2f, 9.2f, 10.45f, 9.2f, 12f)
                curveTo(9.2f, 13.55f, 10.45f, 14.8f, 12f, 14.8f)
                curveTo(13.55f, 14.8f, 14.8f, 13.55f, 14.8f, 12f)
                curveTo(14.8f, 10.45f, 13.55f, 9.2f, 12f, 9.2f)
                close()
            }
        }.build()
    }

    val LocationSearching: ImageVector by lazy { MyLocation }
    val GpsFixed: ImageVector by lazy { MyLocation }

    /**
     * 4-Point Sparkle Constellation Icon (`AutoAwesome`)
     */
    val AutoAwesome: ImageVector by lazy {
        ImageVector.Builder(
            name = "AutoAwesome",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFFFBBF24))) {
                moveTo(11f, 2f)
                lineTo(13.2f, 7.8f)
                lineTo(19f, 10f)
                lineTo(13.2f, 12.2f)
                lineTo(11f, 18f)
                lineTo(8.8f, 12.2f)
                lineTo(3f, 10f)
                lineTo(8.8f, 7.8f)
                close()

                moveTo(18.5f, 14.5f)
                lineTo(19.4f, 17.1f)
                lineTo(22f, 18f)
                lineTo(19.4f, 18.9f)
                lineTo(18.5f, 21.5f)
                lineTo(17.6f, 18.9f)
                lineTo(15f, 18f)
                lineTo(17.6f, 17.1f)
                close()
            }
        }.build()
    }

    val ExpandLess: ImageVector = Icons.Filled.KeyboardArrowUp
    val ExpandMore: ImageVector = Icons.Filled.KeyboardArrowDown
    val PushPin: ImageVector by lazy { AutoAwesome }
    val PushPinOutlined: ImageVector by lazy { AutoAwesome }
    val DeleteOutline: ImageVector = Icons.Filled.Delete
    val CompareArrows: ImageVector = Icons.Filled.Refresh
    val Palette: ImageVector by lazy { AutoAwesome }
    val NotificationsActive: ImageVector = Icons.Filled.Notifications
    val HealthAndSafety: ImageVector = Icons.Filled.Info
    val Explore: ImageVector by lazy { CompassCalibration }
    val LocationOn: ImageVector = Icons.Filled.LocationOn
    val Search: ImageVector = Icons.Filled.Search
    val Clear: ImageVector = Icons.Filled.Clear
    val AddCircle: ImageVector = Icons.Filled.AddCircle
    val Check: ImageVector = Icons.Filled.Check
    val CheckCircle: ImageVector = Icons.Filled.CheckCircle
    val Warning: ImageVector = Icons.Filled.Warning
    val Refresh: ImageVector = Icons.Filled.Refresh
    val Close: ImageVector = Icons.Filled.Close
    val Send: ImageVector by lazy {
        ImageVector.Builder(
            name = "Send",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF38BDF8))) {
                moveTo(2.01f, 21f)
                lineTo(23f, 12f)
                lineTo(2.01f, 3f)
                lineTo(2f, 10f)
                lineTo(17f, 12f)
                lineTo(2f, 14f)
                close()
            }
        }.build()
    }
}

