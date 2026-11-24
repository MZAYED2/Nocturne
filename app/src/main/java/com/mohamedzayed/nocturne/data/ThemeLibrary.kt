package com.mohamedzayed.nocturne.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mohamedzayed.nocturne.model.accentOptions
import com.mohamedzayed.nocturne.model.backgroundOptions
import com.mohamedzayed.nocturne.model.dialOptions
import com.mohamedzayed.nocturne.model.ornamentOptions
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class DialSettings(
    val background: String = "black",
    val accent: String = "sage",
    val ornament: String = "balanced",
    val dial: String = "full",
    val aodPreview: Boolean = false
) {
    fun json() = JSONObject().put("background", background).put("accent", accent)
        .put("ornament", ornament).put("dial", dial).put("aodPreview", aodPreview)

    companion object {
        fun parse(json: JSONObject): DialSettings = DialSettings(
            json.optString("background", "black"),
            json.optString("accent", "sage"),
            json.optString("ornament", "balanced"),
            json.optString("dial", "full"),
            json.optBoolean("aodPreview", false)
        ).also {
            require(backgroundOptions.any { option -> option.id == it.background }) { "Invalid background: ${it.background}" }
            require(accentOptions.any { option -> option.id == it.accent }) { "Invalid accent: ${it.accent}" }
            require(ornamentOptions.any { option -> option.id == it.ornament }) { "Invalid ornament: ${it.ornament}" }
            require(dialOptions.any { option -> option.id == it.dial }) { "Invalid dial: ${it.dial}" }
        }
    }
}

data class ThemePreset(
    val id: String,
    val parent: String,
    val name: String,
    val settings: DialSettings,
    val defaults: DialSettings,
    val created: Long,
    val modified: Long
) {
    fun json() = JSONObject().put("schema", 1).put("id", id).put("parentTheme", parent)
        .put("name", name).put("settings", settings.json()).put("defaults", defaults.json())
        .put("created", created).put("modified", modified)
        .put("compatibility", "native-wff-editor").put("complicationSelection", "on-watch")

    companion object {
        fun create(parent: String, name: String, settings: DialSettings): ThemePreset {
            val now = System.currentTimeMillis()
            return ThemePreset(UUID.randomUUID().toString(), parent, name, settings, settings, now, now)
        }

        fun parse(json: JSONObject): ThemePreset {
            require(json.optInt("schema", 1) == 1) { "Unsupported preset schema" }
            var parentTheme = json.getString("parentTheme")
            // Safe migration: if an old preset references Emerald Nocturne, migrate safely to Sage Nocturne
            if (parentTheme.equals("emerald_nocturne", ignoreCase = true)) {
                parentTheme = "sage_nocturne"
            }
            return ThemePreset(
                id = json.optString("id", UUID.randomUUID().toString()),
                parent = parentTheme,
                name = json.getString("name").trim().take(60),
                settings = DialSettings.parse(json.getJSONObject("settings")),
                defaults = DialSettings.parse(json.optJSONObject("defaults") ?: json.getJSONObject("settings")),
                created = json.optLong("created", System.currentTimeMillis()),
                modified = json.optLong("modified", System.currentTimeMillis())
            )
        }
    }
}

class ThemeLibrary(context: Context) {
    private val storage = context.getSharedPreferences("nocturne_theme_library", Context.MODE_PRIVATE)

    var presets by mutableStateOf(readPresets())
        private set

    var hidden by mutableStateOf(storage.getStringSet("hidden", emptySet()).orEmpty().toSet())
        private set

    private fun readPresets(): List<ThemePreset> {
        val entries = runCatching { JSONArray(storage.getString("presets", "[]")) }.getOrElse { JSONArray() }
        return (0 until entries.length()).mapNotNull { index ->
            runCatching { ThemePreset.parse(entries.getJSONObject(index)) }.getOrNull()
        }
    }

    fun save(preset: ThemePreset) = persist(presets.filterNot { it.id == preset.id } + preset)

    fun delete(id: String) = persist(presets.filterNot { it.id == id })

    private fun persist(values: List<ThemePreset>) {
        val data = JSONArray().apply { values.forEach { put(it.json()) } }
        storage.edit().putString("presets", data.toString()).apply()
        presets = values
    }

    fun hide(id: String, value: Boolean) {
        val updated = if (value) hidden + id else hidden - id
        storage.edit().putStringSet("hidden", updated).apply()
        hidden = updated
    }

    fun duplicate(preset: ThemePreset, copySuffix: String = "Copy"): ThemePreset {
        val now = System.currentTimeMillis()
        val newPreset = preset.copy(
            id = UUID.randomUUID().toString(),
            name = "${preset.name} $copySuffix".take(60),
            created = now,
            modified = now
        )
        save(newPreset)
        return newPreset
    }

    fun rename(id: String, newName: String) {
        val p = presets.firstOrNull { it.id == id } ?: return
        save(p.copy(name = newName.trim().take(60), modified = System.currentTimeMillis()))
    }
}
