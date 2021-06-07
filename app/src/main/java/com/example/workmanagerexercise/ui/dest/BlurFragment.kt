package com.example.workmanagerexercise.ui.dest

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.workmanagerexercise.KEY_IMAGE_URI
import com.example.workmanagerexercise.R
import com.example.workmanagerexercise.base.BaseFragment
import com.example.workmanagerexercise.databinding.FragmentBlurBinding
import com.example.workmanagerexercise.viewmodel.BlurViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class BlurFragment: BaseFragment<FragmentBlurBinding>(
  R.layout.fragment_blur
) {
  private val viewModel by viewModels<BlurViewModel>()

  private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { grant ->
    if (!grant) return@registerForActivityResult

    imageRequestLauncher.launch("image/*")
  }

  private val imageRequestLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    viewModel.setImageUri(uri.toString())
  }

  override fun FragmentBlurBinding.bindingVM() {
    CoroutineScope(Dispatchers.Main).run {
      launch {
        viewModel.isApplyBlurInProgressFlow.collect {
          isInApplyingProgress = it
        }
      }

      launch {
        viewModel.imageUriFlow.collect { uri ->
          Glide.with(this@BlurFragment)
            .clear(ivImage)

          Glide.with(this@BlurFragment)
            .load(uri)
            .into(ivImage)
        }
      }

      launch {
        viewModel.outputUriFlow.collect { uri ->
          isBlurredImageExist = true

          Glide.with(this@BlurFragment)
            .load(uri)
            .into(ivImage)
        }
      }
    }

    viewModel.outputWorkInfos.observe(lifecycleOwner!!, { workInfos ->
      if (workInfos.isNullOrEmpty()) return@observe

      val workInfo = workInfos[0]

      if (!workInfo.state.isFinished) return@observe

      val outputImageUri = workInfo.outputData.getString(KEY_IMAGE_URI)

      if (!outputImageUri.isNullOrEmpty()) {
        viewModel.setOutputUri(outputImageUri)
      }
    })
  }

  override fun FragmentBlurBinding.setEventListener() {
    ivImage.setOnClickListener {
      context ?: return@setOnClickListener

      when (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
        PackageManager.PERMISSION_GRANTED -> {
          imageRequestLauncher.launch("image/*")
        }
        else -> {
          requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
      }
    }

    btnApply.setOnClickListener {
      val blurLevel = when (radioGroup.checkedRadioButtonId) {
        R.id.radioBlurLv1 -> 1
        R.id.radioBlurLv2 -> 2
        R.id.radioBlurLv3 -> 3
        else -> 1
      }

      viewModel.applyBlur(blurLevel)
    }

    btnCancel.setOnClickListener {
      viewModel.cancelWork()
    }
  }

  private fun selectImage() {

  }
}