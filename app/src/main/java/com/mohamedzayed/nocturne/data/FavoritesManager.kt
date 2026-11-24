package com.mohamedzayed.nocturne.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class FavoritesManager(context: Context) {
    private val prefs = context.getSharedPreferences("nocturne_favorites_prefs", Context.MODE_PRIVATE)

    var favorites by mutableStateOf(prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet())
        private set

    fun isFavorite(faceId: String): Boolean {
        return favorites.contains(faceId)
    }

    fun toggleFavorite(faceId: String): Boolean {
        val updated = favorites.toMutableSet()
        val newState = if (updated.contains(faceId)) {
            updated.remove(faceId)
            false
        } else {
            updated.add(faceId)
            true
        }
        prefs.edit().putStringSet("favorite_ids", updated).apply()
        favorites = updated
        return newState
    }
}
