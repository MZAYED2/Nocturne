package com.mohamedzayed.nocturne.data

import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    fun exportBackup(
        themeLibrary: ThemeLibrary,
        favoritesManager: FavoritesManager,
        categoryManager: CategoryManager
    ): String {
        val root = JSONObject()
        root.put("schema", 1)
        root.put("app", "Nocturne")
        root.put("exported_at", System.currentTimeMillis())

        // 1. Presets
        val presetsArr = JSONArray()
        themeLibrary.presets.forEach { preset ->
            presetsArr.put(preset.json())
        }
        root.put("presets", presetsArr)

        // 2. Favorites
        val favsArr = JSONArray()
        favoritesManager.favorites.forEach { favId ->
            favsArr.put(favId)
        }
        root.put("favorites", favsArr)

        // 3. Custom Categories
        val catsArr = JSONArray()
        categoryManager.customCategories.forEach { cat ->
            catsArr.put(JSONObject().apply {
                put("id", cat.id)
                put("name", cat.customName ?: cat.id)
            })
        }
        root.put("custom_categories", catsArr)

        // 4. Category Assignments
        val assignObj = JSONObject()
        categoryManager.categoryAssignments.forEach { (faceId, set) ->
            val arr = JSONArray()
            set.forEach { arr.put(it) }
            assignObj.put(faceId, arr)
        }
        root.put("category_assignments", assignObj)

        return root.toString(2)
    }

    fun importBackup(
        jsonString: String,
        themeLibrary: ThemeLibrary,
        favoritesManager: FavoritesManager,
        categoryManager: CategoryManager
    ): Result<String> {
        return runCatching {
            val root = JSONObject(jsonString)
            val schema = root.optInt("schema", 0)
            require(schema == 1) { "Unsupported backup schema version: $schema" }

            var presetCount = 0
            var favCount = 0
            var catCount = 0

            // 1. Import Presets
            val presetsArr = root.optJSONArray("presets")
            if (presetsArr != null) {
                for (i in 0 until presetsArr.length()) {
                    val obj = presetsArr.getJSONObject(i)
                    runCatching {
                        val preset = ThemePreset.parse(obj)
                        themeLibrary.save(preset)
                        presetCount++
                    }
                }
            }

            // 2. Import Favorites
            val favsArr = root.optJSONArray("favorites")
            if (favsArr != null) {
                for (i in 0 until favsArr.length()) {
                    val favId = favsArr.getString(i)
                    if (!favoritesManager.isFavorite(favId)) {
                        favoritesManager.toggleFavorite(favId)
                        favCount++
                    }
                }
            }

            // 3. Import Custom Categories
            val catsArr = root.optJSONArray("custom_categories")
            if (catsArr != null) {
                for (i in 0 until catsArr.length()) {
                    val obj = catsArr.getJSONObject(i)
                    val name = obj.optString("name", "")
                    if (name.isNotBlank()) {
                        if (categoryManager.addCustomCategory(name)) {
                            catCount++
                        }
                    }
                }
            }

            "Imported $presetCount presets, $favCount favorites, $catCount categories."
        }
    }
}
