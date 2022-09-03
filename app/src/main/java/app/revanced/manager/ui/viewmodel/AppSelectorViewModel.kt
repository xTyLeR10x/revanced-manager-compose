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
import app.revanced.patcher.data.Data
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
import app.revanced.patcher.patch.Patch

class AppSelectorViewModel(val app: Application) : ViewModel() {
    private val installedApps = mutableStateOf<Resource<List<ApplicationInfo>>>(Resource.Loading)
    private val patches = mutableStateOf<Resource<List<Class<out Patch<Data>>>>>(Resource.Loading)
    val filteredApps = mutableListOf<ApplicationInfo>()

    init {
        fetchInstalledApps()
        filterApps()
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

    private fun filterApps(): List<ApplicationInfo> {
        return buildList {
            val (apps) = installedApps.value as? Resource.Success ?: return@buildList
            val (patches) = patches.value as? Resource.Success ?: return@buildList
            apps.forEach app@{ app ->
                patches.forEach patch@{ patch ->
                    patch.compatiblePackages?.forEach { pkg ->
                        if (app.packageName == pkg.name) {
                            filteredApps.add(app)
                        } else return@buildList
                    }
                }
            }
        }
    }

    fun applicationLabel(info: ApplicationInfo): String {
        return app.packageManager.getApplicationLabel(info).toString()
    }

    fun loadIcon(info: ApplicationInfo): Drawable? {
        return info.loadIcon(app.packageManager)
    }
}