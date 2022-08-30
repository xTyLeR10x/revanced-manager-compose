package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import app.revanced.manager.api.API
import app.revanced.manager.patcher.worker.PatcherWorker
import app.revanced.manager.ui.Resource
import app.revanced.manager.util.ghPatches
import app.revanced.patcher.data.Data
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.util.patch.impl.DexPatchBundle
import dalvik.system.DexClassLoader
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*

class PatcherViewModel(private val app: Application, private val api: API) : ViewModel() {
    private val patchBundleCacheDir = app.filesDir.resolve("patch-bundle-cache").also { it.mkdirs() }

    val selectedAppPackage = mutableStateOf(Optional.empty<String>())
    val selectedPatches = mutableStateListOf<String>()
    val patches = mutableStateOf<Resource<List<Class<out Patch<Data>>>>>(Resource.Loading)
    lateinit var patchBundleFile: String

    init { runBlocking {
        loadPatches()
    }  }

    fun setSelectedAppPackage(appId: String) {
        selectedAppPackage.value.ifPresent { s ->
            if (s != appId) selectedPatches.clear()
        }
        selectedAppPackage.value = Optional.of(appId)
    }

    fun selectPatch(patchId: String, state: Boolean) {
        if (state) selectedPatches.add(patchId)
        else selectedPatches.remove(patchId)
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
                patch.compatiblePackages?.forEach { pkg ->
                    // if we detect unsupported once, don't overwrite it
                    if (pkg.name == selected.packageName) {
                        if (!unsupported)
                            unsupported =
                                pkg.versions.isNotEmpty() && !pkg.versions.any { it == selected.versionName }
                        add(PatchClass(patch, unsupported))
                    }
                }
            }
        }
    }

    private suspend fun downloadDefaultPatchBundle(workdir: File): File {
        return try {
            val (_, out) = api.downloadPatches(workdir, ghPatches)
            out
        } catch (e: Exception) {
            throw Exception("Failed to download default patch bundle", e)
        }
    }

    private fun loadPatches() = viewModelScope.launch {
        try {
            val file = downloadDefaultPatchBundle(patchBundleCacheDir)
            patchBundleFile=file.absolutePath
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
    }

    fun startPatcher() {
        WorkManager
            .getInstance(app)
            .enqueue(
                OneTimeWorkRequest.Builder(PatcherWorker::class.java)
                    .setInputData(
                        androidx.work.Data.Builder()
                            .putStringArray("selectedPatches",selectedPatches.toTypedArray())
                            .putString("patchBundleFile",patchBundleFile)
                            .build()
                    )
                    .build()
            )
    }
}

data class PatchClass(
    val patch: Class<out Patch<Data>>,
    val unsupported: Boolean,
)