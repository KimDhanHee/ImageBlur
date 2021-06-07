package com.example.workmanagerexercise.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.workmanagerexercise.KEY_IMAGE_URI
import timber.log.Timber
import java.lang.IllegalArgumentException

class BlurWorker(context: Context, params: WorkerParameters): Worker(context, params) {
  override fun doWork(): Result {
    val resourceUri = inputData.getString(KEY_IMAGE_URI)

    makeStatusNotification("Blurring Image", applicationContext)
    sleep()

    return try {
      if (TextUtils.isEmpty(resourceUri)) {
        Timber.e("Invalid input uri")
        throw IllegalArgumentException("Invalid input uri")
      }

      val resolver = applicationContext.contentResolver

      val picture = BitmapFactory.decodeStream(resolver.openInputStream(Uri.parse(resourceUri)))

      val output = blurBitmap(picture, applicationContext)

      val outputUri = writeBitmapToFile(applicationContext, output)

      val outputData = workDataOf(KEY_IMAGE_URI to outputUri.toString())

      Result.success(outputData)
    } catch (throwable: Throwable) {
      Timber.e(throwable, "Error applying blur")
      Result.failure()
    }
  }
}