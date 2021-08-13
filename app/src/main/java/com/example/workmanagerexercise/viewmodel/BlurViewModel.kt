package com.example.workmanagerexercise.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.workmanagerexercise.IMAGE_MANIPULATION_WORK_NAME
import com.example.workmanagerexercise.KEY_IMAGE_URI
import com.example.workmanagerexercise.TAG_OUTPUT
import com.example.workmanagerexercise.workers.BlurWorker
import com.example.workmanagerexercise.workers.CleanupWorker
import com.example.workmanagerexercise.workers.SaveImageToFileWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BlurViewModel(application: Application) : AndroidViewModel(application) {
  private val workManager = WorkManager.getInstance(application)

  private val _imageUriFlow = MutableStateFlow<Uri?>(null)
  val imageUriFlow: StateFlow<Uri?>
    get() = _imageUriFlow

  internal fun setImageUri(uriString: String?) {
    _imageUriFlow.value = uriOrNull(uriString)
  }

  private val _outputUriFlow = MutableStateFlow<Uri?>(null)
  val outputUriFlow: StateFlow<Uri?>
    get() = _outputUriFlow

  internal fun setOutputUri(outputImageUri: String?) {
    _isApplyBlurInProgressFlow.value = false
    _outputUriFlow.value = uriOrNull(outputImageUri)
  }

  internal val outputWorkInfos: LiveData<List<WorkInfo>> =
    workManager.getWorkInfosByTagLiveData(TAG_OUTPUT)

  private val _isApplyBlurInProgressFlow = MutableStateFlow(false)
  val isApplyBlurInProgressFlow: StateFlow<Boolean>
    get() = _isApplyBlurInProgressFlow

  internal fun cancelWork() {
    workManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME)
    _isApplyBlurInProgressFlow.value = false
  }

  /**
   * Creates the input data bundle which includes the Uri to operate an
   * @return Data which contains the Image Uri as a String
   */
  private fun createInputDataForUri(): Data {
    val builder = Data.Builder()
    _imageUriFlow.value?.let { uri ->
      builder.putString(KEY_IMAGE_URI, uri.toString())
    }
    return builder.build()
  }

  /**
   * Create the WorkRequest to apply the blur and save the resulting image
   * @param blurLevel The amount to blur the image
   */
  internal fun applyBlur(blurLevel: Int) {
    _isApplyBlurInProgressFlow.value = true

    // Add WorkRequest to Cleanup temporary images
    var continuation = workManager
      .beginUniqueWork(
        IMAGE_MANIPULATION_WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        OneTimeWorkRequest.from(CleanupWorker::class.java)
      )

    // Add WorkRequests to blur the image the number of times requested
    for (i in 0 until blurLevel) {
      val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()

      // Input the Uri if this is the first blur operation
      // After the first blur operation the input will be the output of previous
      // blur operations.
      if (i == 0) {
        blurBuilder.setInputData(createInputDataForUri())
      }

      continuation = continuation.then(blurBuilder.build())
    }

    // Create charging constraint
    val constraints = Constraints.Builder()
      .setRequiresCharging(true)
      .build()

    // Add WorkRequest to save the image to the filesystem
    val save = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
      .setConstraints(constraints)
      .addTag(TAG_OUTPUT)
      .build()

    continuation = continuation.then(save)

    // Actually start the work
    continuation.enqueue()
  }

  private fun uriOrNull(uriString: String?): Uri? = when {
    !uriString.isNullOrEmpty() -> Uri.parse(uriString)
    else -> null
  }
}