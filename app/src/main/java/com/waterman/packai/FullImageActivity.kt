package com.waterman.packai

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.waterman.packai.databinding.ActivityFullImageViewBinding
import java.io.File

class FullImageActivity : AppCompatActivity() {

    private lateinit var binding : ActivityFullImageViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullImageViewBinding.inflate(layoutInflater)
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

        val navigationBarView = findViewById<View>(R.id.navigationBarView)
        val navResourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (navResourceId > 0) {
            val navigationBarHeight = resources.getDimensionPixelSize(navResourceId)
            navigationBarView.layoutParams.height = navigationBarHeight
            navigationBarView.requestLayout()
        }

        val imagePath = intent.getStringExtra("image_uri")

        imagePath?.let { path ->
            Log.e("image_uri",path)
            Glide.with(this)
                .load(
                    when {
                        path.startsWith("http", true) -> path              // Remote URL
                        path.startsWith("content://") -> Uri.parse(path)  // Content Uri
                        path.startsWith("file://") -> Uri.parse(path)     // File Uri
                        else -> File(path)                                 // Absolute file path
                    }
                )
                .diskCacheStrategy(DiskCacheStrategy.NONE)  // skip disk cache
                .skipMemoryCache(true)
                .placeholder(R.drawable.ic_no_image)
                .error(R.drawable.ic_no_image)
                .into(binding.imageProduct)
        }

        binding.imgViewBack.setOnClickListener { finish() }

    }
}