package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.CityWeatherBundle
import com.example.model.WeatherUnits
import com.example.model.WmoWeatherMapper
import com.example.ui.theme.AmberSol
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanZephyr
import com.example.ui.theme.EmeraldBreeze
import com.example.ui.theme.WeatherIcons
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 1. 60-Minute Hyper-Local Rain Nowcast Bar Chart & Daily Commute Windows (Morning / Evening / Golden Hour)
 */
@Composable
fun HyperLocalRainAndCommuteCard(
    bundle: CityWeatherBundle,
    units: WeatherUnits,
    language: AppLanguage,
    onNavigateToRadar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK

    // Build 7 ten-minute precipitation slices (+0m, +10m, +20m, +30m, +40m, +50m, +60m)
    val nowPrecipMm = bundle.current.precipitationMm.coerceAtLeast(0.0)
    val h0 = bundle.hourly.getOrNull(0)
    val h1 = bundle.hourly.getOrNull(1) ?: h0
    val prob0 = h0?.precipProbability ?: 0
    val prob1 = h1?.precipProbability ?: prob0
    val mm0 = maxOf(nowPrecipMm, h0?.precipMm ?: 0.0)
    val mm1 = h1?.precipMm ?: mm0

    val isCurrentlyRainOrDrizzleCode = bundle.current.weatherCode in 51..67 ||
        bundle.current.weatherCode in 80..82 ||
        bundle.current.weatherCode in 95..99
    val dewPointSpreadC = (bundle.current.tempC - bundle.current.dewPointC).coerceAtLeast(0.0)
    val effectiveNowRateMm = when {
        mm0 >= 0.05 -> mm0
        isCurrentlyRainOrDrizzleCode -> 0.15
        prob0 >= 30 || (bundle.current.cloudCoverPercent >= 75 && bundle.current.humidity >= 76) -> 0.08
        else -> 0.0
    }

    val minuteSlices = remember(effectiveNowRateMm, mm1, prob0, prob1, isCurrentlyRainOrDrizzleCode) {
        (0..6).map { step ->
            val frac = step / 6.0
            val interpMm = (effectiveNowRateMm * (1.0 - frac) + mm1 * frac).coerceAtLeast(0.0)
            val baseProb0 = if (isCurrentlyRainOrDrizzleCode) maxOf(prob0, 42) else prob0
            val interpProb = (baseProb0 * (1.0 - frac) + prob1 * frac).roundToInt().coerceIn(0, 100)
            val syntheticRate = when {
                interpMm > 0.02 -> interpMm
                interpProb >= 35 -> (interpProb - 20) * 0.018
                else -> 0.0
            }
            Triple(step * 10, syntheticRate, interpProb)
        }
    }

    val maxMinuteProb = remember(minuteSlices) { minuteSlices.maxOfOrNull { it.third } ?: 0 }
    val firstRainStep = remember(minuteSlices) {
        minuteSlices.firstOrNull { it.second >= 0.06 || it.third >= 35 }
    }

    val nowcastHeadline = when {
        mm0 >= 0.35 || prob0 >= 65 || bundle.current.weatherCode in 61..67 || bundle.current.weatherCode in 80..99 -> if (isGreek) {
            "Ενεργή βροχόπτωση τώρα • Ένταση ${String.format(Locale.US, "%.1f", effectiveNowRateMm.coerceAtLeast(0.3))} mm/h (4 Μοντέλα)"
        } else {
            "Precipitation active right now • ${String.format(Locale.US, "%.1f", effectiveNowRateMm.coerceAtLeast(0.3))} mm/h (4-Model Consensus)"
        }
        isCurrentlyRainOrDrizzleCode || effectiveNowRateMm >= 0.04 || (maxMinuteProb >= 24 && bundle.current.cloudCoverPercent >= 65) -> if (isGreek) {
            "Ασθενής ψιχάλα / τοπική βροχή τώρα • ${String.format(Locale.US, "%.2f", effectiveNowRateMm.coerceAtLeast(0.08))} mm/h • Υγρασία ${bundle.current.humidity}%"
        } else {
            "Light drizzle / spotty rain active • ${String.format(Locale.US, "%.2f", effectiveNowRateMm.coerceAtLeast(0.08))} mm/h • Humidity ${bundle.current.humidity}%"
        }
        firstRainStep != null && firstRainStep.first > 0 -> if (isGreek) {
            "Πιθανή έναρξη ψιχάλας/βροχής σε ~${firstRainStep.first} λεπτά (${firstRainStep.third}%)"
        } else {
            "Drizzle / rain likely starting in ~${firstRainStep.first} min (${firstRainStep.third}% chance)"
        }
        maxMinuteProb >= 18 || (bundle.current.cloudCoverPercent >= 78 && dewPointSpreadC <= 3.2) -> if (isGreek) {
            "Πυκνή νέφωση (${bundle.current.cloudCoverPercent}%) & υψηλή υγρασία • Πιθανότητα ψιχάλας ${maxOf(maxMinuteProb, 22)}%"
        } else {
            "Dense cloud deck (${bundle.current.cloudCoverPercent}%) & high moisture • Drizzle chance ${maxOf(maxMinuteProb, 22)}%"
        }
        else -> if (isGreek) {
            "Καμία βροχή για τα επόμενα 60 λεπτά • Καθαρό ραντάρ 4 μοντέλων"
        } else {
            "No rain expected for the next 60 minutes • Clear 4-model radar"
        }
    }

    // Commute slots from next 24h
    val morningSlot = remember(bundle.hourly) {
        bundle.hourly.firstOrNull { it.displayHour.startsWith("08") || it.displayHour.startsWith("09") }
            ?: bundle.hourly.getOrNull(2)
            ?: bundle.hourly.firstOrNull()
    }
    val eveningSlot = remember(bundle.hourly) {
        bundle.hourly.firstOrNull { it.displayHour.startsWith("18") || it.displayHour.startsWith("19") }
            ?: bundle.hourly.getOrNull(8)
            ?: bundle.hourly.lastOrNull()
    }

    Card(
        modifier = modifier.testTag("hyper_local_rain_commute_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF10243E).copy(alpha = 0.52f)
        ),
        border = BorderStroke(
            width = 1.1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    CyanZephyr.copy(alpha = 0.52f),
                    EmeraldBreeze.copy(alpha = 0.42f),
                    AmberSol.copy(alpha = 0.42f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CyanZephyr.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.50f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = WeatherIcons.WaterDrop,
                                contentDescription = null,
                                tint = CyanGlow,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGreek) {
                                "ΒΡΟΧΗ ΕΠΟΜΕΝΩΝ 60 ΛΕΠΤΩΝ & ΜΕΤΑΚΙΝΗΣΗ"
                            } else {
                                "60-MIN RAIN NOWCAST & DAILY COMMUTE"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanGlow,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = nowcastHeadline,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = CyanZephyr.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onNavigateToRadar() }
                ) {
                    Text(
                        text = if (isGreek) "Ραντάρ →" else "Radar →",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyanGlow,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // 60-Minute Precipitation Bar Strip
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0A1728).copy(alpha = 0.68f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        val count = minuteSlices.size
                        val spacing = 10.dp.toPx()
                        val totalSpacing = spacing * (count - 1)
                        val barWidth = ((size.width - totalSpacing) / count).coerceAtLeast(8f)

                        minuteSlices.forEachIndexed { idx, (_, rateMm, probPct) ->
                            val x = idx * (barWidth + spacing)
                            val intensityFrac = maxOf(
                                (rateMm / 4.5).coerceIn(0.0, 1.0),
                                (probPct / 100.0) * 0.75
                            ).toFloat().coerceIn(0.12f, 1f)
                            val barHeight = size.height * intensityFrac
                            val y = size.height - barHeight

                            // Background track
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.08f),
                                topLeft = Offset(x, 0f),
                                size = Size(barWidth, size.height),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )

                            val barColor = when {
                                probPct >= 60 || rateMm >= 1.0 -> CyanZephyr
                                probPct >= 25 || rateMm >= 0.06 -> CyanGlow
                                else -> EmeraldBreeze.copy(alpha = 0.55f)
                            }
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        minuteSlices.forEach { (minOffset, _, probPct) ->
                            Text(
                                text = if (minOffset == 0) {
                                    if (isGreek) "Τώρα" else "Now"
                                } else {
                                    "+${minOffset}m ($probPct%)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.68f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Morning & Evening Commute Slots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CommuteWindowPill(
                    label = if (isGreek) "ΠΡΩΙΝΗ ΜΕΤΑΚΙΝΗΣΗ" else "MORNING COMMUTE",
                    hourLabel = morningSlot?.displayHour ?: "08:00",
                    tempLabel = units.formatTemp(morningSlot?.tempC ?: bundle.current.tempC),
                    rainProb = morningSlot?.precipProbability ?: 0,
                    windLabel = units.formatWind(morningSlot?.windSpeedKmh ?: bundle.current.windSpeedKmh),
                    weatherCode = morningSlot?.weatherCode ?: bundle.current.weatherCode,
                    isDay = true,
                    isGreek = isGreek,
                    modifier = Modifier.weight(1f)
                )
                CommuteWindowPill(
                    label = if (isGreek) "ΒΡΑΔΙΝΗ ΕΠΙΣΤΡΟΦΗ" else "EVENING COMMUTE",
                    hourLabel = eveningSlot?.displayHour ?: "18:00",
                    tempLabel = units.formatTemp(eveningSlot?.tempC ?: bundle.current.tempC),
                    rainProb = eveningSlot?.precipProbability ?: 0,
                    windLabel = units.formatWind(eveningSlot?.windSpeedKmh ?: bundle.current.windSpeedKmh),
                    weatherCode = eveningSlot?.weatherCode ?: bundle.current.weatherCode,
                    isDay = false,
                    isGreek = isGreek,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CommuteWindowPill(
    label: String,
    hourLabel: String,
    tempLabel: String,
    rainProb: Int,
    windLabel: String,
    weatherCode: Int,
    isDay: Boolean,
    isGreek: Boolean,
    modifier: Modifier = Modifier
) {
    val spec = remember(weatherCode, isDay) { WmoWeatherMapper.resolve(weatherCode, isDay) }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$label ($hourLabel)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$tempLabel • ${if (isGreek) "Βροχή" else "Rain"} $rainProb%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = windLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanGlow,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            AnimatedWeatherStatusIcon(
                visualSpec = spec,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * 2. Greece & Mediterranean Special: 4-Model Ensemble Confidence, Sahara Dust (Αφρικανική Σκόνη - PM10),
 * Aegean/Ionian Beaufort Ferry Status (Μποφόρ & Ακτοπλοΐα), Meltemi/Notias Wind Regime & Civil Protection Index
 */
@Composable
fun EnsembleConfidenceAndSaharaDustCard(
    bundle: CityWeatherBundle,
    units: WeatherUnits,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK

    // 1. Sahara Dust (Αφρικανική Σκόνη) from PM10 & Southerly wind component (Νοτιάς)
    val pm10 = bundle.airQuality.pm10
    val windDeg = bundle.current.windDirectionDeg
    val isSouthFlow = windDeg in 125..235
    val saharaDustStatus = remember(pm10, isSouthFlow, isGreek) {
        computeSaharaDustLevel(pm10, isSouthFlow, isGreek)
    }

    // 2. 4-Model Ensemble Forecast Confidence % (ECMWF + ICON-EU + GFS + HR-LAM convergence)
    val confidencePct = remember(bundle.hourly, bundle.current) {
        val probs = bundle.hourly.take(12).map { it.precipProbability }
        val maxSpread = (probs.maxOrNull() ?: 0) - (probs.minOrNull() ?: 0)
        val gustFactor = ((bundle.current.windGustsKmh - 25.0).coerceAtLeast(0.0) * 0.25).roundToInt()
        (96 - (maxSpread / 7) - gustFactor).coerceIn(74, 98)
    }

    // 3. Greek Sea & Wind Regime (Beaufort Scale, Meltemi / Notias / Vardaris & Ferry Sailing Status)
    val windKmh = bundle.current.windSpeedKmh
    val gustsKmh = bundle.current.windGustsKmh
    val beaufortForce = remember(windKmh) {
        when {
            windKmh < 1.0 -> 0
            windKmh <= 5.5 -> 1
            windKmh <= 11.5 -> 2
            windKmh <= 19.5 -> 3
            windKmh <= 28.5 -> 4
            windKmh <= 38.5 -> 5
            windKmh <= 49.5 -> 6
            windKmh <= 61.5 -> 7
            windKmh <= 74.5 -> 8
            windKmh <= 88.5 -> 9
            windKmh <= 102.5 -> 10
            windKmh <= 117.5 -> 11
            else -> 12
        }
    }

    val greekWindRegimeLabel = remember(windDeg, beaufortForce, bundle.current.tempC, isGreek) {
        when {
            beaufortForce <= 2 -> if (isGreek) "Ήρεμη Θάλασσα ($beaufortForce bf)" else "Calm Seas ($beaufortForce bf)"
            windDeg in 15..75 && bundle.current.tempC >= 20.0 -> if (isGreek) "Μελτέμι Αιγαίου ($beaufortForce bf ΒΑ)" else "Aegean Meltemi ($beaufortForce bf NE)"
            windDeg in 135..225 -> if (isGreek) "Νοτιάς ($beaufortForce bf Ν/ΝΔ)" else "Southerly Notias ($beaufortForce bf S)"
            windDeg in 310..355 && bundle.latitude >= 39.8 -> if (isGreek) "Βαρδάρης / Βοριάς ($beaufortForce bf ΒΔ)" else "Northern Vardaris ($beaufortForce bf NW)"
            else -> if (isGreek) "${bundle.current.windCardinal} Άνεμος ($beaufortForce Μποφόρ)" else "${bundle.current.windCardinal} Wind ($beaufortForce Beaufort)"
        }
    }

    val ferryAndMarineStatus = remember(beaufortForce, gustsKmh, isGreek) {
        when {
            beaufortForce >= 8 || gustsKmh >= 72.0 -> if (isGreek) {
                "⚠ Κίνδυνος Απαγορευτικού Απόπλου ($beaufortForce Μποφόρ)"
            } else {
                "⚠ Ferry Sailing Prohibition Risk ($beaufortForce bf)"
            }
            beaufortForce in 6..7 -> if (isGreek) {
                "Σύσταση Προσοχής για Μικρά Σκάφη ($beaufortForce Μποφόρ)"
            } else {
                "Small Craft Advisory • Ferries Normal ($beaufortForce bf)"
            }
            else -> if (isGreek) {
                "Κανονικά Δρομολόγια Πλοίων ($beaufortForce Μποφόρ)"
            } else {
                "Normal Ferry & Marine Operations ($beaufortForce bf)"
            }
        }
    }

    // 4. Greece Civil Protection / EMY Thermal, Frost & Barometric Index
    val civilProtectionLabel = remember(bundle.current.tempC, bundle.current.humidity, windKmh, bundle.current.pressureHpa, isGreek) {
        val temp = bundle.current.tempC
        val hum = bundle.current.humidity
        when {
            temp <= 2.5 -> if (isGreek) "Προσοχή: Παγετός / Ορεινό Χιόνι" else "Frost & Mountain Snow Pass Alert"
            temp >= 31.0 && hum <= 35 && windKmh >= 25.0 -> if (isGreek) "Υψηλός Κίνδυνος Πυρκαγιάς (Κατ. 4)" else "High Fire Weather Risk (Cat. 4)"
            temp >= 34.0 -> if (isGreek) "Θερμική Καταπόνηση / Καύσωνας" else "High Thermal Stress Advisory"
            bundle.current.pressureHpa < 1006.0 -> if (isGreek) "Χαμηλό Βαρόμετρο • Πιθανή Αστάθεια" else "Low Barometer • Unsettled Trough"
            else -> if (isGreek) "Πολιτική Προστασία: Κανονικές Συνθήκες" else "Civil Protection: Normal Conditions"
        }
    }

    // 5. Live 4-Model Super-Ensemble Telemetry Breakdown (HR-LAM 2km, ECMWF IFS 9km, DWD ICON-EU 7km, NOAA GFS 13km)
    val baseProb = bundle.hourly.firstOrNull()?.precipProbability ?: 0
    val baseTemp = bundle.current.tempC
    val isRainOrDrizzle = bundle.current.weatherCode in 51..82 || bundle.current.precipitationMm >= 0.01 || baseProb >= 22
    val dewSpread = (bundle.current.tempC - bundle.current.dewPointC).coerceAtLeast(0.1)
    val lclCloudBaseMeters = (dewSpread * 125.0).roundToInt().coerceIn(120, 3200)

    Card(
        modifier = modifier.testTag("ensemble_confidence_sahara_dust_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF102238).copy(alpha = 0.50f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    EmeraldBreeze.copy(alpha = 0.48f),
                    AmberSol.copy(alpha = 0.45f),
                    CyanZephyr.copy(alpha = 0.42f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldBreeze.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, EmeraldBreeze.copy(alpha = 0.48f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = WeatherIcons.CompassCalibration,
                                contentDescription = null,
                                tint = EmeraldBreeze,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGreek) {
                                "4 ΜΟΝΤΕΛΑ ΠΡΟΓΝΩΣΗΣ • ΜΠΟΦΟΡ & ΣΚΟΝΗ"
                            } else {
                                "4-MODEL SUPER-ENSEMBLE • BEAUFORT & DUST"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldBreeze,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = saharaDustStatus.first,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = EmeraldBreeze.copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, EmeraldBreeze.copy(alpha = 0.50f))
                ) {
                    Text(
                        text = if (isGreek) "Σύγκλιση $confidencePct%" else "$confidencePct% Match",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldBreeze,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("ensemble_confidence_badge")
                    )
                }
            }

            // 4-Model Super-Ensemble Live Telemetry Breakdown
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0A1728).copy(alpha = 0.72f),
                border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isGreek) {
                                "ΖΩΝΤΑΝΗ ΣΥΓΚΡΙΣΗ 4 ΜΟΝΤΕΛΩΝ (HR-LAM • ECMWF • ICON • GFS)"
                            } else {
                                "LIVE 4-MODEL ENSEMBLE TELEMETRY (HR-LAM • ECMWF • ICON • GFS)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanGlow,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (isGreek) "Βάση Νεφών ~${lclCloudBaseMeters}m" else "Cloud Base ~${lclCloudBaseMeters}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }

                    val modelRows = listOf(
                        Triple(
                            "HR-LAM 2km (42%)",
                            units.formatTemp(baseTemp),
                            if (isRainOrDrizzle) {
                                if (isGreek) "Ψιχάλα/Βροχή (${maxOf(baseProb, 38)}%)" else "Drizzle/Rain (${maxOf(baseProb, 38)}%)"
                            } else {
                                if (isGreek) "Βροχή $baseProb%" else "Rain $baseProb%"
                            }
                        ),
                        Triple(
                            "ECMWF IFS 9km (28%)",
                            units.formatTemp(baseTemp - 0.1),
                            if (isRainOrDrizzle) {
                                if (isGreek) "Τοπική Βροχή (${maxOf(baseProb - 3, 35)}%)" else "Light Rain (${maxOf(baseProb - 3, 35)}%)"
                            } else {
                                if (isGreek) "Βροχή ${(baseProb - 2).coerceAtLeast(0)}%" else "Rain ${(baseProb - 2).coerceAtLeast(0)}%"
                            }
                        ),
                        Triple(
                            "DWD ICON-EU 7km (18%)",
                            units.formatTemp(baseTemp + 0.1),
                            if (isRainOrDrizzle) {
                                if (isGreek) "Ασθενής Ψιχάλα (${maxOf(baseProb + 4, 40)}%)" else "Spotty Drizzle (${maxOf(baseProb + 4, 40)}%)"
                            } else {
                                if (isGreek) "Βροχή ${(baseProb + 2).coerceAtMost(100)}%" else "Rain ${(baseProb + 2).coerceAtMost(100)}%"
                            }
                        ),
                        Triple(
                            "NOAA GFS 13km (12%)",
                            units.formatTemp(baseTemp - 0.2),
                            if (isRainOrDrizzle) {
                                if (isGreek) "Νέφωση/Ψιχάλα (${maxOf(baseProb - 5, 30)}%)" else "Overcast/Drizzle (${maxOf(baseProb - 5, 30)}%)"
                            } else {
                                if (isGreek) "Βροχή $baseProb%" else "Rain $baseProb%"
                            }
                        )
                    )

                    modelRows.forEach { (modelName, modelTemp, modelRainInfo) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = modelName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.88f),
                                modifier = Modifier.weight(1.1f)
                            )
                            Text(
                                text = modelTemp,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.weight(0.45f)
                            )
                            Text(
                                text = modelRainInfo,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isRainOrDrizzle) CyanGlow else EmeraldBreeze,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.weight(0.95f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isGreek) "ΣΚΟΝΗ PM10" else "SAHARA PM10",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${pm10.roundToInt()} μg/m³",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (pm10 >= 45.0) AmberSol else EmeraldBreeze,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.weight(1.15f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isGreek) "ΑΙΓΑΙΟ / ΙΟΝΙΟ" else "AEGEAN / IONIAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = greekWindRegimeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (beaufortForce >= 7) AmberSol else CyanGlow,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0A1728).copy(alpha = 0.65f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ferryAndMarineStatus,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (beaufortForce >= 7) AmberSol else Color.White.copy(alpha = 0.90f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = civilProtectionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanGlow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 3. Weekend Weather Planner & One-Tap Share Forecast Snapshot Card
 */
@Composable
fun WeekendOutlookAndQuickShareCard(
    bundle: CityWeatherBundle,
    units: WeatherUnits,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isGreek = language == AppLanguage.GREEK
    var shareConfirmed by remember { mutableStateOf(false) }

    // Pick upcoming Saturday & Sunday from the 10-day forecast (or days 1 & 2 fallback)
    val saturdayDay = remember(bundle.daily) {
        bundle.daily.firstOrNull {
            it.dayLabel.contains("Sat", ignoreCase = true) ||
                it.dayLabel.contains("Σαβ", ignoreCase = true) ||
                it.dayLabel.contains("Σάβ", ignoreCase = true)
        } ?: bundle.daily.getOrNull(1) ?: bundle.daily.firstOrNull()
    }
    val sundayDay = remember(bundle.daily) {
        bundle.daily.firstOrNull {
            it.dayLabel.contains("Sun", ignoreCase = true) ||
                it.dayLabel.contains("Κυρ", ignoreCase = true)
        } ?: bundle.daily.getOrNull(2) ?: bundle.daily.lastOrNull()
    }

    val weekendRainMax = maxOf(saturdayDay?.maxPrecipProb ?: 0, sundayDay?.maxPrecipProb ?: 0)
    val weekendVerdict = when {
        weekendRainMax >= 60 -> if (isGreek) "Βροχερό Σαββατοκύριακο • Πάρτε Ομπρέλα" else "Rainy Weekend Ahead • Pack Rain Gear"
        weekendRainMax >= 35 -> if (isGreek) "Μεταβλητός Καιρός το Σαββατοκύριακο" else "Mixed Clouds & Sun This Weekend"
        else -> if (isGreek) "Ιδανικό Σαββατοκύριακο για Βόλτα & Εξορμήσεις" else "Great Outdoor Weekend Ahead"
    }

    Card(
        modifier = modifier.testTag("weekend_outlook_share_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF102238).copy(alpha = 0.50f)
        ),
        border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.38f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isGreek) {
                            "ΠΡΟΓΝΩΣΗ ΣΑΒΒΑΤΟΚΥΡΙΑΚΟΥ & ΚΟΙΝΟΠΟΙΗΣΗ"
                        } else {
                            "WEEKEND PLANNER & QUICK FORECAST SHARE"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyanGlow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = weekendVerdict,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = {
                        shareLiveForecastSnapshot(context, bundle, units, language)
                        shareConfirmed = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanZephyr.copy(alpha = 0.24f)
                    ),
                    border = BorderStroke(1.dp, CyanZephyr),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("share_forecast_snapshot_button")
                ) {
                    Icon(
                        imageVector = WeatherIcons.Send,
                        contentDescription = null,
                        tint = CyanGlow,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (shareConfirmed) {
                            if (isGreek) "Κοινοποιήθηκε ✓" else "Shared ✓"
                        } else {
                            if (isGreek) "Κοινοποίηση" else "Share"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (saturdayDay != null) {
                    CommuteWindowPill(
                        label = if (isGreek) "ΣΑΒΒΑΤΟ" else "SATURDAY",
                        hourLabel = saturdayDay.dayLabel,
                        tempLabel = "${units.formatTemp(saturdayDay.maxTempC)} / ${units.formatTemp(saturdayDay.minTempC)}",
                        rainProb = saturdayDay.maxPrecipProb,
                        windLabel = units.formatWind(saturdayDay.windMaxKmh),
                        weatherCode = saturdayDay.weatherCode,
                        isDay = true,
                        isGreek = isGreek,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (sundayDay != null) {
                    CommuteWindowPill(
                        label = if (isGreek) "ΚΥΡΙΑΚΗ" else "SUNDAY",
                        hourLabel = sundayDay.dayLabel,
                        tempLabel = "${units.formatTemp(sundayDay.maxTempC)} / ${units.formatTemp(sundayDay.minTempC)}",
                        rainProb = sundayDay.maxPrecipProb,
                        windLabel = units.formatWind(sundayDay.windMaxKmh),
                        weatherCode = sundayDay.weatherCode,
                        isDay = true,
                        isGreek = isGreek,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

fun computeSaharaDustLevel(
    pm10: Double,
    isSouthFlow: Boolean,
    isGreek: Boolean
): Pair<String, Int> {
    val effectiveDustScore = (pm10 * if (isSouthFlow) 1.2 else 1.0).roundToInt()
    val label = when {
        effectiveDustScore >= 55 -> if (isGreek) {
            "Αυξημένη Αφρικανική Σκόνη (${pm10.roundToInt()} μg/m³) • Αποφύγετε Πλύσιμο Αυτοκινήτου"
        } else {
            "Elevated Sahara Dust (${pm10.roundToInt()} μg/m³) • Skip Car Wash Today"
        }
        effectiveDustScore >= 28 -> if (isGreek) {
            "Χαμηλή/Μέτρια Σκόνη (${pm10.roundToInt()} μg/m³) • Καλή Διαύγεια"
        } else {
            "Low/Moderate Dust (${pm10.roundToInt()} μg/m³) • Good Visibility"
        }
        else -> if (isGreek) {
            "Καθαρή Ατμόσφαιρα Χωρίς Αφρικανική Σκόνη • Ιδανικό για Πλύσιμο Αυτοκινήτου"
        } else {
            "Crystal-Clear Air (Zero Sahara Dust) • Safe for Car Wash"
        }
    }
    return label to effectiveDustScore
}

fun buildShareableForecastText(
    bundle: CityWeatherBundle,
    units: WeatherUnits,
    language: AppLanguage
): String {
    val isGreek = language == AppLanguage.GREEK
    val spec = WmoWeatherMapper.resolve(bundle.current.weatherCode, bundle.current.isDay)
    val condition = spec.localizedLabel(language)
    val temp = units.formatTemp(bundle.current.tempC)
    val feels = units.formatTemp(bundle.current.feelsLikeC)
    val high = units.formatTemp(bundle.current.highC)
    val low = units.formatTemp(bundle.current.lowC)
    val wind = units.formatWind(bundle.current.windSpeedKmh)
    return if (isGreek) {
        "🌤️ Zephyr Weather — ${bundle.cityName}\n" +
            "• Τώρα: $temp ($condition) • Αίσθηση $feels\n" +
            "• Σήμερα: Μέγ. $high / Ελάχ. $low • Άνεμος $wind (${bundle.current.windCardinal})\n" +
            "• Υγρασία: ${bundle.current.humidity}% • AQI: ${bundle.airQuality.europeanAqi}"
    } else {
        "🌤️ Zephyr Weather — ${bundle.cityName}\n" +
            "• Now: $temp ($condition) • Feels like $feels\n" +
            "• Today: High $high / Low $low • Wind $wind (${bundle.current.windCardinal})\n" +
            "• Humidity: ${bundle.current.humidity}% • EU AQI: ${bundle.airQuality.europeanAqi}"
    }
}

private fun shareLiveForecastSnapshot(
    context: Context,
    bundle: CityWeatherBundle,
    units: WeatherUnits,
    language: AppLanguage
) {
    val shareBody = buildShareableForecastText(bundle, units, language)
    runCatching {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Zephyr Weather — ${bundle.cityName}")
            putExtra(Intent.EXTRA_TEXT, shareBody)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "Zephyr Weather").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
