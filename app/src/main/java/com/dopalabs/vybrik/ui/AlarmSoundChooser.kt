package com.dopalabs.vybrik.ui

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class AlarmSoundChoice(val uri: String, val title: String, val imported: Boolean = false)

object AlarmSoundStore {
    private const val PREFS = "alarm_sound_library"
    private const val LAST_URI = "last_uri"
    private const val LAST_TITLE = "last_title"
    private const val IMPORTED = "imported_audio"

    fun lastChoice(context: Context): AlarmSoundChoice {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
        return AlarmSoundChoice(
            uri = preferences.getString(LAST_URI, defaultUri) ?: defaultUri,
            title = preferences.getString(LAST_TITLE, "System alarm") ?: "System alarm"
        )
    }

    fun remember(context: Context, choice: AlarmSoundChoice) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(LAST_URI, choice.uri)
            .putString(LAST_TITLE, choice.title)
            .apply()
        if (choice.imported) addImported(context, choice)
    }

    fun addImported(context: Context, choice: AlarmSoundChoice) {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = imported(context).filterNot { it.uri == choice.uri } + choice.copy(imported = true)
        val json = JSONArray().apply {
            current.forEach { put(JSONObject().put("uri", it.uri).put("title", it.title)) }
        }
        preferences.edit().putString(IMPORTED, json.toString()).apply()
    }

    fun imported(context: Context): List<AlarmSoundChoice> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(IMPORTED, null)
            ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    add(AlarmSoundChoice(item.getString("uri"), item.getString("title"), imported = true))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun loadLibrary(context: Context): List<AlarmSoundChoice> {
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
        val system = buildList {
            add(AlarmSoundChoice(defaultUri, "System alarm"))
            val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
            val cursor = manager.cursor
            try {
                var index = 0
                while (cursor.moveToNext()) {
                    val uri = manager.getRingtoneUri(index++)?.toString() ?: continue
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: "Alarm tone"
                    add(AlarmSoundChoice(uri, title))
                }
            } finally {
                cursor.close()
            }
        }
        return (imported(context) + system).distinctBy { it.uri }
    }

    fun displayName(context: Context, uri: Uri): String {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull().orEmpty().ifBlank { "Imported alarm" }
    }
}

@Composable
fun AlarmSoundChooserDialog(
    selected: AlarmSoundChoice,
    onDismiss: () -> Unit,
    onSelected: (AlarmSoundChoice) -> Unit
) {
    val context = LocalContext.current
    var selectedChoice by remember(selected) { mutableStateOf(selected) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val sounds by produceState<List<AlarmSoundChoice>>(emptyList(), refreshKey) {
        value = withContext(Dispatchers.IO) { AlarmSoundStore.loadLibrary(context.applicationContext) }
    }
    val importAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            selectedChoice = AlarmSoundChoice(uri.toString(), AlarmSoundStore.displayName(context, uri), imported = true)
            AlarmSoundStore.addImported(context, selectedChoice)
            refreshKey++
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("ALARM LIBRARY", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
                Text("SYSTEM TONES + YOUR AUDIO", color = Coral, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { importAudio.launch(arrayOf("audio/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AudioFile, null)
                    Text("  IMPORT AUDIO FILE", fontWeight = FontWeight.Black)
                }
                if (sounds.isEmpty()) {
                    Text("LOADING SOUND LIBRARY…", color = Muted)
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth().heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(sounds, key = { it.uri }) { sound ->
                            val isSelected = sound.uri == selectedChoice.uri
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(
                                        if (isSelected) Coral.copy(alpha = 0.13f) else Concrete.copy(alpha = 0.42f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedChoice = sound }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(isSelected, { selectedChoice = sound })
                                Icon(
                                    if (sound.imported) Icons.Default.AudioFile else Icons.Default.LibraryMusic,
                                    null,
                                    tint = if (sound.imported) Acid else Sky,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(Modifier.padding(start = 9.dp).weight(1f)) {
                                    Text(sound.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (sound.imported) "IMPORTED" else "SYSTEM", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                AlarmSoundStore.remember(context, selectedChoice)
                onSelected(selectedChoice)
                onDismiss()
            }) { Text("USE SOUND", style = androidx.compose.material3.MaterialTheme.typography.labelLarge) }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}
