package com.waterman.packai.home.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.google.android.material.appbar.MaterialToolbar
import com.waterman.packai.R
import com.waterman.packai.base.BaseFragment
import com.waterman.packai.databinding.FragmentProfileBinding
import com.waterman.packai.home.activity.HomeActivity
import com.waterman.packai.utils.Constants.setSafeOnClickListener
import com.waterman.packai.utils.EncryptedPrefHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : BaseFragment() {

    private lateinit var binding : FragmentProfileBinding
    @Inject lateinit var sharedPref: EncryptedPrefHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater,container,false)
        return binding.root
    }
    override fun onResume() {
        super.onResume()
        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.toolbarHome)
        toolbar.background = null  // ✅ Removes background completely

        val logOutIcon = requireActivity().findViewById<AppCompatImageView>(R.id.imgViewLogOut)
        val toolbarTitle = requireActivity().findViewById<AppCompatTextView>(R.id.txtViewTitle)
        toolbarTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        ImageViewCompat.setImageTintList(logOutIcon,
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.black))
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        manageToolBar()
        setInitData()
        moveOnClickListeners()
    }

    private fun moveOnClickListeners() = with(binding){

    }

    private fun setInitData() = with(binding){
        val user = sharedPref.getUser()
        user?.let {
           txtFullName.text = it.FirstName + it.LastName ?: "-"
           txtUserName.text = it.UsersName ?: "-"
           txtMobile.setText(it.MobileNo ?: "-")
           txtEmail.setText(it.EmailId ?: "-")
           txtIMEI.setText(it.IMEICode ?: "-")

           txtIsAdmin.setText(if (it.IsAdmin.equals("True")) "Admin" else "User")

           txtLastLogin.setText(it.LastLoginDateTime ?: "-")
           txtLastLogout.setText(it.LastLogOutDateTime ?: "-")
        }
    }

    private fun manageToolBar() {
        (activity as HomeActivity).apply {
            manageToolBar(isVisible = true)
            manageToolBarTitle("Profile")
            manageBackButtonClick(true)
            manageDrawerLock(false)
            setDrawerEnabled(false)
            logOutButtonManage(true)
        }
    }


}