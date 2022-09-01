package app.revanced.manager.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import app.revanced.manager.ui.Resource

class AppSelectorViewModel(val app: Application) : ViewModel() {
    val installedApps = mutableStateOf<Resource<List<ApplicationInfo>>>(Resource.Loading)

    init {
        fetchInstalledApps()
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun fetchInstalledApps() {
            Log.d("ReVanced Manager", "Fetching applications")
        try {
            installedApps.value =
                Resource.success(app.packageManager.getInstalledApplications(PackageManager.GET_META_DATA))
        } catch (e: Exception) {
            Log.e("ReVanced Manager", "An error occurred while fetching apps", e)
        }
    }

    fun applicationLabel(info: ApplicationInfo): String {
        return app.packageManager.getApplicationLabel(info).toString()
    }

    fun loadIcon(info: ApplicationInfo): Drawable? {
        return info.loadIcon(app.packageManager)
    }
}