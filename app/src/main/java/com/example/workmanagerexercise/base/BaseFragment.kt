package com.example.workmanagerexercise.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment

abstract class BaseFragment<VBD: ViewDataBinding>(
  @LayoutRes private val layoutResId: Int
): Fragment() {
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? = DataBindingUtil.inflate<VBD>(
    inflater,
    layoutResId,
    container,
    false
  ).run {

    lifecycleOwner = this@BaseFragment

    bindingVM()
    bindingViewData()
    setEventListener()

    root
  }

  protected open fun VBD.bindingVM() {}
  protected open fun VBD.bindingViewData() {}
  protected open fun VBD.setEventListener() {}
}