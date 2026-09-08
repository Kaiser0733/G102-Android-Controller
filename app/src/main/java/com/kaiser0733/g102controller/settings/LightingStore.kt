package com.kaiser0733.g102controller.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * App-side persistence for the lighting configuration. SharedPreferences only —
 * the mouse's onboard memory is never touched.
 */
class LightingStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("g102_lighting", Context.MODE_PRIVATE)

    fun save(config: LightingConfig) {
        prefs.edit().putString(KEY_CONFIG, config.serialize()).apply()
    }

    fun load(): LightingConfig? = LightingConfig.deserialize(prefs.getString(KEY_CONFIG, null))

    fun disableAutoApply() { prefs.edit().remove(KEY_AUTO_APPLY).apply() }

    private companion object {
        const val KEY_CONFIG = "config"
        const val KEY_AUTO_APPLY = "auto_apply"
    }
}
