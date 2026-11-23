package com.mohamedzayed.nocturne

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.launch

internal data class DialSettings(
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
            json.getString("background"), json.getString("accent"), json.getString("ornament"),
            json.getString("dial"), json.optBoolean("aodPreview")
        ).also {
            require(backgroundOptions.any { option -> option.id == it.background })
            require(accentOptions.any { option -> option.id == it.accent })
            require(ornamentOptions.any { option -> option.first == it.ornament })
            require(dialOptions.any { option -> option.first == it.dial })
        }
    }
}

internal data class ThemePreset(
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
            require(json.getInt("schema") == 1) { "Unsupported preset schema" }
            return ThemePreset(
                UUID.fromString(json.getString("id")).toString(), json.getString("parentTheme"),
                json.getString("name").trim().also { require(it.length in 1..60) },
                DialSettings.parse(json.getJSONObject("settings")), DialSettings.parse(json.getJSONObject("defaults")),
                json.getLong("created"), json.getLong("modified")
            )
        }
    }
}

internal class ThemeLibrary(context: Context) {
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
}

@Composable
internal fun ThemeManagerDialog(
    catalog: List<WatchFaceItem>,
    library: ThemeLibrary,
    onOpen: (ThemePreset) -> Unit,
    onClose: () -> Unit
) = MaterialTheme(colorScheme = darkColorScheme(
    surface = androidx.compose.ui.graphics.Color(0xFF181E26),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF202730),
    primary = androidx.compose.ui.graphics.Color(0xFFE4C582)
)) {
    val context = LocalContext.current
    var action by remember { mutableStateOf<(() -> Unit)?>(null) }
    var confirmation by remember { mutableStateOf("") }
    var edited by remember { mutableStateOf<ThemePreset?>(null) }
    var name by remember { mutableStateOf("") }
    var export by remember { mutableStateOf<ThemePreset?>(null) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var watchSlots by remember { mutableStateOf<Map<String, Boolean>?>(null) }
    var watchNodeId by remember { mutableStateOf<String?>(null) }
    fun queryWatch(request: String, nodeId: String? = null) {
        scope.launch {
            try {
                val bridge = findWatchBridge(context, request, nodeId)
                val reply = bridge?.management ?: error("Watch unavailable or WearBridge needs an update")
                watchNodeId = bridge.node.id
                if (reply.has("error")) error(reply.getString("error"))
                val slots = reply.getJSONArray("slots")
                watchSlots = (0 until slots.length()).associate { index ->
                    val slot = slots.getJSONObject(index)
                    slot.getString("package") to slot.getBoolean("active")
                }
                message = if (request.startsWith("remove|")) "Wear OS confirmed removal" else ""
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                watchSlots = null
                message = e.localizedMessage ?: "Unable to query the watch"
            }
        }
    }
    LaunchedEffect(Unit) { queryWatch("list") }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val preset = export
        if (uri != null && preset != null) {
            message = runCatching {
                requireNotNull(context.contentResolver.openOutputStream(uri)).bufferedWriter().use {
                    it.write(preset.json().toString(2))
                }
                "Preset exported"
            }.getOrElse { "Export failed" }
        }
        export = null
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            message = runCatching {
                val data = requireNotNull(context.contentResolver.openInputStream(uri)).use { stream ->
                    val bytes = ByteArray(16_385)
                    var count = 0
                    while (count < bytes.size) {
                        val read = stream.read(bytes, count, bytes.size - count)
                        if (read < 0) break
                        count += read
                    }
                    require(count <= 16_384) { "Preset file is too large" }
                    String(bytes, 0, count, Charsets.UTF_8)
                }
                val imported = ThemePreset.parse(JSONObject(data))
                require(catalog.any { it.id == imported.parent }) { "Parent theme unavailable" }
                val now = System.currentTimeMillis()
                library.save(imported.copy(id = UUID.randomUUID().toString(), created = now, modified = now))
                "Preset imported"
            }.getOrElse { it.message ?: "Invalid preset" }
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Theme Manager") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Themes", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { queryWatch("list") }) { Text("Refresh watch status") }
                catalog.forEach { theme ->
                    val hidden = theme.id in library.hidden
                    Text(theme.name)
                    Text(if (hidden) "Archived" else if (theme.id == "emerald_nocturne") "Legacy" else "Master")
                    val installed = watchSlots?.get(theme.packageName)
                    Text(when (installed) {
                        true -> "Active on watch"
                        false -> "Installed on watch"
                        null -> if (watchSlots == null) "Watch status unavailable" else "No Nocturne push slot"
                    })
                    if (installed != null) {
                        TextButton(onClick = {
                            if (installed) message = "Select another face on the watch before removing this active face."
                            else {
                                confirmation = "Remove ${theme.name} from the watch?"
                                val confirmedNode = watchNodeId
                                action = {
                                    if (confirmedNode != null) queryWatch("remove|${theme.packageName}", confirmedNode)
                                }
                            }
                        }) { Text("Remove from watch") }
                    }
                    Row {
                        TextButton(onClick = {
                            val preset = ThemePreset.create(theme.id, (theme.name + " copy").take(60), DialSettings())
                            library.save(preset)
                            onOpen(preset)
                        }) { Text("Duplicate") }
                        TextButton(onClick = {
                            if (hidden) library.hide(theme.id, false) else {
                                confirmation = "Archive ${theme.name} in the phone catalog?"
                                action = { library.hide(theme.id, true) }
                            }
                        }) { Text(if (hidden) "Restore" else "Archive") }
                    }
                }
                Text("My Presets", style = MaterialTheme.typography.titleMedium)
                if (library.presets.isEmpty()) Text("Duplicate a theme to create a preset.")
                library.presets.forEach { preset ->
                    Text(preset.name)
                    Row {
                        TextButton(onClick = { onOpen(preset) }) { Text("Customize") }
                        TextButton(onClick = { edited = preset; name = preset.name }) { Text("Rename") }
                    }
                    Row {
                        TextButton(onClick = {
                            confirmation = "Delete ${preset.name} from the phone library?"
                            action = { library.delete(preset.id) }
                        }) { Text("Delete") }
                        TextButton(onClick = {
                            export = preset
                            saveFile.launch("nocturne-preset.json")
                        }) { Text("Export") }
                    }
                }
                TextButton(onClick = { openFile.launch(arrayOf("application/json")) }) { Text("Import preset") }
                if (message.isNotEmpty()) Text(message)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } }
    )

    edited?.let { preset ->
        AlertDialog(onDismissRequest = { edited = null }, title = { Text("Rename preset") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it.take(60) }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    library.save(preset.copy(name = name.trim(), modified = System.currentTimeMillis()))
                    edited = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { edited = null }) { Text("Cancel") } })
    }
    action?.let { confirm ->
        AlertDialog(onDismissRequest = { action = null }, title = { Text(confirmation) },
            confirmButton = { TextButton(onClick = { confirm(); action = null }) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = { action = null }) { Text("Cancel") } })
    }
}
