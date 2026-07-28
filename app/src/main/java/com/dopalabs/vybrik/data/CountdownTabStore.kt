package com.dopalabs.vybrik.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class CountdownTabStore(context: Context) {
    private val preferences = context.getSharedPreferences("countdown_tabs", Context.MODE_PRIVATE)
    private val _tabs = MutableStateFlow(load())
    val tabs: StateFlow<List<CountdownTab>> = _tabs

    fun add(name: String) {
        val cleanName = name.trim().take(20)
        if (cleanName.isEmpty()) return
        save(_tabs.value + CountdownTab(UUID.randomUUID().toString(), cleanName))
    }

    fun rename(id: String, name: String) {
        val cleanName = name.trim().take(20)
        if (cleanName.isEmpty()) return
        save(_tabs.value.map { if (it.id == id) it.copy(name = cleanName) else it })
    }

    fun remove(id: String) {
        if (_tabs.value.size <= 1) return
        save(_tabs.value.filterNot { it.id == id })
    }

    private fun load(): List<CountdownTab> {
        val raw = preferences.getString(KEY_TABS, null) ?: return DEFAULT_TABS
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    add(CountdownTab(item.getString("id"), item.getString("name")))
                }
            }.ifEmpty { DEFAULT_TABS }
        }.getOrDefault(DEFAULT_TABS)
    }

    private fun save(tabs: List<CountdownTab>) {
        _tabs.value = tabs
        val json = JSONArray().apply {
            tabs.forEach { tab ->
                put(JSONObject().put("id", tab.id).put("name", tab.name))
            }
        }
        preferences.edit().putString(KEY_TABS, json.toString()).apply()
    }

    companion object {
        private const val KEY_TABS = "tabs"
        val DEFAULT_TABS = listOf(
            CountdownTab("daily", "Daily"),
            CountdownTab("one-time", "One-time"),
            CountdownTab("holidays", "Holidays"),
            CountdownTab("events", "Events")
        )
    }
}
