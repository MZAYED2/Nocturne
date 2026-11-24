package com.mohamedzayed.nocturne.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

class RotationPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("nocturne_rotation_prefs", Context.MODE_PRIVATE)

    var enabled by mutableStateOf(prefs.getBoolean("enabled", false))
        private set

    var intervalHours by mutableIntStateOf(prefs.getInt("interval_hours", 3))
        private set

    var order by mutableStateOf(prefs.getString("order", "sequential") ?: "sequential")
        private set

    var participatingFaces by mutableStateOf(loadParticipating())
        private set

    var lastRotationTimestamp by mutableLongStateOf(prefs.getLong("last_rotation_time", 0L))
        private set

    var lastRotatedFaceId by mutableStateOf(prefs.getString("last_rotated_face", null))
        private set

    private fun loadParticipating(): Set<String> {
        val str = prefs.getString("participating_faces", null)
        if (str == null) {
            return setOf("sage_nocturne", "lunar_phase", "obsidian_atelier")
        }
        return try {
            val arr = JSONArray(str)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } catch (_: Exception) {
            setOf("sage_nocturne", "lunar_phase", "obsidian_atelier")
        }
    }

    fun setRotationEnabled(value: Boolean) {
        prefs.edit().putBoolean("enabled", value).apply()
        enabled = value
    }

    fun setInterval(hours: Int) {
        val safeHours = when (hours) {
            1, 3, 6, 12, 24 -> hours
            else -> 3
        }
        prefs.edit().putInt("interval_hours", safeHours).apply()
        intervalHours = safeHours
    }

    fun setRotationOrder(newOrder: String) {
        val safeOrder = if (newOrder == "random") "random" else "sequential"
        prefs.edit().putString("order", safeOrder).apply()
        order = safeOrder
    }

    fun toggleFaceParticipation(faceId: String) {
        val updated = if (participatingFaces.contains(faceId)) {
            participatingFaces - faceId
        } else {
            participatingFaces + faceId
        }
        val arr = JSONArray()
        updated.forEach { arr.put(it) }
        prefs.edit().putString("participating_faces", arr.toString()).apply()
        participatingFaces = updated
    }

    fun recordSuccessfulRotation(faceId: String) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong("last_rotation_time", now)
            .putString("last_rotated_face", faceId)
            .apply()
        lastRotationTimestamp = now
        lastRotatedFaceId = faceId
    }
}
