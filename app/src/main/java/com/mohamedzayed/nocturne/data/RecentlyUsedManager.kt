package com.mohamedzayed.nocturne.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mohamedzayed.nocturne.model.WatchFaceItem
import org.json.JSONArray

class RecentlyUsedManager(context: Context) {
    private val prefs = context.getSharedPreferences("nocturne_recent_prefs", Context.MODE_PRIVATE)

    var recentFaceIds by mutableStateOf(loadRecents())
        private set

    private fun loadRecents(): List<String> {
        val str = prefs.getString("recent_ids", "[]") ?: "[]"
        return try {
            val arr = JSONArray(str)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun recordUsed(faceId: String) {
        val current = recentFaceIds.filterNot { it == faceId }.toMutableList()
        current.add(0, faceId) // Move to front
        val trimmed = current.take(5)
        val arr = JSONArray()
        trimmed.forEach { arr.put(it) }
        prefs.edit().putString("recent_ids", arr.toString()).apply()
        recentFaceIds = trimmed
    }

    fun getRecentFaces(catalog: List<WatchFaceItem>): List<WatchFaceItem> {
        return recentFaceIds.mapNotNull { id -> catalog.firstOrNull { it.id == id } }
    }
}
