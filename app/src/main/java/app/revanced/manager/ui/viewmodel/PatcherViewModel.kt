package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import app.revanced.manager.Variables.patches
import app.revanced.manager.Variables.selectedAppPackage
import app.revanced.manager.Variables.selectedPatches
import app.revanced.manager.api.API
import app.revanced.manager.patcher.worker.PatcherWorker
import app.revanced.manager.preferences.PreferencesManager
import app.revanced.manager.ui.Resource
import app.revanced.patcher.data.Data
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
import app.revanced.patcher.extensions.PatchExtensions.options
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.util.patch.impl.DexPatchBundle
import dalvik.system.DexClassLoader
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class PatcherViewModel(private val app: Application, private val api: API, private val prefs: PreferencesManager) : ViewModel() {
    private val patchBundleCacheDir =
        app.filesDir.resolve("patch-bundle-cache").also { it.mkdirs() }
    private lateinit var patchBundleFile: String


    init {
        runBlocking {
            loadPatches()
        }
    }

    fun selectPatch(patchId: String, state: Boolean) {
        if (state) selectedPatches.add(patchId)
        else selectedPatches.remove(patchId)
    }

    fun selectAllPatches(patchList: List<PatchClass>, selectAll: Boolean) {
        patchList.forEach { patch ->
            val patchId = patch.patch.patchName
            if (selectAll) selectedPatches.add(patchId)
            else selectedPatches.remove(patchId)
        }
    }

    fun isPatchSelected(patchId: String): Boolean {
        return selectedPatches.contains(patchId)
    }

    fun anyPatchSelected(): Boolean {
        return !selectedPatches.isEmpty()
    }


    private fun getSelectedPackageInfo() =
        if (selectedAppPackage.value.isPresent)
            app.packageManager.getPackageInfo(
                selectedAppPackage.value.get(),
                PackageManager.GET_META_DATA
            )
        else null

    fun getFilteredPatches(): List<PatchClass> {
        return buildList {
            val selected = getSelectedPackageInfo() ?: return@buildList
            val (patches) = patches.value as? Resource.Success ?: return@buildList
            patches.forEach patch@{ patch ->
                var unsupported = false
                var hasPatchOptions = false
                if (patch.options != null) {
                    hasPatchOptions = true
                    Log.d("ReVanced Manager", "${patch.patchName} has patch options.")
                }
                patch.compatiblePackages?.forEach { pkg ->
                    // if we detect unsupported once, don't overwrite it
                    if (pkg.name == selected.packageName) {
                        if (!unsupported)
                            unsupported =
                                pkg.versions.isNotEmpty() && !pkg.versions.any { it == selected.versionName }
                        add(PatchClass(patch, unsupported, hasPatchOptions))
                    }
                }
            }
        }
    }

    private suspend fun downloadPatchBundle(workdir: File): File {
        return try {
            val (_, out) = api.downloadFile(workdir, prefs.srcPatches.toString(), ".jar")
            out
        } catch (e: Exception) {
            throw Exception("Failed to download patch bundle", e)
        }
    }

    private fun loadPatches() = viewModelScope.launch {
        try {
            val file = downloadPatchBundle(patchBundleCacheDir)
            patchBundleFile = file.absolutePath
            loadPatches0(file.absolutePath)
        } catch (e: Exception) {
            Log.e("ReVancedManager", "An error occurred while loading patches", e)
        }
    }

    private fun loadPatches0(path: String) {
        val patchClasses = DexPatchBundle(
            path, DexClassLoader(
                path,
                app.codeCacheDir.absolutePath,
                null,
                javaClass.classLoader
            )
        ).loadPatches()
        patches.value = Resource.Success(patchClasses)
        Log.d("ReVanced Manager", "Finished loading patches")
    }

    fun startPatcher() {
        WorkManager
            .getInstance(app)
            .enqueue(
                OneTimeWorkRequest.Builder(PatcherWorker::class.java)
                    .setInputData(
                        androidx.work.Data.Builder()
                            .putStringArray("selectedPatches", selectedPatches.toTypedArray())
                            .putString("patchBundleFile", patchBundleFile)
                            .build()
                    )
                    .build()
            )
    }
}

data class PatchClass(
    val patch: Class<out Patch<Data>>,
    val unsupported: Boolean,
    val hasPatchOptions: Boolean,
)