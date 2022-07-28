package app.revanced.manager.manager

import android.content.SharedPreferences
import app.revanced.manager.manager.base.BasePreferenceManager

class PreferencesManager(
    sharedPreferences: SharedPreferences
) : BasePreferenceManager(sharedPreferences) {
    var dynamicTheming by booleanPreference("dynamicTheming", false)
    var darklight by booleanPreference("darklight", false)
    var autoUpdate by booleanPreference("autoUpdate", false)
}