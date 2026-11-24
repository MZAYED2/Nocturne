package com.mohamedzayed.nocturne.preview

import java.util.Date
import kotlin.math.roundToInt

object LunarCalculation {
    fun calculateCurrentMoonPhase(): Int {
        val now = Date().time
        val knownNewMoon = 1704974220000L
        val synodicPeriodMillis = 29.530588853 * 24.0 * 60.0 * 60.0 * 1000.0
        val diff = (now - knownNewMoon) % synodicPeriodMillis
        val positiveDiff = if (diff < 0) diff + synodicPeriodMillis else diff
        val cycleFraction = positiveDiff / synodicPeriodMillis
        return (cycleFraction * 28.0).roundToInt().coerceIn(0, 28)
    }

    fun getMoonPhaseName(phaseIndex: Int, isArabic: Boolean): String {
        return when (phaseIndex) {
            0, 28 -> if (isArabic) "محاق" else "New Moon"
            in 1..6 -> if (isArabic) "هلال متزايد" else "Waxing Crescent"
            7 -> if (isArabic) "تربيع أول" else "First Quarter"
            in 8..13 -> if (isArabic) "أحدب متزايد" else "Waxing Gibbous"
            14 -> if (isArabic) "بدر كامل" else "Full Moon"
            in 15..20 -> if (isArabic) "أحدب متناقص" else "Waning Gibbous"
            21 -> if (isArabic) "تربيع أخير" else "Last Quarter"
            else -> if (isArabic) "هلال متناقص" else "Waning Crescent"
        }
    }
}
