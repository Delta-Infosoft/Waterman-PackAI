package com.waterman.packai.splash.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.waterman.packai.R
import com.waterman.packai.authentication.activity.AuthenticationActivity
import com.waterman.packai.databinding.ActivitySplashBinding
import com.waterman.packai.home.activity.HomeActivity
import com.waterman.packai.utils.EncryptedPrefHelper
import com.waterman.packai.utils.PrefKeys
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySplashBinding
    @Inject lateinit var encryptedPrefHelper: EncryptedPrefHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
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

        navigationToTutorialsScreen()
    }

    fun navigationToTutorialsScreen() {
        Handler(Looper.getMainLooper()).postDelayed({
            val isTutorialCompleted = encryptedPrefHelper.getBoolean(PrefKeys.IS_TUTORIAL_COMPLETED, false)
            val isLogin = encryptedPrefHelper.getBoolean(PrefKeys.IS_LOGIN, false)
            if (isLogin) {
                val intent = Intent(this@SplashActivity, HomeActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this@SplashActivity, AuthenticationActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, 2000)
    }
}