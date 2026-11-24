package com.mohamedzayed.nocturne.preview

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.mohamedzayed.nocturne.R
import com.mohamedzayed.nocturne.model.DisplayMode
import java.util.Calendar

val lunarMoonResIds = intArrayOf(
    R.drawable.lunar_moon_00, R.drawable.lunar_moon_01, R.drawable.lunar_moon_02, R.drawable.lunar_moon_03,
    R.drawable.lunar_moon_04, R.drawable.lunar_moon_05, R.drawable.lunar_moon_06, R.drawable.lunar_moon_07,
    R.drawable.lunar_moon_08, R.drawable.lunar_moon_09, R.drawable.lunar_moon_10, R.drawable.lunar_moon_11,
    R.drawable.lunar_moon_12, R.drawable.lunar_moon_13, R.drawable.lunar_moon_14, R.drawable.lunar_moon_15,
    R.drawable.lunar_moon_16, R.drawable.lunar_moon_17, R.drawable.lunar_moon_18, R.drawable.lunar_moon_19,
    R.drawable.lunar_moon_20, R.drawable.lunar_moon_21, R.drawable.lunar_moon_22, R.drawable.lunar_moon_23,
    R.drawable.lunar_moon_24, R.drawable.lunar_moon_25, R.drawable.lunar_moon_26, R.drawable.lunar_moon_27,
    R.drawable.lunar_moon_28
)

val refinedMoonResIds = intArrayOf(
    R.drawable.refined_moon_00, R.drawable.refined_moon_01, R.drawable.refined_moon_02, R.drawable.refined_moon_03,
    R.drawable.refined_moon_04, R.drawable.refined_moon_05, R.drawable.refined_moon_06, R.drawable.refined_moon_07,
    R.drawable.refined_moon_08, R.drawable.refined_moon_09, R.drawable.refined_moon_10, R.drawable.refined_moon_11,
    R.drawable.refined_moon_12, R.drawable.refined_moon_13, R.drawable.refined_moon_14, R.drawable.refined_moon_15,
    R.drawable.refined_moon_16, R.drawable.refined_moon_17, R.drawable.refined_moon_18, R.drawable.refined_moon_19,
    R.drawable.refined_moon_20, R.drawable.refined_moon_21, R.drawable.refined_moon_22, R.drawable.refined_moon_23,
    R.drawable.refined_moon_24, R.drawable.refined_moon_25, R.drawable.refined_moon_26, R.drawable.refined_moon_27,
    R.drawable.refined_moon_28
)

val dhikrResIds = intArrayOf(
    R.drawable.uc_dhikr_0, R.drawable.uc_dhikr_1, R.drawable.uc_dhikr_2, R.drawable.uc_dhikr_3
)

@Composable
fun WatchFacePreviewCanvas(
    faceId: String,
    displayMode: DisplayMode,
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xFF101317),
    accentColor: Color = Color(0xFFC6AC79),
    ornamentType: String = "balanced",
    dialType: String = "full",
    moonPhaseIndex: Int = 14,
    currentTime: Calendar = Calendar.getInstance()
) {
    val context = LocalContext.current
    val bitmapCache = remember { mutableMapOf<Int, ImageBitmap>() }

    fun getBmp(resId: Int): ImageBitmap {
        return bitmapCache.getOrPut(resId) {
            BitmapFactory.decodeResource(context.resources, resId).asImageBitmap()
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val scale = size.width / 1000f

        when (faceId) {
            "lunar_phase" -> {
                drawLunarPhaseFace(
                    displayMode = displayMode,
                    scale = scale,
                    moonPhaseIndex = moonPhaseIndex,
                    currentTime = currentTime,
                    getBmp = ::getBmp
                )
            }
            "obsidian_atelier" -> {
                drawObsidianAtelierFace(
                    displayMode = displayMode,
                    scale = scale,
                    currentTime = currentTime,
                    getBmp = ::getBmp
                )
            }
            else -> {
                // sage_nocturne default
                drawSageNocturneFace(
                    displayMode = displayMode,
                    scale = scale,
                    bgColor = bgColor,
                    accentColor = accentColor,
                    ornamentType = ornamentType,
                    dialType = dialType,
                    moonPhaseIndex = moonPhaseIndex,
                    currentTime = currentTime,
                    getBmp = ::getBmp
                )
            }
        }
    }
}

fun DrawScope.drawSageNocturneFace(
    displayMode: DisplayMode,
    scale: Float,
    bgColor: Color,
    accentColor: Color,
    ornamentType: String,
    dialType: String,
    moonPhaseIndex: Int,
    currentTime: Calendar,
    getBmp: (Int) -> ImageBitmap
) {
    val isAod = displayMode == DisplayMode.AOD

    // 1. Background
    val dialBg = if (isAod) Color(0xFF000000) else bgColor
    drawCircle(color = dialBg, radius = 500f * scale, center = Offset(500f * scale, 500f * scale))

    // 2. Ottoman Arabesque on Left Half (Active mode only)
    if (!isAod && ornamentType != "none") {
        val ornRes = when (ornamentType) {
            "minimal" -> R.drawable.visible_0_orn_0
            "ornate" -> R.drawable.visible_0_orn_2
            else -> R.drawable.visible_0_orn_1
        }
        val ornBmp = getBmp(ornRes)
        drawImage(
            image = ornBmp,
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize((1000 * scale).toInt(), (1000 * scale).toInt())
        )
    }

    // 3. Dial Numerals and Minute Track
    val dialRes = if (isAod) {
        R.drawable.refined_0_dial_0_aod
    } else {
        when (dialType) {
            "cardinal" -> R.drawable.refined_0_dial_1
            "markers" -> R.drawable.refined_0_dial_2
            else -> R.drawable.refined_0_dial_0
        }
    }
    val dialBmp = getBmp(dialRes)
    drawImage(
        image = dialBmp,
        dstOffset = IntOffset(0, 0),
        dstSize = IntSize((1000 * scale).toInt(), (1000 * scale).toInt())
    )

    // 4. Subdial Rings
    val ringBmp = getBmp(R.drawable.uc_0_ring)
    val ringAlpha = if (isAod) 0.5f else 1.0f

    // Left Ring (Battery)
    drawImage(
        image = ringBmp,
        dstOffset = IntOffset((171 * scale).toInt(), (391 * scale).toInt()),
        dstSize = IntSize((228 * scale).toInt(), (228 * scale).toInt()),
        alpha = ringAlpha
    )

    // Right Ring (Calendar)
    drawImage(
        image = ringBmp,
        dstOffset = IntOffset((601 * scale).toInt(), (391 * scale).toInt()),
        dstSize = IntSize((228 * scale).toInt(), (228 * scale).toInt()),
        alpha = ringAlpha
    )

    // Subdial Complications Content
    if (!isAod) {
        val batBmp = getBmp(R.drawable.refined_battery)
        val batW = (58 * scale).toInt()
        val batH = (40 * scale).toInt()
        val batX = (285 * scale - batW / 2).toInt()
        val batY = (425 * scale).toInt()
        drawImage(
            image = batBmp,
            dstOffset = IntOffset(batX, batY),
            dstSize = IntSize(batW, batH)
        )

        val calBmp = getBmp(R.drawable.sage_calendar)
        val calW = (50 * scale).toInt()
        val calH = (46 * scale).toInt()
        val calX = (715 * scale - calW / 2).toInt()
        val calY = (422 * scale).toInt()
        drawImage(
            image = calBmp,
            dstOffset = IntOffset(calX, calY),
            dstSize = IntSize(calW, calH)
        )

        val stepBmp = getBmp(R.drawable.sage_steps)
        val stepW = (48 * scale).toInt()
        val stepH = (36 * scale).toInt()
        val stepX = (500 * scale - stepW / 2).toInt()
        val stepY = (622 * scale).toInt()
        drawImage(
            image = stepBmp,
            dstOffset = IntOffset(stepX, stepY),
            dstSize = IntSize(stepW, stepH)
        )

        drawContext.canvas.nativeCanvas.apply {
            val numPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#FFF0D8")
                textAlign = Paint.Align.CENTER
                textSize = 50f * scale
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }
            val monthPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#FFF0D8")
                textAlign = Paint.Align.CENTER
                textSize = 38f * scale
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }

            drawText("45%", 285f * scale, 532f * scale, numPaint)
            drawText("Nov", 715f * scale, 502f * scale, monthPaint)
            drawText("21", 715f * scale, 555f * scale, numPaint)
            drawText("0", 500f * scale, 725f * scale, numPaint)
        }
    }

    // 5. Dhikr Calligraphy
    val dhikrIndex = (currentTime.get(Calendar.MINUTE) / 15) % 4
    val dhikrBmp = getBmp(dhikrResIds[dhikrIndex])
    drawImage(
        image = dhikrBmp,
        dstOffset = IntOffset((300 * scale).toInt(), (335 * scale).toInt()),
        dstSize = IntSize((400 * scale).toInt(), (80 * scale).toInt()),
        alpha = if (isAod) 0.65f else 0.95f
    )

    // 6. Photorealistic Small Moon at Top
    if (!isAod) {
        val safeMoonIndex = moonPhaseIndex.coerceIn(0, 28)
        val moonBmp = getBmp(refinedMoonResIds[safeMoonIndex])
        drawImage(
            image = moonBmp,
            dstOffset = IntOffset((392 * scale).toInt(), (122 * scale).toInt()),
            dstSize = IntSize((216 * scale).toInt(), (216 * scale).toInt())
        )
    }

    // 7. Hands
    val hours = currentTime.get(Calendar.HOUR_OF_DAY) % 12
    val minutes = currentTime.get(Calendar.MINUTE)
    val seconds = currentTime.get(Calendar.SECOND)

    val hourDeg = hours * 30f + minutes * 0.5f
    val minDeg = minutes * 6f + seconds * 0.1f
    val secDeg = seconds * 6f

    val centerDial = Offset(500f * scale, 500f * scale)

    val hourRes = if (isAod) R.drawable.refined_0_hour_aod else R.drawable.refined_0_hour
    val hourBmp = getBmp(hourRes)
    withTransform({
        rotate(degrees = hourDeg, pivot = centerDial)
    }) {
        drawImage(
            image = hourBmp,
            dstOffset = IntOffset((480 * scale).toInt(), (250 * scale).toInt()),
            dstSize = IntSize((40 * scale).toInt(), (280 * scale).toInt())
        )
    }

    val minRes = if (isAod) R.drawable.refined_0_minute_aod else R.drawable.refined_0_minute
    val minBmp = getBmp(minRes)
    withTransform({
        rotate(degrees = minDeg, pivot = centerDial)
    }) {
        drawImage(
            image = minBmp,
            dstOffset = IntOffset((483 * scale).toInt(), (150 * scale).toInt()),
            dstSize = IntSize((34 * scale).toInt(), (380 * scale).toInt())
        )
    }

    if (!isAod) {
        val secBmp = getBmp(R.drawable.uc_0_second)
        withTransform({
            rotate(degrees = secDeg, pivot = centerDial)
        }) {
            drawImage(
                image = secBmp,
                dstOffset = IntOffset((492 * scale).toInt(), (140 * scale).toInt()),
                dstSize = IntSize((16 * scale).toInt(), (395 * scale).toInt())
            )
        }
    }

    val hubBmp = getBmp(R.drawable.uc_0_hub)
    drawImage(
        image = hubBmp,
        dstOffset = IntOffset((477 * scale).toInt(), (477 * scale).toInt()),
        dstSize = IntSize((46 * scale).toInt(), (46 * scale).toInt())
    )
}

fun DrawScope.drawLunarPhaseFace(
    displayMode: DisplayMode,
    scale: Float,
    moonPhaseIndex: Int,
    currentTime: Calendar,
    getBmp: (Int) -> ImageBitmap
) {
    val isAod = displayMode == DisplayMode.AOD
    val centerDial = Offset(500f * scale, 500f * scale)

    drawCircle(color = Color(0xFF000000), radius = 500f * scale, center = centerDial)

    if (!isAod) {
        val safeMoonIndex = moonPhaseIndex.coerceIn(0, 28)
        val moonBmp = getBmp(lunarMoonResIds[safeMoonIndex])
        drawImage(
            image = moonBmp,
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize((1000 * scale).toInt(), (1000 * scale).toInt())
        )

        val markersBmp = getBmp(R.drawable.lunar_markers)
        drawImage(
            image = markersBmp,
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize((1000 * scale).toInt(), (1000 * scale).toInt())
        )

        val hours = currentTime.get(Calendar.HOUR_OF_DAY) % 12
        val minutes = currentTime.get(Calendar.MINUTE)
        val seconds = currentTime.get(Calendar.SECOND)

        val hourDeg = hours * 30f + minutes * 0.5f
        val minDeg = minutes * 6f + seconds * 0.1f
        val secDeg = seconds * 6f

        val hourBmp = getBmp(R.drawable.lunar_hour)
        withTransform({
            rotate(degrees = hourDeg, pivot = centerDial)
        }) {
            drawImage(
                image = hourBmp,
                dstOffset = IntOffset((476 * scale).toInt(), (225 * scale).toInt()),
                dstSize = IntSize((48 * scale).toInt(), (320 * scale).toInt())
            )
        }

        val minBmp = getBmp(R.drawable.lunar_minute)
        withTransform({
            rotate(degrees = minDeg, pivot = centerDial)
        }) {
            drawImage(
                image = minBmp,
                dstOffset = IntOffset((479 * scale).toInt(), (85 * scale).toInt()),
                dstSize = IntSize((42 * scale).toInt(), (460 * scale).toInt())
            )
        }

        val secBmp = getBmp(R.drawable.lunar_second)
        withTransform({
            rotate(degrees = secDeg, pivot = centerDial)
        }) {
            drawImage(
                image = secBmp,
                dstOffset = IntOffset((490 * scale).toInt(), (65 * scale).toInt()),
                dstSize = IntSize((20 * scale).toInt(), (520 * scale).toInt())
            )
        }

        val hubBmp = getBmp(R.drawable.lunar_hub)
        drawImage(
            image = hubBmp,
            dstOffset = IntOffset((477 * scale).toInt(), (477 * scale).toInt()),
            dstSize = IntSize((46 * scale).toInt(), (46 * scale).toInt())
        )
    } else {
        val dialBmp = getBmp(R.drawable.lunar_dial_aod)
        drawImage(
            image = dialBmp,
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize((1000 * scale).toInt(), (1000 * scale).toInt())
        )

        val hours = currentTime.get(Calendar.HOUR_OF_DAY) % 12
        val minutes = currentTime.get(Calendar.MINUTE)
        val hourDeg = hours * 30f + minutes * 0.5f
        val minDeg = minutes * 6f

        val hourBmp = getBmp(R.drawable.lunar_hour_aod)
        withTransform({
            rotate(degrees = hourDeg, pivot = centerDial)
        }) {
            drawImage(
                image = hourBmp,
                dstOffset = IntOffset((476 * scale).toInt(), (225 * scale).toInt()),
                dstSize = IntSize((48 * scale).toInt(), (320 * scale).toInt())
            )
        }

        val minBmp = getBmp(R.drawable.lunar_minute_aod)
        withTransform({
            rotate(degrees = minDeg, pivot = centerDial)
        }) {
            drawImage(
                image = minBmp,
                dstOffset = IntOffset((479 * scale).toInt(), (85 * scale).toInt()),
                dstSize = IntSize((42 * scale).toInt(), (460 * scale).toInt())
            )
        }

        val hubBmp = getBmp(R.drawable.lunar_hub)
        drawImage(
            image = hubBmp,
            dstOffset = IntOffset((477 * scale).toInt(), (477 * scale).toInt()),
            dstSize = IntSize((46 * scale).toInt(), (46 * scale).toInt())
        )
    }
}

fun DrawScope.drawObsidianAtelierFace(
    displayMode: DisplayMode,
    scale: Float,
    currentTime: Calendar,
    getBmp: (Int) -> ImageBitmap
) {
    val isAod = displayMode == DisplayMode.AOD
    val centerDial = Offset(500f * scale, 500f * scale)

    drawCircle(color = Color(0xFF000000), radius = 500f * scale, center = centerDial)

    if (!isAod) {
        val dialBmp = getBmp(R.drawable.obsidian_dial)
        drawImage(
            image = dialBmp,
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize((1000 * scale).toInt(), (1000 * scale).toInt())
        )

        val powerBmp = getBmp(R.drawable.obsidian_power_hand)
        val powerPivot = Offset(215f * scale, 475f * scale)
        val batteryPct = 72f
        val powerAngle = (batteryPct - 50f) * 1.2f
        withTransform({
            rotate(degrees = powerAngle, pivot = powerPivot)
        }) {
            drawImage(
                image = powerBmp,
                dstOffset = IntOffset((207 * scale).toInt(), (375 * scale).toInt()),
                dstSize = IntSize((16 * scale).toInt(), (125 * scale).toInt())
            )
        }

        val seconds = currentTime.get(Calendar.SECOND)
        val secDeg = seconds * 6f
        val secPivot = Offset(500f * scale, 715f * scale)
        val smallSecBmp = getBmp(R.drawable.obsidian_small_sec)
        withTransform({
            rotate(degrees = secDeg, pivot = secPivot)
        }) {
            drawImage(
                image = smallSecBmp,
                dstOffset = IntOffset((492 * scale).toInt(), (595 * scale).toInt()),
                dstSize = IntSize((16 * scale).toInt(), (160 * scale).toInt())
            )
        }

        val hours = currentTime.get(Calendar.HOUR_OF_DAY) % 12
        val minutes = currentTime.get(Calendar.MINUTE)
        val hourDeg = hours * 30f + minutes * 0.5f
        val minDeg = minutes * 6f + seconds * 0.1f

        val hourBmp = getBmp(R.drawable.obsidian_hour)
        withTransform({
            rotate(degrees = hourDeg, pivot = centerDial)
        }) {
            drawImage(
                image = hourBmp,
                dstOffset = IntOffset((476 * scale).toInt(), (214 * scale).toInt()),
                dstSize = IntSize((48 * scale).toInt(), (320 * scale).toInt())
            )
        }

        val minBmp = getBmp(R.drawable.obsidian_minute)
        withTransform({
            rotate(degrees = minDeg, pivot = centerDial)
        }) {
            drawImage(
                image = minBmp,
                dstOffset = IntOffset((480 * scale).toInt(), (95 * scale).toInt()),
                dstSize = IntSize((40 * scale).toInt(), (440 * scale).toInt())
            )
        }

        val hubBmp = getBmp(R.drawable.obsidian_hub)
        drawImage(
            image = hubBmp,
            dstOffset = IntOffset((470 * scale).toInt(), (470 * scale).toInt()),
            dstSize = IntSize((60 * scale).toInt(), (60 * scale).toInt())
        )
    } else {
        val dialBmp = getBmp(R.drawable.obsidian_dial_aod)
        drawImage(
            image = dialBmp,
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize((1000 * scale).toInt(), (1000 * scale).toInt())
        )

        val hours = currentTime.get(Calendar.HOUR_OF_DAY) % 12
        val minutes = currentTime.get(Calendar.MINUTE)
        val hourDeg = hours * 30f + minutes * 0.5f
        val minDeg = minutes * 6f

        val hourBmp = getBmp(R.drawable.obsidian_hour_aod)
        withTransform({
            rotate(degrees = hourDeg, pivot = centerDial)
        }) {
            drawImage(
                image = hourBmp,
                dstOffset = IntOffset((476 * scale).toInt(), (214 * scale).toInt()),
                dstSize = IntSize((48 * scale).toInt(), (320 * scale).toInt())
            )
        }

        val minBmp = getBmp(R.drawable.obsidian_minute_aod)
        withTransform({
            rotate(degrees = minDeg, pivot = centerDial)
        }) {
            drawImage(
                image = minBmp,
                dstOffset = IntOffset((480 * scale).toInt(), (95 * scale).toInt()),
                dstSize = IntSize((40 * scale).toInt(), (440 * scale).toInt())
            )
        }

        val hubBmp = getBmp(R.drawable.obsidian_hub)
        drawImage(
            image = hubBmp,
            dstOffset = IntOffset((470 * scale).toInt(), (470 * scale).toInt()),
            dstSize = IntSize((60 * scale).toInt(), (60 * scale).toInt())
        )
    }
}
