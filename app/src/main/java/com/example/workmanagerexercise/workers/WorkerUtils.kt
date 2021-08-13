package com.example.workmanagerexercise.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.workmanagerexercise.CHANNEL_ID
import com.example.workmanagerexercise.DELAY_TIME_MILLIS
import com.example.workmanagerexercise.NOTIFICATION_ID
import com.example.workmanagerexercise.NOTIFICATION_TITLE
import com.example.workmanagerexercise.OUTPUT_PATH
import com.example.workmanagerexercise.R
import com.example.workmanagerexercise.VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
import com.example.workmanagerexercise.VERBOSE_NOTIFICATION_CHANNEL_NAME
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

fun makeStatusNotification(message: String, context: Context) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    val name = VERBOSE_NOTIFICATION_CHANNEL_NAME
    val description = VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
    val importance = NotificationManager.IMPORTANCE_HIGH
    val channel = NotificationChannel(CHANNEL_ID, name, importance)
    channel.description = description

    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

    notificationManager?.createNotificationChannel(channel)
  }

  val builder = NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(R.drawable.ic_launcher_foreground)
    .setContentTitle(NOTIFICATION_TITLE)
    .setContentText(message)
    .setPriority(NotificationCompat.PRIORITY_HIGH)
    .setVibrate(LongArray(0))

  NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
}

fun sleep() {
  runBlocking {
    delay(DELAY_TIME_MILLIS)
  }
}

@WorkerThread
fun blurBitmap(bitmap: Bitmap, applicationContext: Context): Bitmap {
  lateinit var rsContext: RenderScript
  try {
    val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)

    rsContext = RenderScript.create(applicationContext, RenderScript.ContextType.DEBUG)

    val inAlloc = Allocation.createFromBitmap(rsContext, bitmap)
    val outAlloc = Allocation.createTyped(rsContext, inAlloc.type)
    ScriptIntrinsicBlur.create(rsContext, Element.U8_4(rsContext)).apply {
      setRadius(10f)
      setInput(inAlloc)
      forEach(outAlloc)
    }
    outAlloc.copyTo(output)

    return output
  } finally {
    rsContext.finish()
  }
}

@Throws(FileNotFoundException::class)
fun writeBitmapToFile(applicationContext: Context, bitmap: Bitmap): Uri {
  val name = String.format("blur-filter-output-%s.png", UUID.randomUUID().toString())
  val outputDir = File(applicationContext.filesDir, OUTPUT_PATH)
  if (!outputDir.exists()) {
    outputDir.mkdirs()
  }

  val outputFile = File(outputDir, name)

  var out: FileOutputStream? = null

  try {
    out = FileOutputStream(outputFile)
    bitmap.compress(Bitmap.CompressFormat.PNG, 0, out)
  } finally {
    out?.let {
      try {
        it.close()
      } catch (ignore: IOException) {
      }
    }
  }

  return Uri.fromFile(outputFile)
}