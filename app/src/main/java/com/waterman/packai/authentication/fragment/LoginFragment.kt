package com.waterman.packai.authentication.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.viewModels
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.waterman.packai.authentication.viewmodel.LoginState
import com.waterman.packai.authentication.viewmodel.AuthenticationViewModel
import com.waterman.packai.base.BaseFragment
import com.waterman.packai.databinding.FragmentLoginBinding
import com.waterman.packai.home.activity.HomeActivity
import com.waterman.packai.utils.Constants
import com.waterman.packai.utils.Constants.getTrimmedText
import com.waterman.packai.utils.Constants.hideKeyboard
import com.waterman.packai.utils.Constants.setSafeOnClickListener
import com.waterman.packai.utils.EncryptedPrefHelper
import com.waterman.packai.utils.PrefKeys
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : BaseFragment() {

    private lateinit var binding: FragmentLoginBinding
    private val viewModel: AuthenticationViewModel by viewModels()
    @Inject lateinit var sharedPref: EncryptedPrefHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestFirebaseToken()
        setupClickListeners()
        observeLogin()

    }
    private fun requestFirebaseToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (token != null) {
                    sharedPref.saveFCMToken(token)
                }
                Log.d("FCM", token)
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }
    private fun setupClickListeners() = with(binding) {
        btnLogin.setSafeOnClickListener {
            if(txtUserName.getTrimmedText().isEmpty()){
                showToast("Please enter username")
            }else if(txtPassword.getTrimmedText().isEmpty()){
                showToast("Please enter password")
            }else{
                hideKeyboard(it)
                txtUserName.clearFocus()
                txtPassword.clearFocus()
                callLoginApi()
            }
        }
    }
    private fun callLoginApi() = with(binding) {
        viewModel.login(
            userName = txtUserName.getTrimmedText(),
            password = txtPassword.getTrimmedText(),
        )
    }
    private fun observeLogin() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                LoginState.Loading -> {
                    setLoginEnabled(false)
                    binding.constViewApprovalCode.visibility = View.GONE
                    showLoader()
                }

                is LoginState.Success -> {
                    binding.constViewApprovalCode.visibility = View.GONE
                    hideLoader()
                    navigateToHome()
                }

                is LoginState.ApprovalRequired -> {
                    hideLoader()
                    setLoginEnabled(true)

                    binding.constViewApprovalCode.visibility = View.VISIBLE
                    binding.editTextApprovalCode.setText(Constants.getDeviceId(requireContext()))
                    showToast(state.message)
                }

                is LoginState.Error -> {
                    binding.constViewApprovalCode.visibility = View.GONE
                    hideLoader()
                    setLoginEnabled(true)
                    showToast(state.message)
                }

                LoginState.Idle -> {
                    binding.constViewApprovalCode.visibility = View.GONE
                    hideLoader()
                    setLoginEnabled(true)
                }
            }
        }
    }
    private fun navigateToHome() {
        sharedPref.putBoolean(PrefKeys.IS_LOGIN, true)
        startActivity(Intent(requireContext(), HomeActivity::class.java))
        requireActivity().finish()
    }
    private fun showLoader() {
        binding.progressBarLogin.visibility = View.VISIBLE
        requireActivity().window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        binding.constLayoutMain.alpha = 0.5f
    }
    private fun hideLoader() {
        binding.constLayoutMain.alpha = 1f
        requireActivity().window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        binding.progressBarLogin.visibility = View.GONE
    }
    private fun setLoginEnabled(enabled: Boolean) {
        binding.btnLogin.isEnabled = enabled
        binding.btnLogin.alpha = if (enabled) 1f else 0.6f
    }

}