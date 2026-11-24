package com.mohamedzayed.nocturne.data

import android.content.Context
import com.mohamedzayed.nocturne.model.WatchFaceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object CatalogRepository {

    private const val BANNED_EMERALD_ID = "emerald_nocturne"

    fun canonicalName(id: String, fallback: String): String = when (id) {
        "sage_nocturne" -> "Sage"
        "lunar_phase" -> "Lunar"
        "obsidian_atelier" -> "Obsidian"
        else -> fallback
    }

    fun canonicalNameAr(id: String, fallback: String): String = when (id) {
        "sage_nocturne" -> "سيج"
        "lunar_phase" -> "لونار"
        "obsidian_atelier" -> "أوبسيديان"
        else -> fallback
    }

    val masterCatalog = listOf(
        WatchFaceItem(
            id = "sage_nocturne",
            name = "Sage",
            nameAr = "سيج",
            description = "Ottoman Sage Green heritage dial with authentic arabesque, small moon phase, dhikr calligraphy, and 3 complications.",
            descriptionAr = "التصميم العثماني الأخضر الأصلي المسترجع بالكامل مع الزخرفة العثمانية، القمر الصغير، الذكر، والتعقيدات الثلاث.",
            packageName = "com.mohamedzayed.nocturne.watchfacepush.sage",
            validationToken = "kYFLcpyX+atPTOjj2hGB4P/qNOfxZLHbdOfowim+bm0=:MS4wLjA=",
            apkFilename = "Nocturne-Sage-Push.apk",
            downloadUrl = "https://github.com/MZAYED2/Nocturne/releases/latest/download/Nocturne-Sage-Push.apk",
            preview = "preview_sage.png",
            versionCode = 1007,
            versionName = "1.0.7",
            sha256 = "47e6ca812ed6e42555c2df0f4fd8c492a7dfff3b7fb124080d7b13bd77f1d0c9",
            sizeBytes = 28167780,
            defaultCategories = listOf("classic", "islamic", "luxury")
        ),
        WatchFaceItem(
            id = "lunar_phase",
            name = "Lunar",
            nameAr = "لونار",
            description = "Photorealistic Edge-to-Edge full moon disc from NASA 16-bit LROC data with astronomical lighting across 29 phases, applied faceted hour markers, and Haute Horlogerie hands.",
            descriptionAr = "قرص قمر واقعي يملأ الشاشة بالكامل Edge-to-Edge مأخوذ من بيانات NASA LROC الفضائية بدقة 16-bit عبر 29 طوراً فلكياً، مع علامات ساعات بارزة ثلاثية الأبعاد وعقارب دوفين فاخرة.",
            packageName = "com.mohamedzayed.nocturne.watchfacepush.lunar",
            validationToken = "bMe5QuJpyuumD/AfCQthpZ8zWdnb/GRBqC6Puscpw+Q=:MS4wLjA=",
            apkFilename = "Nocturne-Lunar-Push.apk",
            downloadUrl = "https://github.com/MZAYED2/Nocturne/releases/latest/download/Nocturne-Lunar-Push.apk",
            preview = "preview_lunar.png",
            versionCode = 1007,
            versionName = "1.0.7",
            sha256 = "09cd46e4e92b82b45aebd7f50d235204c3c73c8ee6aa0fa2b4c0d30300da1b70",
            sizeBytes = 28163684,
            defaultCategories = listOf("astronomy", "luxury", "minimal")
        ),
        WatchFaceItem(
            id = "obsidian_atelier",
            name = "Obsidian",
            nameAr = "أوبسيديان",
            description = "Haute Horlogerie mechanical dial in Brushed Platinum, Soft Titanium, and Smoked Graphite (Zero Gold, No Branding) with Dauphine hands, Small Seconds, and Power Reserve.",
            descriptionAr = "تحفة ميكانيكية فاخرة بباليتة البلاتين المصقول والتيتانيوم والجرافيت المدخن (بدون ذهب وبدون شعارات) مع عقارب دوفين المشطوفة، عقرب الثواني الصغير، مؤشر احتياطي الطاقة، ونافذة التاريخ.",
            packageName = "com.mohamedzayed.nocturne.watchfacepush.obsidian",
            validationToken = "UnsQ7e2fp48oONVV1mMlM/Z6Z1ZB3+5jGQ2d//b/smo=:MS4wLjA=",
            apkFilename = "Nocturne-Obsidian-Push.apk",
            downloadUrl = "https://github.com/MZAYED2/Nocturne/releases/latest/download/Nocturne-Obsidian-Push.apk",
            preview = "preview_obsidian.png",
            versionCode = 1007,
            versionName = "1.0.7",
            sha256 = "b52311852823a0aa058f48ff292deade92da47631316dfc077ef65be0c44e88c",
            sizeBytes = 28163684,
            defaultCategories = listOf("luxury", "classic", "minimal")
        )
    )

    fun loadLocalCatalog(context: Context): List<WatchFaceItem> {
        return try {
            val jsonStr = context.assets.open("catalog.json").bufferedReader().use { it.readText() }
            parseCatalog(jsonStr)
        } catch (_: Exception) {
            masterCatalog
        }
    }

    suspend fun fetchRemoteCatalog(): List<WatchFaceItem> = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://raw.githubusercontent.com/MZAYED2/Nocturne/main/catalog.json",
            "https://raw.githubusercontent.com/MZAYED2/Nocturne/main/distribution/catalog.json"
        )
        for (urlStr in urls) {
            try {
                val url = URL(urlStr)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Nocturne-Companion/1.0.8")
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val parsed = parseCatalog(body)
                    if (parsed.isNotEmpty()) return@withContext parsed
                }
            } catch (_: Exception) {}
        }
        emptyList()
    }

    fun parseCatalog(jsonStr: String): List<WatchFaceItem> {
        return try {
            val root = JSONObject(jsonStr)
            val array = root.getJSONArray("watchfaces")
            val items = mutableListOf<WatchFaceItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                // Mandatorily exclude Emerald Nocturne from active product
                if (id.equals(BANNED_EMERALD_ID, ignoreCase = true)) continue

                val defaultCats = when (id) {
                    "sage_nocturne" -> listOf("classic", "islamic", "luxury")
                    "lunar_phase" -> listOf("astronomy", "luxury", "minimal")
                    "obsidian_atelier" -> listOf("luxury", "classic", "minimal")
                    else -> listOf("classic")
                }

                val rawName = obj.optString("name", id)
                val rawNameAr = obj.optString("name_ar", rawName)

                items.add(
                    WatchFaceItem(
                        id = id,
                        name = canonicalName(id, rawName),
                        nameAr = canonicalNameAr(id, rawNameAr),
                        description = obj.getString("description"),
                        descriptionAr = obj.optString("description_ar", obj.getString("description")),
                        packageName = obj.getString("package_name"),
                        validationToken = obj.getString("validation_token"),
                        apkFilename = obj.getString("apk_filename"),
                        downloadUrl = obj.optString("download_url", ""),
                        preview = obj.optString("preview", ""),
                        versionCode = obj.optInt("version_code", 1007),
                        versionName = obj.optString("version_name", "1.0.7"),
                        sha256 = obj.optString("sha256", ""),
                        sizeBytes = obj.optLong("size_bytes", 0),
                        defaultCategories = defaultCats
                    )
                )
            }
            if (items.isNotEmpty()) items else masterCatalog
        } catch (_: Exception) {
            masterCatalog
        }
    }

    fun mergeCatalogs(current: List<WatchFaceItem>, remote: List<WatchFaceItem>): List<WatchFaceItem> {
        val map = current.filterNot { it.id == BANNED_EMERALD_ID }.associateBy { it.id }.toMutableMap()
        for (item in remote) {
            if (item.id != BANNED_EMERALD_ID) {
                map[item.id] = item
            }
        }
        return map.values.toList()
    }
}
