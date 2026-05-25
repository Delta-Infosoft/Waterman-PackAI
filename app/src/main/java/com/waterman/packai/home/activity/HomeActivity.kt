package com.waterman.packai.home.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import com.waterman.packai.R
import com.waterman.packai.authentication.activity.AuthenticationActivity
import com.waterman.packai.authentication.viewmodel.AuthenticationViewModel
import com.waterman.packai.authentication.viewmodel.LogoutState
import com.waterman.packai.base.BaseActivity
import com.waterman.packai.databinding.ActivityHomeBinding
import com.waterman.packai.home.fragment.HomeFragment
import com.waterman.packai.home.fragment.ProfileFragment
import com.waterman.packai.utils.Constants
import com.waterman.packai.utils.Constants.setSafeOnClickListener
import com.waterman.packai.utils.Constants.showConfirmDialog
import com.waterman.packai.utils.EncryptedPrefHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : BaseActivity() {

    private lateinit var binding : ActivityHomeBinding
    private val viewModel: AuthenticationViewModel by viewModels()
    @Inject lateinit var sharedPref: EncryptedPrefHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
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

        val navigationBarView = findViewById<View>(R.id.navigationBarView)
        val navResourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (navResourceId > 0) {
            val navigationBarHeight = resources.getDimensionPixelSize(navResourceId)
            navigationBarView.layoutParams.height = navigationBarHeight
            navigationBarView.requestLayout()
        }

        if (savedInstanceState == null) {
            loadFragment(fragment = HomeFragment(), isAdd = false, isAddBackStack = false)
        }

        //loadFragment(fragment = HomeFragment(), isAdd = true, isAddBackStack = false)

        observeLogOutApiData()
    }

    fun manageHomeToolbar(){
        binding.imgViewLogOut.visibility = View.GONE
        binding.imgViewFilter.visibility = View.VISIBLE

        binding.imgViewFilter.setSafeOnClickListener {
            filterClickListener?.onFilterClicked()        }
    }
    private var filterClickListener: OnFilterClickListener? = null

    fun setFilterClickListener(listener: OnFilterClickListener?) {
        filterClickListener = listener
    }

    interface OnFilterClickListener {
        fun onFilterClicked()
    }

    fun manageToolBar(isVisible : Boolean) = with(binding){
        if(isVisible){
            toolbarHome.visibility = View.VISIBLE
        }else{
            toolbarHome.visibility = View.GONE
        }
    }

    fun logOutButtonManage(isVisible : Boolean) = with(binding){
        if(isVisible){
            imgViewLogOut.visibility = View.VISIBLE
        }else{
            imgViewLogOut.visibility = View.GONE
        }
    }

    fun manageDrawerLock(isDrawerVisible: Boolean) {
        if (isDrawerVisible) {
            binding.imgViewMenu.visibility = View.VISIBLE
            binding.imgViewMenu.setSafeOnClickListener {
                loadFragment(fragment = ProfileFragment(), isAdd = false, isAddBackStack = true)
            }
            binding.imgViewLogOut.setSafeOnClickListener {
                showConfirmDialog(
                    context = this@HomeActivity,
                    title = "Logout",
                    message = "Are you sure want to logged out from this device?",
                    onOk = {
                        Log.e("LogOut Activity","true")

                        Log.e("Logout","true")
                        val user = sharedPref.getUser()
                        viewModel.logout(
                            userName = user?.UsersName ?: "",
                            imei = Constants.getDeviceId(this@HomeActivity)
                        )
                    }
                )
            }
            binding.imgViewBack.visibility = View.GONE
        } else {
            binding.imgViewMenu.visibility = View.GONE
            binding.imgViewBack.visibility = View.VISIBLE
        }
    }

    fun manageBackButtonClick(isVisible: Boolean) = with(binding){
        if (isVisible) {
            imgViewBack.visibility = View.VISIBLE
            imgViewBack.setSafeOnClickListener { supportFragmentManager.popBackStackImmediate() }
        } else {
            imgViewBack.visibility = View.GONE
        }
    }

    fun manageToolBarTitle(title : String){
        binding.txtViewTitle.text = title
    }

    fun isIconVisible(isVisible: Boolean) = with(binding){
        if (isVisible) {
            binding.imgViewBack.visibility = View.VISIBLE
        } else {
            binding.imgViewBack.visibility = View.GONE
        }
    }

    fun setDrawerEnabled(enabled: Boolean) {
        val drawerLayout = binding.drawerLayout
        drawerLayout.setDrawerLockMode(
            if (enabled)
                DrawerLayout.LOCK_MODE_UNLOCKED
            else
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        )
    }

    private fun showLoader() {
        binding.progressBarHomeActivity.visibility = View.VISIBLE
    }
    private fun hideLoader() {
        binding.progressBarHomeActivity.visibility = View.GONE
    }

    private fun observeLogOutApiData() {
        Log.e("LogOut Click","True")
        viewModel.logoutState.observe(this) { state ->
            when (state) {
                is LogoutState.Loading -> showLoader()

                is LogoutState.Success -> {
                    hideLoader()

                    sharedPref.clear()
                    viewModel.clearLocalData()
                    val intent = Intent(this, AuthenticationActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }

                is LogoutState.Error -> {
                    hideLoader()
                    showToast(state.message)
                }

                else -> Unit
            }
        }
    }


}