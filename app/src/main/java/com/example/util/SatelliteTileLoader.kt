package com.example.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tan

enum class SatelliteMapStyle(val id: String) {
    HYBRID_SATELLITE("hybrid_satellite"),
    STREET_TERRAIN("street_terrain")
}

object SatelliteTileLoader {
    private const val TILE_SIZE = 256
    private const val OUTPUT_SIZE = 512
    private const val GRID_SPAN = 3 // 3x3 tiles (768x768) cropped to 512x512 around exact town lat/lon

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(7, TimeUnit.SECONDS)
            .readTimeout(7, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val bitmapCache = object : LruCache<String, Bitmap>(32) {}

    data class TileCoordinate(
        val zoom: Int,
        val exactTileX: Double,
        val exactTileY: Double,
        val centerTileX: Int,
        val centerTileY: Int,
        val offsetPxX: Float,
        val offsetPxY: Float
    )

    fun computeWebMercatorTile(lat: Double, lon: Double, zoom: Int): TileCoordinate {
        val safeZoom = zoom.coerceIn(3, 17)
        val n = 2.0.pow(safeZoom.toDouble())
        val clampedLat = lat.coerceIn(-85.0511, 85.0511)
        val latRad = clampedLat * PI / 180.0
        val exactX = ((lon + 180.0) / 360.0) * n
        val exactY = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n
        val tileX = floor(exactX).toInt().coerceIn(0, n.toInt() - 1)
        val tileY = floor(exactY).toInt().coerceIn(0, n.toInt() - 1)
        val fracX = (exactX - tileX).toFloat()
        val fracY = (exactY - tileY).toFloat()
        return TileCoordinate(
            zoom = safeZoom,
            exactTileX = exactX,
            exactTileY = exactY,
            centerTileX = tileX,
            centerTileY = tileY,
            offsetPxX = fracX * TILE_SIZE,
            offsetPxY = fracY * TILE_SIZE
        )
    }

    /**
     * Fetches a 3x3 Web Mercator satellite & street tile mosaic centered on [latitude], [longitude],
     * overlays highways/streets and town/place boundary labels, validates every tile so no missing
     * or duplicated pixel blocks can ever appear, and crops an exact 512x512 viewport so the town's
     * coordinates are dead-center at (256, 256).
     */
    suspend fun fetchTownSatelliteMosaic(
        latitude: Double,
        longitude: Double,
        zoom: Int = 13,
        mapStyle: SatelliteMapStyle = SatelliteMapStyle.HYBRID_SATELLITE
    ): Bitmap? = withContext(Dispatchers.IO) {
        val key = String.format(Locale.US, "v4_%s_%.4f_%.4f_%d", mapStyle.id, latitude, longitude, zoom)
        bitmapCache.get(key)?.let { return@withContext it }

        runCatching {
            val coord = computeWebMercatorTile(latitude, longitude, zoom)
            val maxTileIndex = (1 shl coord.zoom) - 1

            data class GridTileResult(
                val gx: Int,
                val gy: Int,
                val baseBitmap: Bitmap?,
                val roadsBitmap: Bitmap?,
                val labelBitmap: Bitmap?
            )

            val results = coroutineScope {
                val jobs = mutableListOf<Deferred<GridTileResult>>()
                for (gy in 0 until GRID_SPAN) {
                    for (gx in 0 until GRID_SPAN) {
                        val tx = (coord.centerTileX + (gx - 1)).coerceIn(0, maxTileIndex)
                        val ty = (coord.centerTileY + (gy - 1)).coerceIn(0, maxTileIndex)
                        jobs.add(
                            async {
                                if (mapStyle == SatelliteMapStyle.STREET_TERRAIN) {
                                    val streetBase = downloadValidTile(
                                        "https://basemaps.cartocdn.com/rastertiles/voyager/${coord.zoom}/$tx/$ty.png"
                                    ) ?: downloadValidTile(
                                        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/${coord.zoom}/$ty/$tx"
                                    )
                                    GridTileResult(
                                        gx = gx,
                                        gy = gy,
                                        baseBitmap = streetBase,
                                        roadsBitmap = null,
                                        labelBitmap = null
                                    )
                                } else {
                                    val satBase = downloadValidTile(
                                        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/${coord.zoom}/$ty/$tx"
                                    ) ?: downloadValidTile(
                                        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/${coord.zoom}/$ty/$tx"
                                    )
                                    val roads = if (satBase != null) {
                                        downloadTile(
                                            "https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile/${coord.zoom}/$ty/$tx"
                                        )
                                    } else {
                                        null
                                    }
                                    val label = if (satBase != null) {
                                        downloadTile(
                                            "https://services.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/${coord.zoom}/$ty/$tx"
                                        )
                                    } else {
                                        null
                                    }
                                    GridTileResult(
                                        gx = gx,
                                        gy = gy,
                                        baseBitmap = satBase,
                                        roadsBitmap = roads,
                                        labelBitmap = label
                                    )
                                }
                            }
                        )
                    }
                }
                jobs.map { it.await() }
            }

            // Require ALL 9 tiles in the 3x3 grid to succeed so we NEVER stamp a duplicate tile or blank square
            if (results.any { it.baseBitmap == null }) {
                return@runCatching null
            }

            val fullGridSize = TILE_SIZE * GRID_SPAN // 768x768
            val fullGridBitmap = createBitmap(fullGridSize, fullGridSize, Bitmap.Config.ARGB_8888)
            val fullCanvas = Canvas(fullGridBitmap)
            val basePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = 225
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = 255
            }

            fullCanvas.drawColor(0xFF0A1E36.toInt())

            for (tile in results) {
                val left = tile.gx * TILE_SIZE.toFloat()
                val top = tile.gy * TILE_SIZE.toFloat()
                val bmp = tile.baseBitmap ?: continue
                fullCanvas.drawBitmap(bmp, left, top, basePaint)
                if (tile.roadsBitmap != null) {
                    fullCanvas.drawBitmap(tile.roadsBitmap, left, top, roadPaint)
                }
                if (tile.labelBitmap != null) {
                    fullCanvas.drawBitmap(tile.labelBitmap, left, top, labelPaint)
                }
            }

            // Exact pixel position of (latitude, longitude) inside the 768x768 3x3 grid
            val exactCenterX = TILE_SIZE + coord.offsetPxX
            val exactCenterY = TILE_SIZE + coord.offsetPxY
            val cropLeft = (exactCenterX - OUTPUT_SIZE / 2f).roundToInt()
                .coerceIn(0, fullGridSize - OUTPUT_SIZE)
            val cropTop = (exactCenterY - OUTPUT_SIZE / 2f).roundToInt()
                .coerceIn(0, fullGridSize - OUTPUT_SIZE)

            val cropped = createBitmap(OUTPUT_SIZE, OUTPUT_SIZE, Bitmap.Config.ARGB_8888)
            val cropCanvas = Canvas(cropped)
            val srcRect = Rect(cropLeft, cropTop, cropLeft + OUTPUT_SIZE, cropTop + OUTPUT_SIZE)
            val dstRect = Rect(0, 0, OUTPUT_SIZE, OUTPUT_SIZE)
            cropCanvas.drawBitmap(fullGridBitmap, srcRect, dstRect, basePaint)

            bitmapCache.put(key, cropped)
            cropped
        }.getOrNull()
    }

    /**
     * Checks that a downloaded tile bitmap is valid and not a uniform grey "Map data not yet available" placeholder.
     */
    private fun isValidTileBitmap(bmp: Bitmap?): Boolean {
        if (bmp == null || bmp.width < 128 || bmp.height < 128) return false
        // Sample 5 points across the tile to reject solid uniform placeholder error tiles
        val samplePoints = listOf(
            32 to 32,
            224 to 32,
            128 to 128,
            32 to 224,
            224 to 224
        )
        val firstPixel = bmp[32, 32]
        val r0 = Color.red(firstPixel)
        val g0 = Color.green(firstPixel)
        val b0 = Color.blue(firstPixel)
        var maxDiff = 0
        for ((sx, sy) in samplePoints) {
            val px = bmp[sx.coerceAtMost(bmp.width - 1), sy.coerceAtMost(bmp.height - 1)]
            val diff = abs(Color.red(px) - r0) + abs(Color.green(px) - g0) + abs(Color.blue(px) - b0)
            if (diff > maxDiff) maxDiff = diff
        }
        // Esri "No Map Data" placeholder tiles are uniform dull grey (R≈G≈B≈204 with near-zero variance)
        val isUniformGreyPlaceholder = maxDiff < 3 && abs(r0 - g0) <= 2 && abs(g0 - b0) <= 2 && r0 in 180..245
        return !isUniformGreyPlaceholder
    }

    private fun downloadValidTile(url: String): Bitmap? {
        val firstTry = downloadTile(url)
        if (isValidTileBitmap(firstTry)) return firstTry
        val retry = downloadTile(url)
        return if (isValidTileBitmap(retry)) retry else null
    }

    private fun downloadTile(url: String): Bitmap? {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ZephyrWeatherAndroid/2.0")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val bytes = response.body?.bytes() ?: return@use null
                if (bytes.isEmpty()) return@use null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }.getOrNull()
    }
}

@Composable
fun rememberTownSatelliteImageBitmap(
    latitude: Double,
    longitude: Double,
    zoom: Int = 13,
    mapStyle: SatelliteMapStyle = SatelliteMapStyle.HYBRID_SATELLITE
): ImageBitmap? {
    var imageBitmap by remember(latitude, longitude, zoom, mapStyle) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(latitude, longitude, zoom, mapStyle) {
        val fetched = SatelliteTileLoader.fetchTownSatelliteMosaic(
            latitude = latitude,
            longitude = longitude,
            zoom = zoom,
            mapStyle = mapStyle
        )
        if (fetched != null) {
            imageBitmap = fetched.asImageBitmap()
        }
    }

    return imageBitmap
}
