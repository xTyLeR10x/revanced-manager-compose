package app.revanced.manager.patcher.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import app.revanced.manager.R
import app.revanced.manager.Variables.patches
import app.revanced.manager.api.API
import app.revanced.manager.patcher.aapt.Aapt
import app.revanced.manager.patcher.aligning.ZipAligner
import app.revanced.manager.patcher.aligning.zip.ZipFile
import app.revanced.manager.patcher.aligning.zip.structures.ZipEntry
import app.revanced.manager.patcher.signing.Signer
import app.revanced.manager.preferences.PreferencesManager
import app.revanced.manager.ui.Resource
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.data.Data
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.util.patch.impl.DexPatchBundle
import dalvik.system.DexClassLoader
import java.io.File

class PatcherWorker(context: Context, parameters: WorkerParameters, private val api: API, private val prefs: PreferencesManager) :
    CoroutineWorker(context, parameters) {
    val tag = "ReVanced Manager"

    override suspend fun doWork(): Result {
        val selectedPatches = inputData.getStringArray("selectedPatches")
            ?: throw IllegalArgumentException("selectedPatches is missing")
        val patchBundleFile = inputData.getString("patchBundleFile")
            ?: throw IllegalArgumentException("patchBundleFile is missing")

        val notificationIntent = Intent(applicationContext, PatcherWorker::class.java)
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        val channel =
            NotificationChannel(
                "revanced-patcher-patching",
                "Patching",
                NotificationManager.IMPORTANCE_LOW
            )
        val notificationManager =
            ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)
        notificationManager!!.createNotificationChannel(channel)
        val notification: Notification = Notification.Builder(applicationContext, channel.id)
            .setContentTitle(applicationContext.getText(R.string.patcher_notification_title))
            .setContentText(applicationContext.getText(R.string.patcher_notification_message))
            .setLargeIcon(Icon.createWithResource(applicationContext, R.drawable.manager))
            .setSmallIcon(Icon.createWithResource(applicationContext, R.drawable.manager))
            .setContentIntent(pendingIntent)
            .build()

        setForeground(ForegroundInfo(1, notification))

        runPatcher(selectedPatches.toList(), patchBundleFile)
        return Result.success()
    }

    private suspend fun runPatcher(selectedPatches: List<String>, patchBundleFile: String): Boolean {

        val aaptPath = Aapt.binary(applicationContext).absolutePath
        val frameworkPath =
            applicationContext.filesDir.resolve("framework").also { it.mkdirs() }.absolutePath
        val integrationsCacheDir =
            applicationContext.filesDir.resolve("integrations-cache").also { it.mkdirs() }

        loadPatches(patchBundleFile)
        Log.d(tag, "Checking prerequisites")
        val patches = findPatchesByIds(selectedPatches)
        if (patches.isEmpty()) return true
        val integrations = downloadIntegrations(integrationsCacheDir)

        Log.d(tag, "Creating directories")
        val workdir = createWorkDir()
        val inputFile = File(workdir.parentFile!!, "base.apk")
        val patchedFile = File(workdir, "patched.apk")
        val alignedFile = File(workdir, "aligned.apk")
        val outputFile = File(workdir, "out.apk")
        val cacheDirectory = workdir.resolve("cache")

        try {
            //                Log.d(tag, "Copying base.apk from ${info.packageName}")
            //                withContext(Dispatchers.IO) {
            //                    Files.copy(
            //                        File(info.publicSourceDir).toPath(),
            //                        inputFile.toPath(),
            //                        StandardCopyOption.REPLACE_EXISTING
            //                    )
            //                }

            Log.d(tag, "Creating patcher")
            val patcher = Patcher(
                PatcherOptions(
                    inputFile,
                    cacheDirectory.absolutePath,
                    patchResources = true,
                    aaptPath = aaptPath,
                    frameworkFolderLocation = frameworkPath,
                    logger = object : app.revanced.patcher.logging.Logger {
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
                result.dexFiles.forEach { Log.d(tag, "Writing dex file ${it.name}")
                fs.addEntryCompressData(ZipEntry.createWithName(it.name), it.stream.readAllBytes()) }
                fs.copyEntriesFromFileAligned(ZipFile(inputFile), ZipAligner::getEntryAlignment)
            }
            Log.d(tag, "Signing apk")
            Signer("ReVanced", "s3cur3p@ssw0rd").signApk(alignedFile, outputFile)
            Log.i(tag,"Successfully patched into $outputFile")
        } catch (e: Exception) {
            Log.e(tag, "Error while patching", e)
        }

        Log.d(tag, "Deleting workdir")
        workdir.deleteRecursively()

        return false
    }

    //private fun installNonRoot(apk: File) {
    //    val intent = Intent(Intent.ACTION_VIEW);
    //    intent.setDataAndType(
    //        Uri.fromFile(apk), "application/vnd.android.package-archive"
    //    );
    //    applicationContext.startActivity(intent);
    //}

    private fun createWorkDir(): File {
        return applicationContext.filesDir.resolve("tmp-${System.currentTimeMillis()}")
            .also { it.mkdirs() }
    }

    private fun findPatchesByIds(ids: Iterable<String>): List<Class<out Patch<Data>>> {
        val (patches) = patches.value as? Resource.Success ?: return listOf()
        return patches.filter { patch -> ids.any { it == patch.patchName } }
    }

    private suspend fun downloadIntegrations(workdir: File): File {
        return try {
            val (_, out) = api.downloadFile(
                workdir,
                prefs.srcIntegrations.toString(),
                ".apk"
            )
            out
        } catch (e: Exception) {
            throw Exception("Failed to download integrations", e)
        }
    }

    private fun loadPatches(patchBundleFile: String) {
        try {
            loadPatches0(patchBundleFile)
        } catch (e: Exception) {
            Log.e(tag, "An error occurred while loading patches", e)
        }
    }

    private fun loadPatches0(path: String) {
        val patchClasses = DexPatchBundle(
            path, DexClassLoader(
                path,
                applicationContext.codeCacheDir.absolutePath,
                null,
                javaClass.classLoader
            )
        ).loadPatches()
        patches.value = Resource.Success(patchClasses)
    }
}