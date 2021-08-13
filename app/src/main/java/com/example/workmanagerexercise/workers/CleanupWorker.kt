package com.example.workmanagerexercise.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.workmanagerexercise.OUTPUT_PATH
import timber.log.Timber
import java.io.File
import java.lang.Exception

/**
 * Cleans up temporary files generated during blurring process
 */
class CleanupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
  override fun doWork(): Result {
    // Makes a notification when the work starts and slows down the work so that
    // it's easier to see each WorkRequest start, even on emulated devices
    makeStatusNotification("Cleaning up old temporary files", applicationContext)
    sleep()

    return try {
      val outputDirectory = File(applicationContext.filesDir, OUTPUT_PATH)

      if (outputDirectory.exists()) {
        outputDirectory.listFiles()?.forEach { entry ->
          val name = entry.name

          if (name.isNotEmpty() && name.endsWith(".png")) {
            val deleted = entry.delete()
            Timber.i("Deleted $name - $deleted")
          }
        }
      }

      Result.success()
    } catch (exception: Exception) {
      Timber.e(exception)
      Result.failure()
    }
  }
}