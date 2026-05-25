package com.waterman.packai.authentication.activity

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.waterman.packai.R
import com.waterman.packai.authentication.fragment.LoginFragment
import com.waterman.packai.base.BaseActivity
import com.waterman.packai.databinding.ActivityAuthenticationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthenticationActivity : BaseActivity() {

    private lateinit var binding : ActivityAuthenticationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        // Step 1: Make status bar transparent
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(binding.root)

        // Step 2: Get status bar height and apply to view
        val statusBarView = findViewById<View>(R.id.statusBarView)

        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            val statusBarHeight = resources.getDimensionPixelSize(resourceId)
            statusBarView.layoutParams.height = statusBarHeight
            statusBarView.requestLayout()
        }
        loadFragment(LoginFragment(), isAdd = true, isAddBackStack = false)
    }

}