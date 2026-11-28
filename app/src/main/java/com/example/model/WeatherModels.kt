package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.WeatherIcons
import java.util.Locale
import kotlin.math.ln
import kotlin.math.roundToInt

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val shortBadge: String
) {
    ENGLISH("en", "English", "EN"),
    GREEK("el", "Ελληνικά (Greek)", "ΕΛ")
}

enum class RadarLayerMode(
    val id: String,
    val titleEn: String,
    val titleEl: String
) {
    RAIN("rain", "Rain Radar", "Ραντάρ Βροχής"),
    WIND("wind", "Wind & Gusts", "Άνεμοι & Ριπές"),
    CLOUDS("clouds", "Cloud Deck", "Νεφοκάλυψη"),
    TEMP("temp", "Thermal Map", "Θερμοκρασία");

    fun localizedTitle(lang: AppLanguage): String =
        if (lang == AppLanguage.GREEK) titleEl else titleEn
}

enum class SkyParticleEffect {
    SUN_RAYS,
    SUNSET_DUSK,
    STARFIELD,
    CLOUDS,
    RAIN,
    SNOW,
    SNOW_STORM,
    THUNDER,
    FOG
}

enum class SkyAtmospherePreset(
    val id: String,
    val labelEn: String,
    val labelEl: String,
    val weatherCode: Int?,
    val isDay: Boolean,
    val isSunset: Boolean = false
) {
    LIVE_GPS("live", "Live Sky", "Ζωντανός", null, true),
    SUNSET_TWILIGHT("sunset", "Sunset Dusk", "Ηλιοβασίλεμα", 0, true, isSunset = true),
    STARLIT_NIGHT("night", "Night & Moon", "Νύχτα & Σελήνη", 0, false),
    SUNNY_CORONA("sunny", "Sunny Sky", "Ηλιοφάνεια", 0, true),
    HEAVY_RAIN("rain", "Rain Fall", "Βροχόπτωση", 65, true),
    THUNDERSTORM("storm", "Thunderstorm", "Καταιγίδα", 95, false),
    SNOW_CRYSTALS("snow", "Snow Clouds", "Σύννεφα Χιονιού", 73, true),
    SNOW_STORM("snow_storm", "Snow Storm", "Χιονοθύελλα", 75, true),
    DRIFTING_CLOUDS("clouds", "Cloud Deck", "Συννεφιά", 2, true);

    fun localizedLabel(lang: AppLanguage): String =
        if (lang == AppLanguage.GREEK) labelEl else labelEn

    fun localizedTitle(lang: AppLanguage): String = localizedLabel(lang)

    fun resolveVisualSpec(
        liveCode: Int,
        liveIsDay: Boolean,
        liveIsSunset: Boolean = false
    ): WeatherVisualSpec {
        return if (weatherCode == null) {
            WmoWeatherMapper.resolve(liveCode, liveIsDay, liveIsSunset)
        } else {
            val effectiveIsDay = when (this) {
                STARLIT_NIGHT, THUNDERSTORM -> false
                SUNNY_CORONA, SUNSET_TWILIGHT -> true
                else -> liveIsDay
            }
            WmoWeatherMapper.resolve(weatherCode, effectiveIsDay, this.isSunset)
        }
    }
}

enum class WeatherProvider(
    val id: String,
    val displayName: String,
    val subtitleEn: String,
    val subtitleEl: String,
    val resolutionBadge: String,
    val modelQuery: String?
) {
    BEST_MATCH(
        id = "best_match",
        displayName = "Multi-Model + Live Radar (Best)",
        subtitleEn = "Auto-fuses ECMWF IFS, DWD ICON, NOAA GFS & 15-min Live Doppler Nowcast Radar",
        subtitleEl = "Αυτόματος συνδυασμός ECMWF IFS, DWD ICON, GFS & Ζωντανού Ραντάρ 15 λεπτών",
        resolutionBadge = "1–2 km Radar + 3-Model Ensemble",
        modelQuery = null
    ),
    ECMWF(
        id = "ecmwf",
        displayName = "ECMWF IFS (European Centre)",
        subtitleEn = "Leading global numerical model (0.25° high-precision worldwide)",
        subtitleEl = "Κορυφαίο παγκόσμιο μετεωρολογικό μοντέλο (υψηλής ακρίβειας 0.25°)",
        resolutionBadge = "0.25° Global #1",
        modelQuery = "ecmwf_ifs025"
    ),
    DWD_ICON(
        id = "dwd_icon",
        displayName = "DWD ICON (German Weather Service)",
        subtitleEn = "High-resolution 2–7 km European & mountain terrain microclimate model",
        subtitleEl = "Μοντέλο υψηλής ανάλυσης 2–7 km για Ευρώπη, Ελλάδα & ορεινά μικροκλίματα",
        resolutionBadge = "2–7 km Europe HD",
        modelQuery = "icon_seamless"
    ),
    METEO_FRANCE(
        id = "meteo_france",
        displayName = "Météo-France AROME / ARPEGE",
        subtitleEn = "1.3–10 km Mediterranean & European high-resolution convective storm model",
        subtitleEl = "Μοντέλο 1.3–10 km υψηλής ακρίβειας για τη Μεσόγειο και καταιγίδες",
        resolutionBadge = "1.3–10 km Med HD",
        modelQuery = "meteofrance_seamless"
    ),
    NOAA_GFS(
        id = "gfs",
        displayName = "NOAA GFS + HRRR (US Weather Service)",
        subtitleEn = "Global Forecast System + 3 km HRRR with rapid hourly updates",
        subtitleEl = "Παγκόσμιο σύστημα πρόγνωσης NOAA με συνεχή ωριαία ανανέωση",
        resolutionBadge = "3–13 km Seamless",
        modelQuery = "gfs_seamless"
    ),
    UKMO(
        id = "ukmo",
        displayName = "UK Met Office (UKMO Seamless)",
        subtitleEn = "High-resolution 1.5–10 km global and regional atmospheric model",
        subtitleEl = "Παγκόσμιο ατμοσφαιρικό μοντέλο υψηλής ανάλυσης 1.5–10 km",
        resolutionBadge = "1.5–10 km Global",
        modelQuery = "ukmo_seamless"
    );

    val subtitle: String get() = subtitleEn
    fun localizedSubtitle(lang: AppLanguage): String =
        if (lang == AppLanguage.GREEK) subtitleEl else subtitleEn
}

enum class AppThemeSetting(
    val id: String,
    val displayName: String,
    val displayNameEl: String,
    val subtitle: String,
    val subtitleEl: String,
    val primaryHex: Long
) {
    AEGEAN_NIGHT(
        id = "aegean_night",
        displayName = "Deep Aegean Night",
        displayNameEl = "Βαθιά Νύχτα Αιγαίου",
        subtitle = "Lapis indigo, starlight particles, and oceanic cyan",
        subtitleEl = "Βαθύ μπλε του Αιγαίου, αστρική λάμψη και κυανές αποχρώσεις",
        primaryHex = 0xFF38BDF8
    ),
    MEDITERRANEAN_DAWN(
        id = "mediterranean_dawn",
        displayName = "Mediterranean Dawn",
        displayNameEl = "Μεσογειακή Αυγή",
        subtitle = "Warm sunrise amber, coral warmth, and golden dawn",
        subtitleEl = "Ζεστό κεχριμπάρι ανατολής και χρυσαφένιες αποχρώσεις",
        primaryHex = 0xFFFBBF24
    ),
    CYCLADIC_COBALT(
        id = "cycladic_cobalt",
        displayName = "Cycladic Cobalt",
        displayNameEl = "Κυκλαδίτικο Κοβάλτιο",
        subtitle = "Intense Greek island cobalt blue & sea-salt white",
        subtitleEl = "Έντονο κυκλαδίτικο μπλε και λευκό του αφρού της θάλασσας",
        primaryHex = 0xFF2563EB
    ),
    ONYX_MINIMAL(
        id = "onyx_minimal",
        displayName = "Onyx Minimal",
        displayNameEl = "Όνυχας Minimal",
        subtitle = "Pure pitch AMOLED black with silver monochrome",
        subtitleEl = "Απόλυτο μαύρο AMOLED για μέγιστη εξοικονόμηση ενέργειας",
        primaryHex = 0xFF94A3B8
    );

    fun localizedName(lang: AppLanguage): String =
        if (lang == AppLanguage.GREEK) displayNameEl else displayName

    fun localizedSubtitle(lang: AppLanguage): String =
        if (lang == AppLanguage.GREEK) subtitleEl else subtitle
}

enum class AutoRefreshInterval(
    val id: String,
    val displayName: String,
    val displayNameEl: String,
    val subtitle: String,
    val subtitleEl: String,
    val intervalMs: Long,
    val badge: String
) {
    EVERY_1_MIN(
        id = "1m",
        displayName = "Every 1 Minute",
        displayNameEl = "Κάθε 1 Λεπτό",
        subtitle = "Live 60-second updates with smart screen-off pause to prevent battery drain",
        subtitleEl = "Ζωντανή ανανέωση κάθε 60 δευτ. με έξυπνη παύση όταν σβήνει η οθόνη",
        intervalMs = 60_000L,
        badge = "1 Min Live"
    ),
    EVERY_5_MIN(
        id = "5m",
        displayName = "Every 5 Minutes",
        displayNameEl = "Κάθε 5 Λεπτά",
        subtitle = "High-cadence weather sync with ultra-low background power usage",
        subtitleEl = "Συχνός συγχρονισμός με ελάχιστη κατανάλωση μπαταρίας",
        intervalMs = 300_000L,
        badge = "5 Min Eco"
    ),
    EVERY_15_MIN(
        id = "15m",
        displayName = "Every 15 Minutes",
        displayNameEl = "Κάθε 15 Λεπτά",
        subtitle = "Balanced meteorological station update cadence",
        subtitleEl = "Ισορροπημένη συχνότητα ανανέωσης μετεωρολογικού σταθμού",
        intervalMs = 900_000L,
        badge = "15 Min Balanced"
    ),
    EVERY_30_MIN(
        id = "30m",
        displayName = "Every 30 Minutes",
        displayNameEl = "Κάθε 30 Λεπτά",
        subtitle = "Maximum battery conservation mode",
        subtitleEl = "Μέγιστη εξοικονόμηση μπαταρίας",
        intervalMs = 1_800_000L,
        badge = "30 Min Saver"
    );

    fun localizedName(lang: AppLanguage): String =
        if (lang == AppLanguage.GREEK) displayNameEl else displayName

    fun localizedSubtitle(lang: AppLanguage): String =
        if (lang == AppLanguage.GREEK) subtitleEl else subtitle
}

data class WeatherVisualSpec(
    val code: Int,
    val label: String,
    val labelEl: String,
    val summary: String,
    val summaryEl: String,
    val icon: ImageVector,
    val iconTint: Color,
    val gradientTop: Color,
    val gradientMid: Color,
    val gradientBottom: Color,
    val particleEffect: SkyParticleEffect,
    val isNight: Boolean = false
) {
    fun localizedLabel(lang: AppLanguage): String =
        if (lang == AppLanguage.GREEK) labelEl else label

    fun localizedSummary(lang: AppLanguage): String =
        if (lang == AppLanguage.GREEK) summaryEl else summary

    /**
     * Preserves the multi-color vector paths of our signature weather icons:
     * 1. WbSunny (Golden Sun)
     * 2. PartlySunny (Golden Sun + Pure White Cloud)
     * 3. Grain (Pure White Cloud + Sky-Blue Raindrops)
     * 4. SnowCloud & SnowStorm (Frosted Ice-Blue Cloud + Crystalline Snowflakes / Blizzard Gale)
     * 5. Thunderstorm (Pure White Cloud + Golden Lightning + Sky-Blue Raindrops)
     */
    fun resolvedIconTint(): Color = Color.Unspecified
}

/**
 * Maps WMO weather codes to weather icons & atmospheric sky textures:
 * - Daytime:
 *   1. Sun Only (`WeatherIcons.WbSunny`) -> codes 0, 1
 *   2. Sun with White Clouds (`WeatherIcons.PartlySunny`) -> codes 2, 3
 *   3. Foggy & Mist (`WeatherIcons.Foggy`) -> codes 45, 48
 *   4. White Clouds with Rain (`WeatherIcons.Grain`) -> codes 51, 53, 55, 61, 63, 65, 80, 81, 82
 *   5. Snow Clouds & Snowfall (`WeatherIcons.SnowCloud`) -> codes 56, 66, 71, 73, 77, 85
 *   6. Snow Storm / Blizzard (`WeatherIcons.SnowStorm`) -> codes 57, 67, 75, 86
 *   7. Thunderstorm (`WeatherIcons.Thunderstorm`) -> codes 95, 96, 99
 */
object WmoWeatherMapper {
    fun isNightHourLabel(hourLabel: String): Boolean {
        val clean = hourLabel.trim()
        val hourInt = clean.substringBefore(":").trim().toIntOrNull()
            ?: clean.take(2).toIntOrNull()
            ?: return false
        return hourInt >= 18 || hourInt <= 6
    }

    fun isSunsetClockTime(clockLabel: String, sunsetLabel: String = "17:30"): Boolean {
        val curMins = parseClockMinutes(clockLabel) ?: return false
        val setMins = parseClockMinutes(sunsetLabel) ?: (17 * 60 + 30)
        return curMins in (setMins - 45)..(setMins + 40)
    }

    private fun parseClockMinutes(clock: String): Int? {
        val parts = clock.trim().split(":")
        if (parts.size < 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].take(2).toIntOrNull() ?: return null
        return h * 60 + m
    }

    fun resolve(code: Int, isDay: Boolean = true, isSunset: Boolean = false): WeatherVisualSpec {
        if (isSunset && code < 51) {
            return WeatherVisualSpec(
                code = code,
                label = "Golden Sunset",
                labelEl = "Ηλιοβασίλεμα & Λυκόφως",
                summary = "Warm golden-crimson sunset horizon transitioning into velvet twilight.",
                summaryEl = "Μαγευτικό ηλιοβασίλεμα με πορφυρές και χρυσαφένιες αποχρώσεις στον ορίζοντα.",
                icon = if (code <= 1) WeatherIcons.WbSunny else WeatherIcons.PartlySunny,
                iconTint = Color(0xFFFB923C),
                gradientTop = Color(0xFF1F0E38),
                gradientMid = Color(0xFF832A52),
                gradientBottom = Color(0xFF0E0D22),
                particleEffect = SkyParticleEffect.SUNSET_DUSK,
                isNight = false
            )
        }

        val nightMode = !isDay
        return when (code) {
            0 -> if (isDay) {
                WeatherVisualSpec(
                    code = code,
                    label = "Clear Sky",
                    labelEl = "Αίθριος Ουρανός",
                    summary = "Abundant sunshine and crisp visibility across the horizon all day.",
                    summaryEl = "Λαμπερός ήλιος όλη μέρα και εξαιρετική ορατότητα στον ορίζοντα.",
                    icon = WeatherIcons.WbSunny,
                    iconTint = Color(0xFFFBBF24),
                    gradientTop = Color(0xFF093B6B),
                    gradientMid = Color(0xFF15659E),
                    gradientBottom = Color(0xFF0A213F),
                    particleEffect = SkyParticleEffect.SUN_RAYS,
                    isNight = false
                )
            } else {
                WeatherVisualSpec(
                    code = code,
                    label = "Clear Night",
                    labelEl = "Ξάστερος Ουρανός",
                    summary = "Starlit night sky with a luminous moon and calm atmospheric conditions.",
                    summaryEl = "Ξάστερη νύχτα με λαμπερό φεγγάρι και ήπιες ατμοσφαιρικές συνθήκες.",
                    icon = WeatherIcons.NightsStay,
                    iconTint = Color(0xFFFDE047),
                    gradientTop = Color(0xFF030712),
                    gradientMid = Color(0xFF0C1D3E),
                    gradientBottom = Color(0xFF040812),
                    particleEffect = SkyParticleEffect.STARFIELD,
                    isNight = true
                )
            }
            1 -> if (isDay) {
                WeatherVisualSpec(
                    code = code,
                    label = "Mostly Sunny",
                    labelEl = "Ηλιοφάνεια",
                    summary = "Predominantly bright sun all day with light drifting clouds.",
                    summaryEl = "Λαμπερή ηλιοφάνεια όλη μέρα με ελάχιστες αραιές νεφώσεις.",
                    icon = WeatherIcons.WbSunny,
                    iconTint = Color(0xFFFBBF24),
                    gradientTop = Color(0xFF0B4578),
                    gradientMid = Color(0xFF19669C),
                    gradientBottom = Color(0xFF0A203D),
                    particleEffect = SkyParticleEffect.SUN_RAYS,
                    isNight = false
                )
            } else {
                WeatherVisualSpec(
                    code = code,
                    label = "Moonlit Night",
                    labelEl = "Φεγγαρόλουστη Νύχτα",
                    summary = "Clear moonlit sky with crisp starlight and soft nocturnal clouds.",
                    summaryEl = "Καθαρή νύχτα με φεγγάρι, αστέρια και απαλά νυχτερινά σύννεφα.",
                    icon = WeatherIcons.NightsStay,
                    iconTint = Color(0xFFFDE047),
                    gradientTop = Color(0xFF040918),
                    gradientMid = Color(0xFF0E2144),
                    gradientBottom = Color(0xFF040812),
                    particleEffect = SkyParticleEffect.STARFIELD,
                    isNight = true
                )
            }
            2 -> if (isDay) {
                WeatherVisualSpec(
                    code = code,
                    label = "Partly Cloudy",
                    labelEl = "Μερικώς Συννεφιασμένος",
                    summary = "Bright golden sun shining behind drifting white clouds.",
                    summaryEl = "Ήλιος με λευκά σύννεφα και μεγάλα διαστήματα ηλιοφάνειας.",
                    icon = WeatherIcons.PartlySunny,
                    iconTint = Color(0xFF93C5FD),
                    gradientTop = Color(0xFF162C46),
                    gradientMid = Color(0xFF28486B),
                    gradientBottom = Color(0xFF0C1A2E),
                    particleEffect = SkyParticleEffect.CLOUDS,
                    isNight = false
                )
            } else {
                WeatherVisualSpec(
                    code = code,
                    label = "Moon & Clouds",
                    labelEl = "Φεγγάρι με Σύννεφα",
                    summary = "Luminous crescent moon shining through drifting moonlit night clouds.",
                    summaryEl = "Φεγγάρι με φωτισμένα νυχτερινά σύννεφα στον ουρανό.",
                    icon = WeatherIcons.NightsStay,
                    iconTint = Color(0xFFFDE047),
                    gradientTop = Color(0xFF040A1A),
                    gradientMid = Color(0xFF102244),
                    gradientBottom = Color(0xFF040914),
                    particleEffect = SkyParticleEffect.STARFIELD,
                    isNight = true
                )
            }
            3 -> if (isDay) {
                WeatherVisualSpec(
                    code = code,
                    label = "Cloudy Sky",
                    labelEl = "Συννεφιά",
                    summary = "White clouds drifting across the sky with intervals of sun.",
                    summaryEl = "Λευκά σύννεφα στον ορίζοντα με ανοίγματα ηλιοφάνειας.",
                    icon = WeatherIcons.PartlySunny,
                    iconTint = Color(0xFFCBD5E1),
                    gradientTop = Color(0xFF1C2D42),
                    gradientMid = Color(0xFF30465E),
                    gradientBottom = Color(0xFF0E1929),
                    particleEffect = SkyParticleEffect.CLOUDS,
                    isNight = false
                )
            } else {
                WeatherVisualSpec(
                    code = code,
                    label = "Cloudy Night",
                    labelEl = "Νυχτερινή Συννεφιά",
                    summary = "Deep night sky with moonlight filtering through dense drifting clouds.",
                    summaryEl = "Νυχτερινός ουρανός με φεγγάρι και πυκνά κινούμενα σύννεφα.",
                    icon = WeatherIcons.NightsStay,
                    iconTint = Color(0xFFFDE047),
                    gradientTop = Color(0xFF050C1D),
                    gradientMid = Color(0xFF122340),
                    gradientBottom = Color(0xFF040812),
                    particleEffect = SkyParticleEffect.STARFIELD,
                    isNight = true
                )
            }
            45, 48 -> WeatherVisualSpec(
                code = code,
                label = "Foggy & Mist",
                labelEl = "Ομίχλη & Πάχνη",
                summary = "Low horizontal mist ribbons and atmospheric fog reducing distant visibility.",
                summaryEl = "Πέπλο ομίχλης και υδρατμοί στην ατμόσφαιρα με περιορισμένη ορατότητα.",
                icon = WeatherIcons.Foggy,
                iconTint = Color(0xFFBAE6FD),
                gradientTop = if (isDay) Color(0xFF1B2D3E) else Color(0xFF060E1C),
                gradientMid = if (isDay) Color(0xFF334A5E) else Color(0xFF112036),
                gradientBottom = if (isDay) Color(0xFF0D1826) else Color(0xFF050B16),
                particleEffect = SkyParticleEffect.FOG,
                isNight = nightMode
            )
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> WeatherVisualSpec(
                code = code,
                label = when (code) {
                    in 51..55 -> "Light Rain"
                    61, 80 -> "Clouds & Rain"
                    63, 81 -> "Steady Rain"
                    else -> "Heavy Rain"
                },
                labelEl = when (code) {
                    in 51..55 -> "Σύννεφα με Ψιχάλες"
                    61, 80 -> "Σύννεφα με Βροχή"
                    63, 81 -> "Βροχόπτωση"
                    else -> "Ισχυρή Βροχή"
                },
                summary = if (isDay) {
                    "White rain clouds with active showers; carry an umbrella."
                } else {
                    "Moonlit night rain clouds with active nocturnal showers; carry an umbrella."
                },
                summaryEl = if (isDay) {
                    "Σύννεφα με βροχή στην περιοχή· κρατήστε ομπρέλα."
                } else {
                    "Νυχτερινή βροχή με σύννεφα στην περιοχή· κρατήστε ομπρέλα."
                },
                icon = WeatherIcons.Grain,
                iconTint = Color(0xFF38BDF8),
                gradientTop = if (isDay) Color(0xFF09203A) else Color(0xFF020714),
                gradientMid = if (isDay) Color(0xFF133860) else Color(0xFF091B36),
                gradientBottom = if (isDay) Color(0xFF051120) else Color(0xFF030812),
                particleEffect = SkyParticleEffect.RAIN,
                isNight = nightMode
            )
            56, 66, 71, 73, 77, 85 -> WeatherVisualSpec(
                code = code,
                label = when (code) {
                    56, 66 -> "Freezing Sleet & Snow"
                    71 -> "Light Snow Clouds"
                    77 -> "Snow Crystals"
                    85 -> "Snow Showers"
                    else -> "Snow Clouds & Snowfall"
                },
                labelEl = when (code) {
                    56, 66 -> "Χιονόνερο & Παγετός"
                    71 -> "Ασθενής Χιονόπτωση"
                    77 -> "Νιφάδες Χιονιού"
                    85 -> "Μπόρες Χιονιού"
                    else -> "Σύννεφα Χιονιού & Χιονόπτωση"
                },
                summary = "Winter snow clouds blankets the mountains and towns with crisp crystalline snowflakes.",
                summaryEl = "Χειμερινά σύννεφα χιονιού με κρυστάλλινες νιφάδες και χιονόπτωση στην περιοχή.",
                icon = WeatherIcons.SnowCloud,
                iconTint = Color(0xFF7DD3FC),
                gradientTop = if (isDay) Color(0xFF122C4A) else Color(0xFF040C1C),
                gradientMid = if (isDay) Color(0xFF214A75) else Color(0xFF0E2240),
                gradientBottom = if (isDay) Color(0xFF0A192E) else Color(0xFF040A16),
                particleEffect = SkyParticleEffect.SNOW,
                isNight = nightMode
            )
            57, 67, 75, 86 -> WeatherVisualSpec(
                code = code,
                label = when (code) {
                    57, 67 -> "Freezing Ice & Snow Storm"
                    86 -> "Heavy Blizzard Squalls"
                    else -> "Heavy Snow Storm"
                },
                labelEl = when (code) {
                    57, 67 -> "Παγωμένη Χιονοθύελλα"
                    86 -> "Ισχυρή Χιονοκαταιγίδα"
                    else -> "Ισχυρή Χιονοθύελλα"
                },
                summary = "Intense winter snow storm with dense snowfall, freezing gusts, and rapid snow accumulation.",
                summaryEl = "Ισχυρή χιονοθύελλα με πυκνή χιονόπτωση, παγωμένες ριπές ανέμου και χιονόστρωση.",
                icon = WeatherIcons.SnowStorm,
                iconTint = Color(0xFFBAE6FD),
                gradientTop = if (isDay) Color(0xFF0E223B) else Color(0xFF030916),
                gradientMid = if (isDay) Color(0xFF1A3A5C) else Color(0xFF0B1C36),
                gradientBottom = if (isDay) Color(0xFF071222) else Color(0xFF030812),
                particleEffect = SkyParticleEffect.SNOW_STORM,
                isNight = nightMode
            )
            95, 96, 99 -> WeatherVisualSpec(
                code = code,
                label = "Thunderstorm",
                labelEl = "Καταιγίδα",
                summary = "Storm clouds with lightning bolts and heavy rain gusts.",
                summaryEl = "Καταιγίδα με κεραυνούς, βροχή και ισχυρές ριπές ανέμου.",
                icon = WeatherIcons.Thunderstorm,
                iconTint = Color(0xFFFBBF24),
                gradientTop = if (isDay) Color(0xFF1A1645) else Color(0xFF040614),
                gradientMid = if (isDay) Color(0xFF262359) else Color(0xFF0E1334),
                gradientBottom = Color(0xFF050814),
                particleEffect = SkyParticleEffect.THUNDER,
                isNight = nightMode
            )
            else -> WeatherVisualSpec(
                code = code,
                label = if (isDay) "Sun & Clouds" else "Moon & Clouds",
                labelEl = if (isDay) "Ήλιος με Σύννεφα" else "Φεγγάρι με Σύννεφα",
                summary = "Mixed sky conditions across the local region.",
                summaryEl = "Εναλλαγές καιρού στην ευρύτερη περιοχή.",
                icon = if (isDay) WeatherIcons.PartlySunny else WeatherIcons.NightsStay,
                iconTint = if (isDay) Color.White else Color(0xFFFDE047),
                gradientTop = if (isDay) Color(0xFF0D2640) else Color(0xFF050C1C),
                gradientMid = if (isDay) Color(0xFF183656) else Color(0xFF112344),
                gradientBottom = Color(0xFF09172A),
                particleEffect = if (isDay) SkyParticleEffect.CLOUDS else SkyParticleEffect.STARFIELD,
                isNight = nightMode
            )
        }
    }
}

enum class HourlyChartMode(val title: String, val titleEl: String) {
    TEMPERATURE("Temp & Feel", "Θερμοκρασία"),
    PRECIPITATION("Rain %", "Βροχή %"),
    WIND_UV("Wind & UV", "Άνεμος & UV");

    fun localizedTitle(lang: AppLanguage): String =
        if (lang == AppLanguage.GREEK) titleEl else title
}

data class WeatherUnits(
    val useFahrenheit: Boolean = false,
    val useMph: Boolean = false,
    val useInHg: Boolean = false
) {
    fun formatTemp(celsius: Double, includeUnit: Boolean = true): String {
        val value = if (useFahrenheit) (celsius * 9.0 / 5.0 + 32.0).roundToInt() else celsius.roundToInt()
        return if (includeUnit) "$value°${if (useFahrenheit) "F" else "C"}" else "$value°"
    }

    fun formatDeltaTemp(deltaCelsius: Double): String {
        val value = if (useFahrenheit) (deltaCelsius * 9.0 / 5.0).roundToInt() else deltaCelsius.roundToInt()
        val sign = if (value > 0) "+" else ""
        return "$sign$value°${if (useFahrenheit) "F" else "C"}"
    }

    fun formatWind(kmh: Double): String {
        return if (useMph) {
            "${(kmh * 0.621371).roundToInt()} mph"
        } else {
            "${kmh.roundToInt()} km/h"
        }
    }

    fun formatPressure(hPa: Double): String {
        return if (useInHg) {
            String.format(Locale.US, "%.2f inHg", hPa * 0.02953)
        } else {
            "${hPa.roundToInt()} hPa"
        }
    }
}

data class HourlyForecastItem(
    val isoTime: String,
    val displayHour: String,
    val tempC: Double,
    val feelsLikeC: Double,
    val precipProbability: Int,
    val precipMm: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val windSpeedKmh: Double,
    val uvIndex: Double,
    val humidity: Int,
    val windGustsKmh: Double = windSpeedKmh * 1.35,
    val windDirectionDeg: Int = 240,
    val cloudCoverPercent: Int = 50
)

data class DailyForecastItem(
    val dateIso: String,
    val dayLabel: String,
    val fullDateLabel: String,
    val weatherCode: Int,
    val maxTempC: Double,
    val minTempC: Double,
    val apparentMaxC: Double,
    val apparentMinC: Double,
    val maxPrecipProb: Int,
    val precipSumMm: Double,
    val sunriseTime: String,
    val sunsetTime: String,
    val daylightHours: Double,
    val uvIndexMax: Double,
    val windMaxKmh: Double,
    val hourly: List<HourlyForecastItem> = emptyList()
)

data class AirQualityTelemetry(
    val usAqi: Int,
    val europeanAqi: Int,
    val pm25: Double,
    val pm10: Double,
    val ozone: Double,
    val nitrogenDioxide: Double,
    val carbonMonoxide: Double
) {
    val categoryLabel: String
        get() = when {
            usAqi <= 50 -> "Good"
            usAqi <= 100 -> "Moderate"
            usAqi <= 150 -> "Sensitive"
            usAqi <= 200 -> "Unhealthy"
            else -> "Hazardous"
        }

    fun localizedCategory(lang: AppLanguage): String =
        if (lang == AppLanguage.GREEK) {
            when {
                usAqi <= 50 -> "Καλή"
                usAqi <= 100 -> "Μέτρια"
                usAqi <= 150 -> "Ευαίσθητη"
                usAqi <= 200 -> "Ανθυγιεινή"
                else -> "Επικίνδυνη"
            }
        } else categoryLabel

    val healthAdvice: String
        get() = when {
            usAqi <= 50 -> "Crisp, clean air quality. Ideal for endurance sports and open windows."
            usAqi <= 100 -> "Acceptable air quality; unusually sensitive individuals should pace intense outdoor workouts."
            usAqi <= 150 -> "Sensitive groups should reduce prolonged heavy exertion outdoors."
            else -> "Elevated particulates detected. Prefer indoor training and filtration today."
        }

    val badgeColor: Color
        get() = when {
            usAqi <= 50 -> Color(0xFF34D399)
            usAqi <= 100 -> Color(0xFFFBBF24)
            usAqi <= 150 -> Color(0xFFFB923C)
            else -> Color(0xFFFB7185)
        }
}

data class CurrentWeatherTelemetry(
    val tempC: Double,
    val feelsLikeC: Double,
    val highC: Double,
    val lowC: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val humidity: Int,
    val dewPointC: Double,
    val windSpeedKmh: Double,
    val windGustsKmh: Double,
    val windDirectionDeg: Int,
    val pressureHpa: Double,
    val visibilityKm: Double,
    val cloudCoverPercent: Int,
    val uvIndex: Double,
    val precipitationMm: Double
) {
    val windCardinal: String
        get() {
            val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            val index = (((windDirectionDeg % 360) + 22.5) / 45.0).toInt() % 8
            return dirs[index]
        }

    val beaufortLabel: String
        get() = when {
            windSpeedKmh < 2 -> "Calm (Force 0)"
            windSpeedKmh < 12 -> "Light Breeze (Force 2)"
            windSpeedKmh < 29 -> "Moderate Breeze (Force 4)"
            windSpeedKmh < 50 -> "Fresh Breeze (Force 6)"
            else -> "Strong Gale (Force 8+)"
        }
}

data class ActivitySuitability(
    val id: String,
    val name: String,
    val score: Int,
    val statusLabel: String,
    val recommendation: String,
    val bestTimeWindow: String,
    val accentColor: Color
)

data class RainTimingOutlook(
    val hasRainInNext48h: Boolean,
    val nextRainOnsetLabel: String,
    val peakRainWindowLabel: String,
    val totalRainNext7DaysMm: Double,
    val peakPrecipProbability: Int,
    val rainRiskLevel: String,
    val rainAdvice: String,
    val hourlyRainTimeline: List<HourlyRainPoint>
)

data class HourlyRainPoint(
    val hourLabel: String,
    val probabilityPercent: Int,
    val precipMm: Double,
    val tempC: Double,
    val windKmh: Double,
    val isRaining: Boolean
)

data class SmartWeatherInsight(
    val headline: String,
    val detail: String,
    val tag: String
)

data class CityWeatherBundle(
    val cityId: Long,
    val cityName: String,
    val regionName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double,
    val timezoneId: String,
    val currentLocalTimeLabel: String,
    val sunProgressFraction: Float,
    val current: CurrentWeatherTelemetry,
    val hourly: List<HourlyForecastItem>,
    val daily: List<DailyForecastItem>,
    val airQuality: AirQualityTelemetry,
    val rainOutlook: RainTimingOutlook,
    val insights: List<SmartWeatherInsight>,
    val activities: List<ActivitySuitability>,
    val bestOutdoorWindow: String,
    val fetchedAtEpochMs: Long,
    val isLiveNetwork: Boolean,
    val provider: WeatherProvider = WeatherProvider.BEST_MATCH
)

fun calculateDewPointCelsius(tempC: Double, humidityPercent: Int): Double {
    val rh = humidityPercent.coerceIn(1, 100) / 100.0
    val a = 17.27
    val b = 237.7
    val alpha = ((a * tempC) / (b + tempC)) + ln(rh)
    return (b * alpha) / (a - alpha)
}
