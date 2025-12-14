package com.safe.setting.app.utils

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.safe.setting.app.workers.UploadWorker
import java.util.concurrent.TimeUnit

object WorkUtils {
    private const val TAG = "WorkUtils"

    fun enqueueUpload(context: Context, filePath: String, fileType: String, randomName: String) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val work = OneTimeWorkRequestBuilder<UploadWorker>()
                .setInputData(workDataOf(
                    UploadWorker.KEY_FILE_PATH to filePath,
                    UploadWorker.KEY_FILE_TYPE to fileType,
                    UploadWorker.KEY_RANDOM_NAME to randomName
                ))
                .addTag("UPLOAD_WORK")
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueue(work)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue upload: ${e.message}")
        }
    }
}
