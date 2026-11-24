package com.mohamedzayed.nocturne.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mohamedzayed.nocturne.R
import org.json.JSONArray
import org.json.JSONObject

data class CategoryItem(
    val id: String,
    val nameRes: Int? = null,
    val customName: String? = null,
    val isSystem: Boolean = true
) {
    fun getDisplayName(context: Context): String {
        return nameRes?.let { context.getString(it) } ?: (customName ?: id)
    }
}

class CategoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("nocturne_categories_prefs", Context.MODE_PRIVATE)

    val systemCategories = listOf(
        CategoryItem("all", R.string.category_all, isSystem = true),
        CategoryItem("classic", R.string.category_classic, isSystem = true),
        CategoryItem("minimal", R.string.category_minimal, isSystem = true),
        CategoryItem("islamic", R.string.category_islamic, isSystem = true),
        CategoryItem("luxury", R.string.category_luxury, isSystem = true),
        CategoryItem("astronomy", R.string.category_astronomy, isSystem = true),
        CategoryItem("digital", R.string.category_digital, isSystem = true),
        CategoryItem("favorites", R.string.category_favorites, isSystem = true),
        CategoryItem("my_presets", R.string.category_my_presets, isSystem = true)
    )

    var customCategories by mutableStateOf(loadCustomCategories())
        private set

    var categoryAssignments by mutableStateOf(loadAssignments())
        private set

    val allCategories: List<CategoryItem>
        get() = systemCategories + customCategories

    private fun loadCustomCategories(): List<CategoryItem> {
        val str = prefs.getString("custom_categories", "[]") ?: "[]"
        return try {
            val arr = JSONArray(str)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                CategoryItem(
                    id = obj.getString("id"),
                    customName = obj.getString("name"),
                    isSystem = false
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCustomCategories(list: List<CategoryItem>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.customName ?: it.id)
            })
        }
        prefs.edit().putString("custom_categories", arr.toString()).apply()
        customCategories = list
    }

    fun addCustomCategory(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val id = "user_" + trimmed.lowercase().replace(" ", "_")
        if (allCategories.any { it.id == id || it.customName.equals(trimmed, ignoreCase = true) }) {
            return false
        }
        val updated = customCategories + CategoryItem(id = id, customName = trimmed, isSystem = false)
        saveCustomCategories(updated)
        return true
    }

    fun renameCustomCategory(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val updated = customCategories.map {
            if (it.id == id && !it.isSystem) it.copy(customName = trimmed) else it
        }
        saveCustomCategories(updated)
    }

    fun deleteCustomCategory(id: String) {
        val updated = customCategories.filterNot { it.id == id && !it.isSystem }
        saveCustomCategories(updated)
        // Also remove assignment
        val updatedAssignments = categoryAssignments.mapValues { (_, cats) -> cats - id }
        saveAssignments(updatedAssignments)
    }

    private fun loadAssignments(): Map<String, Set<String>> {
        val str = prefs.getString("assignments", "{}") ?: "{}"
        return try {
            val obj = JSONObject(str)
            val map = mutableMapOf<String, Set<String>>()
            obj.keys().forEach { faceId ->
                val arr = obj.getJSONArray(faceId)
                val set = (0 until arr.length()).map { arr.getString(it) }.toSet()
                map[faceId] = set
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveAssignments(map: Map<String, Set<String>>) {
        val obj = JSONObject()
        map.forEach { (faceId, cats) ->
            val arr = JSONArray()
            cats.forEach { arr.put(it) }
            obj.put(faceId, arr)
        }
        prefs.edit().putString("assignments", obj.toString()).apply()
        categoryAssignments = map
    }

    fun getCategoriesForFace(faceId: String, defaults: List<String>): Set<String> {
        val saved = categoryAssignments[faceId]
        return saved ?: defaults.toSet()
    }

    fun assignCategory(faceId: String, categoryId: String, defaults: List<String>) {
        val current = getCategoriesForFace(faceId, defaults).toMutableSet()
        current.add(categoryId)
        val updated = categoryAssignments.toMutableMap()
        updated[faceId] = current
        saveAssignments(updated)
    }

    fun removeCategoryFromFace(faceId: String, categoryId: String, defaults: List<String>) {
        val current = getCategoriesForFace(faceId, defaults).toMutableSet()
        current.remove(categoryId)
        val updated = categoryAssignments.toMutableMap()
        updated[faceId] = current
        saveAssignments(updated)
    }

    fun isFaceInCategory(faceId: String, categoryId: String, defaults: List<String>): Boolean {
        if (categoryId == "all") return true
        val assigned = getCategoriesForFace(faceId, defaults)
        return assigned.any { it.equals(categoryId, ignoreCase = true) }
    }
}
