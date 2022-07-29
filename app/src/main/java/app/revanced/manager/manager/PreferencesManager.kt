package app.revanced.manager.manager

import android.content.SharedPreferences
import app.revanced.manager.Global
import app.revanced.manager.manager.base.BasePreferenceManager
import app.revanced.manager.ui.theme.Theme

class PreferencesManager(
    sharedPreferences: SharedPreferences
) : BasePreferenceManager(sharedPreferences) {
    var theme by enumPreference("theme", Theme.SYSTEM)
    var dynamicTheming by booleanPreference("dynamicTheming", false)
    var autoUpdate by booleanPreference("autoUpdate", false)
    var patches by stringPreference("patches", Global.ghPatches)
    var integrations by stringPreference("integrations", Global.ghIntegrations)
}