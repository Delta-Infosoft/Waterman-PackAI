package com.waterman.packai.base

import android.widget.Toast
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class BaseFragment : Fragment() {
    fun loadFragment(fragment: Fragment, isAdd: Boolean, isAddBackStack: Boolean) {
        (activity as BaseActivity).loadFragment(
            fragment = fragment,
            isAdd = isAdd,
            isAddBackStack = isAddBackStack
        )
    }

    fun showToast(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()
    }



}