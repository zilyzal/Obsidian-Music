package com.obsidian.music.data

import android.content.Context

/** Small settings and history the player remembers between launches. Synchronous, no database. */
class PlayerPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("player", Context.MODE_PRIVATE)

    var eqPreset: String
        get() = prefs.getString("eq_preset", null) ?: "Obsidian"
        set(v) = prefs.edit().putString("eq_preset", v).apply()

    var eqGains: List<Float>
        get() = prefs.getString("eq_gains", null)
            ?.split(',')?.mapNotNull { it.toFloatOrNull() }
            ?.takeIf { it.size == 5 }
            ?: listOf(0.28f, 0.14f, -0.06f, 0.22f, 0.34f)
        set(v) = prefs.edit().putString("eq_gains", v.joinToString(",")).apply()

    var spatial: Boolean
        get() = prefs.getBoolean("spatial", false)
        set(v) = prefs.edit().putBoolean("spatial", v).apply()

    var reduceTransparency: Boolean
        get() = prefs.getBoolean("reduce_transparency", false)
        set(v) = prefs.edit().putBoolean("reduce_transparency", v).apply()

    var recent: List<Long>
        get() = prefs.getString("recent", null)?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
        set(v) = prefs.edit().putString("recent", v.joinToString(",")).apply()
}
