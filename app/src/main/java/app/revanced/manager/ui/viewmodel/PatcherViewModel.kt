package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.Variables.patches
import app.revanced.manager.Variables.selectedAppPackage
import app.revanced.manager.Variables.selectedPatches
import app.revanced.manager.api.API
import app.revanced.manager.patcher.aapt.Aapt
import app.revanced.manager.patcher.aligning.ZipAligner
import app.revanced.manager.patcher.aligning.zip.ZipFile
import app.revanced.manager.patcher.aligning.zip.structures.ZipEntry
import app.revanced.manager.patcher.signing.Signer
import app.revanced.manager.ui.Resource
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.data.Data
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
import app.revanced.patcher.extensions.PatchExtensions.options
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.logging.Logger
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.util.patch.impl.DexPatchBundle
import dalvik.system.DexClassLoader
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import java.io.File

class PatcherViewModel(private val app: Application, private val api: API) : ViewModel() {
    private val patchBundleCacheDir =
        app.filesDir.resolve("patch-bundle-cache").also { it.mkdirs() }
    private val integrationsCacheDir =
        app.filesDir.resolve("integrations-cache").also { it.mkdirs() }
    private lateinit var patchBundleFile: String
    val tag = "ReVanced Manager"


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
            if (selectAll && !patch.unsupported) selectedPatches.add(patchId)
            else selectedPatches.remove(patchId)
        }
    }

    fun setOption(patch: PatchClass, key: String, value: String) {
        patch.patch.options?.set(key, value)
        for (option in patch.patch.options!!) {
            println(option.key + option.value + option.title + option.description)
        }
    }

    fun getOption(patch: PatchClass, key: String) {
        patch.patch.options?.get(key)
    }

    fun isPatchSelected(patchId: String): Boolean {
        return selectedPatches.contains(patchId)
    }

    fun anyPatchSelected(): Boolean {
        return !selectedPatches.isEmpty()
    }


    fun getSelectedPackageInfo() =
        if (selectedAppPackage.value.isPresent)
            app.packageManager.getPackageInfo(
                selectedAppPackage.value.get(),
                PackageManager.GET_META_DATA
            )
        else null

    fun getFilteredPatchesAndCheckOptions(): List<PatchClass> {
        return buildList {
            val selected = getSelectedPackageInfo() ?: return@buildList
            val (patches) = patches.value as? Resource.Success ?: return@buildList
            patches.forEach patch@{ patch ->
                var unsupported = false
                var hasPatchOptions = false
                if (patch.options != null) {
                    hasPatchOptions = true
                    Log.d(tag, "${patch.patchName} has patch options.")
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

    private fun loadPatches() = viewModelScope.launch {
        try {
            val file = api.downloadPatchBundle(patchBundleCacheDir)
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
        viewModelScope.launch {

            val aaptPath = Aapt.binary(app).absolutePath
            val frameworkPath =
                app.filesDir.resolve("framework").also { it.mkdirs() }.absolutePath

            Log.d(tag, "Checking prerequisites")
            val patches = findPatchesByIds(selectedPatches)
            if (patches.isEmpty()) return@launch

            Log.d(tag, "Creating directories")
            val workdir = createWorkDir()
            val inputFile = File(
                getSelectedPackageInfo()?.applicationInfo?.publicSourceDir
                    ?: "Can't find input APK."
            )
            val patchedFile = File(workdir, "patched.apk")
            val alignedFile = File(workdir, "aligned.apk")
            val outputFile = File(workdir, "out.apk")
            val cacheDirectory = workdir.resolve("cache")
            val integrations = api.downloadIntegrations(integrationsCacheDir)
            try {
                Log.d(tag, "Creating patcher")
                val patcher = Patcher(
                    PatcherOptions(
                        inputFile,
                        cacheDirectory.absolutePath,
                        patchResources = true,
                        aaptPath = aaptPath,
                        frameworkFolderLocation = frameworkPath,
                        logger = object : Logger {
                            override fun error(msg: String) {
                                Log.e(tag, msg)
                            }

                            override fun warn(msg: String) {
                                Log.w(tag, msg)
                            }

                            override fun info(msg: String) {
                                Log.i(tag, msg)
                            }

                            override fun trace(msg: String) {
                                Log.v(tag, msg)
                            }
                        }
                    )
                )

                Log.d(tag, "Merging integrations")//TODO add again
                patcher.addFiles(listOf(integrations)) {}

                Log.d(tag, "Adding ${patches.size} patch(es)")
                patcher.addPatches(patches)

                Log.d(tag, "Applying patches")
                patcher.applyPatches().forEach { (patch, result) ->
                    if (result.isSuccess) {
                        Log.i(tag, "[success] $patch")
                        return@forEach
                    }
                    Log.e(tag, "[error] $patch:", result.exceptionOrNull()!!)
                }

                Log.d(tag, "Saving file")
                val result = patcher.save()
                ZipFile(patchedFile).use { fs ->
                    result.dexFiles.forEach {
                        Log.d(tag, "Writing dex file ${it.name}")
                        fs.addEntryCompressData(
                            ZipEntry.createWithName(it.name),
                            it.stream.readAllBytes()
                        )
                    }
                    fs.copyEntriesFromFileAligned(ZipFile(inputFile), ZipAligner::getEntryAlignment)
                }
                Log.d(tag, "Signing apk")
                Signer("ReVanced", "s3cur3p@ssw0rd").signApk(alignedFile, outputFile)
                Log.i(tag, "Successfully patched into $outputFile")
            } catch (e: Exception) {
                Log.e(tag, "Error while patching", e)
            }

            Log.d(tag, "Deleting workdir")
            workdir.deleteRecursively()

        }
    }

    private fun createWorkDir(): File {
        return app.filesDir.resolve("tmp-${System.currentTimeMillis()}")
            .also { it.mkdirs() }
    }

    private fun findPatchesByIds(ids: Iterable<String>): List<Class<out Patch<Data>>> {
        val (patches) = patches.value as? Resource.Success ?: return listOf()
        return patches.filter { patch -> ids.any { it == patch.patchName } }
    }
}

@Parcelize
data class PatchClass(
    val patch: Class<out Patch<Data>>,
    val unsupported: Boolean,
    val hasPatchOptions: Boolean,
) : Parcelable