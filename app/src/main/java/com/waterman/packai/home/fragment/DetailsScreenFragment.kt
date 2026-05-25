package com.waterman.packai.home.fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.viewModels
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.appbar.MaterialToolbar
import com.waterman.packai.FullImageActivity
import com.waterman.packai.R
import com.waterman.packai.base.BaseFragment
import com.waterman.packai.databinding.FragmentDetailsScreenBinding
import com.waterman.packai.home.activity.HomeActivity
import com.waterman.packai.home.viewmodel.GetUploadedPhotoState
import com.waterman.packai.home.viewmodel.HomeViewModel
import com.waterman.packai.home.viewmodel.UpdatePackAIState
import com.waterman.packai.network.request.UpdatePackAIRequest
import com.waterman.packai.network.response.PhotoItem
import com.waterman.packai.network.response.ProductList
import com.waterman.packai.utils.Constants
import com.waterman.packai.utils.Constants.getTrimmedText
import com.waterman.packai.utils.Constants.setSafeOnClickListener
import com.waterman.packai.utils.Constants.showConfirmDialog
import com.waterman.packai.utils.Constants.showSerialMismatchDialog
import com.waterman.packai.utils.EncryptedPrefHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DetailsScreenFragment : BaseFragment() {
    private lateinit var binding: FragmentDetailsScreenBinding
    @Inject lateinit var sharedPref: EncryptedPrefHelper
    private var pumpImageUrl: String? = null
    private var motorImageUrl: String? = null
    private var bodyImageUrl: String? = null
    private var mainImageUrl: String? = null

    private var motorMainImageUrl: String? = null
    private var pumpSetImageUrl: String? = null
    private val viewModel: HomeViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDetailsScreenBinding.inflate(inflater, container, false)
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
        setUpInitData()
        moveOnClickListeners()

        observeGetPhotoData()
        observeUpdatePackAIData()
    }
    private fun moveOnClickListeners() = with(binding){
        imgUpload.setSafeOnClickListener {
            mainImageUrl?.let { bitmap ->
                val intent = Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("image_uri", mainImageUrl.toString())
                startActivity(intent)
            }
        }
        imgViewMidCropPump.setSafeOnClickListener {
            pumpImageUrl?.let { bitmap ->
                val intent = Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("image_uri", pumpImageUrl.toString())
                startActivity(intent)
            }
        }
        imgViewMidCropMotor.setSafeOnClickListener {
            motorImageUrl?.let { bitmap ->
                val intent = Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("image_uri", motorImageUrl.toString())
                startActivity(intent)
            }
        }
        imgViewTopCrop.setSafeOnClickListener {
            bodyImageUrl?.let { bitmap ->
                val intent = Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("image_uri", bodyImageUrl.toString())
                startActivity(intent)
            }
        }
        imgUploadMotor.setSafeOnClickListener {
            motorMainImageUrl?.let { bitmap ->
                val intent = Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("image_uri", motorMainImageUrl.toString())
                startActivity(intent)
            }
        }
        imgUploadPumpSet.setSafeOnClickListener {
            pumpSetImageUrl?.let { bitmap ->
                val intent = Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("image_uri", pumpSetImageUrl.toString())
                startActivity(intent)
            }
        }
    }
    private fun isImageLoaded(imageView: AppCompatImageView): Boolean {
        val drawable = imageView.drawable ?: return false

        return drawable !is VectorDrawable && drawable !is VectorDrawableCompat
    }
    private fun manageToolBar() {
        (activity as HomeActivity).apply {
            manageToolBar(isVisible = true)
            manageToolBarTitle("Details")
            manageBackButtonClick(true)
            manageDrawerLock(false)
            setDrawerEnabled(false)
        }
    }
    private fun setUpInitData() = with(binding) {
        val checkItem = arguments?.getParcelable<ProductList>("CHECK_ITEM")
        checkItem?.let { checkItem ->
            Log.e("CHECK_ITEM",checkItem.toString())
            checkItem.PackAIEntryId?.let {
                viewModel.getUploadedPhotoAPI(recordId = it, formType = "")
                viewModel.getUploadedPhotoAPI(recordId = it, formType = "PackAI OtherPhoto")
            }
            txtSerialNumber.setText(checkItem.SrNo)
            txtSoNo.setText(checkItem.SONo)
            txtItemName.setText(checkItem.ItemName)
            txtCustomerName.setText(checkItem.CustomerName)
            txtSoRemark.setText(checkItem.Remarks)

            updateStatusUI(
                status = checkItem.TopBodyStatus,
                textView = txtBodyScan,
                matchedText = "Logo scan matched",
                notMatchedText = "Logo scan mismatched"
            )
            updateStatusUI(
                status = checkItem.MotorSerialNoStatus,
                textView = txtMotorSerialNo,
                matchedText = "Motor serial number matched\n(Extracted:${checkItem.ExtractedMotorSrNo})",
                notMatchedText = "Motor serial number mismatched\n(Extracted:${checkItem.ExtractedMotorSrNo})"
            )
            updateStatusUI(
                status = checkItem.PumpSerialNoStatus,
                textView = txtPumpSerialNo,
                matchedText = "Pump serial number matched\n(Extracted:${checkItem.ExtractedPumpSrNo})",
                notMatchedText = "Pump serial number mismatched\n(Extracted:${checkItem.ExtractedPumpSrNo})"
            )
            updateStatusUI(
                status = checkItem.PumpTypeStatus,
                textView = binding.txtPumpTypeText,
                matchedText = "Pump type matched\n(Extracted:${checkItem.ExtractedPumpType})",
                notMatchedText = "Pump type mismatched\n(Extracted:${checkItem.ExtractedPumpType})"
            )
            updateStatusUI(
                status = checkItem.MotorTypeStatus,
                textView = binding.txtMotorTypeText,
                matchedText = "Motor type matched\n(Extracted:${checkItem.ExtractedMotorType})",
                notMatchedText = "Motor type mismatched\n(Extracted:${checkItem.ExtractedMotorType})"
            )

            txtPumpSerialNo.setSafeOnClickListener {
                val user = sharedPref.getUser()
                if(user?.IsAdmin?.equals("True") != true){
                    return@setSafeOnClickListener
                }
                showSerialMismatchDialog(
                    context = requireContext(),
                    title = getString(R.string.title_pump_sr_no),
                    ierpSrNo = txtSerialNumber.getTrimmedText(),
                    scannedSrNo = checkItem.ExtractedPumpSrNo ?: "",
                    status = checkItem.PumpSerialNoStatus,
                    mismatchMessage = when (checkItem.PumpSerialNoStatus) {
                        "OK" -> "Serial number matched successfully"
                        "FAILED" -> "Serial number mismatch"
                        else -> "Unknown status"
                    },
                    onConfirm = { finalValue ->
                        Constants.hideKeyboard(it)
                        if(txtSerialNumber.getTrimmedText() != finalValue.trim()){
                            showConfirmDialog(
                                context = requireContext(),
                                title = getString(R.string.title_pump_sr_no),
                                message = "Pump sr no not matched with the entered value, Are you sure want to force match?",
                                onOk = {
                                    val request = UpdatePackAIRequest(
                                        packAIEntryId = checkItem.PackAIEntryId,
                                        userId = checkItem.InsertedbyUserId,
                                        type = "PumpSrNo",
                                        pumpSerialNoStatus = "OK",
                                        extractedPumpSrNo = finalValue.trim()
                                    )
                                    viewModel.updatePackAIData(request)
                                }
                            )
                        }else {
                            val request = UpdatePackAIRequest(
                                packAIEntryId = checkItem.PackAIEntryId,
                                userId = checkItem.InsertedbyUserId,
                                type = "PumpSrNo",
                                pumpSerialNoStatus = "OK",
                                extractedPumpSrNo = finalValue.trim()
                            )
                            viewModel.updatePackAIData(request)
                        }
                    }
                )
            }
            txtMotorSerialNo.setSafeOnClickListener {
                val user = sharedPref.getUser()
                if(user?.IsAdmin?.equals("True") != true){
                    return@setSafeOnClickListener
                }
                val status = checkItem.MotorSerialNoStatus
                val expected = txtSerialNumber.getTrimmedText()
                val extracted = checkItem.ExtractedMotorSrNo ?: ""

                showSerialMismatchDialog(
                    context = requireContext(),
                    title = getString(R.string.title_motor_sr_no),
                    ierpSrNo = expected,
                    scannedSrNo = extracted,
                    status = status,
                    mismatchMessage = when (status) {
                        "OK" -> "Serial number matched successfully"
                        "FAILED" -> "Serial number mismatch"
                        else -> getString(R.string.msg_serial_unknown)
                    },
                    onConfirm = { finalValue ->
                        Constants.hideKeyboard(it)
                        if(txtSerialNumber.getTrimmedText() != finalValue.trim()){
                            showConfirmDialog(
                                context = requireContext(),
                                title = getString(R.string.title_pump_sr_no),
                                message = "Motor sr no not matched with the entered value, Are you sure want to force match?",
                                onOk = {
                                    val request = UpdatePackAIRequest(
                                        packAIEntryId = checkItem.PackAIEntryId,
                                        userId = checkItem.InsertedbyUserId,
                                        type = "MotorSrNo",
                                        motorSerialNoStatus = "OK",
                                        extractedMotorSrNo = finalValue.trim()
                                    )
                                    viewModel.updatePackAIData(request)
                                }
                            )
                        }else {
                            val request = UpdatePackAIRequest(
                                packAIEntryId = checkItem.PackAIEntryId,
                                userId = checkItem.InsertedbyUserId,
                                type = "MotorSrNo",
                                motorSerialNoStatus = "OK",
                                extractedMotorSrNo = finalValue.trim()
                            )
                            viewModel.updatePackAIData(request)
                        }
                    }
                )
            }
            txtBodyScan.setSafeOnClickListener {
                val status = checkItem.TopBodyStatus
                val expected = txtCustomerName.getTrimmedText()
                val extracted = checkItem.FullAiTextLogo ?: ""

                showSerialMismatchDialog(
                    context = requireContext(),
                    title = getString(R.string.title_logo),
                    ierpSrNo = expected,
                    scannedSrNo = extracted,
                    status = status,
                    mismatchMessage = when (status) {
                        "OK" -> "Logo scan matched"
                        "FAILED" -> "Logo scan mismatched"
                        else -> getString(R.string.msg_logo_unknown)
                    },
                    onConfirm = { }
                )
            }
            txtPumpTypeText.setSafeOnClickListener {
                val user = sharedPref.getUser()
                if(user?.IsAdmin?.equals("True") != true){
                    return@setSafeOnClickListener
                }
                val status = checkItem.PumpTypeStatus
                val expected = txtItemName.getTrimmedText()
                val extracted = checkItem.ExtractedPumpType ?: ""

                showSerialMismatchDialog(
                    context = requireContext(),
                    title = getString(R.string.title_pump_type),
                    ierpSrNo = expected,
                    scannedSrNo = extracted,
                    status = status,
                    mismatchMessage = when (status) {
                        "OK" -> "Pump type matched"
                        "FAILED" -> "Pump type mismatched"
                        else -> getString(R.string.msg_type_unknown)
                    },
                    onConfirm = { finalValue ->
                        Constants.hideKeyboard(it)
                        if(txtItemName.getTrimmedText() != finalValue.trim()){
                            showConfirmDialog(
                                context = requireContext(),
                                title = getString(R.string.title_pump_sr_no),
                                message = "Pump type value not matched with the entered value, Are you sure want to force match?",
                                onOk = {
                                    val request = UpdatePackAIRequest(
                                        packAIEntryId = checkItem.PackAIEntryId,
                                        userId = checkItem.InsertedbyUserId,
                                        type = "PumpType",
                                        pumpTypeStatus = "OK",
                                        extractedPumpType = finalValue.trim()
                                    )
                                    viewModel.updatePackAIData(request)
                                }
                            )
                        }else {
                            val request = UpdatePackAIRequest(
                                packAIEntryId = checkItem.PackAIEntryId,
                                userId = checkItem.InsertedbyUserId,
                                type = "PumpType",
                                pumpTypeStatus = "OK",
                                extractedPumpType = finalValue.trim()
                            )
                            viewModel.updatePackAIData(request)
                        }
                    }
                )
            }
            txtMotorTypeText.setSafeOnClickListener {
                val user = sharedPref.getUser()
                if(user?.IsAdmin?.equals("True") != true){
                    return@setSafeOnClickListener
                }
                val status = checkItem.MotorTypeStatus
                val expected = txtItemName.getTrimmedText()
                val extracted = checkItem.ExtractedMotorType ?: ""

                showSerialMismatchDialog(
                    context = requireContext(),
                    title = getString(R.string.title_motor_type),
                    ierpSrNo = expected,
                    scannedSrNo = extracted,
                    status = status,
                    mismatchMessage = when (status) {
                        "OK" -> "Motor type matched"
                        "FAILED" -> "Motor type mismatched"
                        else -> getString(R.string.msg_type_unknown)
                    },
                    onConfirm = { finalValue ->
                        Constants.hideKeyboard(it)
                        if(txtItemName.getTrimmedText() != finalValue.trim()){
                            showConfirmDialog(
                                context = requireContext(),
                                title = getString(R.string.title_pump_sr_no),
                                message = "Motor type value not matched with the entered value, Are you sure want to force match?",
                                onOk = {
                                    val request = UpdatePackAIRequest(
                                        packAIEntryId = checkItem.PackAIEntryId,
                                        userId = checkItem.InsertedbyUserId,
                                        type = "MotorType",
                                        motorTypeStatus = "OK",
                                        extractedMotorType = finalValue.trim()
                                    )
                                    viewModel.updatePackAIData(request)
                                }
                            )
                        }else {
                            val request = UpdatePackAIRequest(
                                packAIEntryId = checkItem.PackAIEntryId,
                                userId = checkItem.InsertedbyUserId,
                                type = "MotorType",
                                motorTypeStatus = "OK",
                                extractedMotorType = finalValue.trim()
                            )
                            viewModel.updatePackAIData(request)
                        }
                    }
                )
            }
        }
    }
    private fun updateStatusUI(status: String?, textView: AppCompatTextView, matchedText: String, notMatchedText: String) {
        val isMatched = status.orEmpty().contains("ok", true)

        val iconRes = if (isMatched) R.drawable.ic_verified else R.drawable.ic_danger
        val tintColor = ContextCompat.getColor(requireContext(), if (isMatched) R.color.white else R.color.red_1)
        val bgRes = if (isMatched) R.drawable.bg_rounded_fill_secondary else R.drawable.bg_rounded_fill_red
        textView.setTextColor(tintColor)
        textView.setBackgroundResource(bgRes)
        val drawable = ContextCompat.getDrawable(requireContext(), iconRes)?.mutate()
        drawable?.setTint(tintColor)

        textView.apply {
            text = if (isMatched) matchedText else notMatchedText
            setCompoundDrawablesWithIntrinsicBounds(drawable, null, ContextCompat.getDrawable(requireContext(),  R.drawable.ic_info)?.mutate(), null)
        }
    }
    private fun loadImage(url: String?, imageView: AppCompatImageView) {
        Glide.with(imageView.context)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.NONE)  // skip disk cache
            .skipMemoryCache(true)                       // skip memory cache
            .placeholder(R.drawable.ic_no_image)
            .error(R.drawable.ic_no_image)
            .into(imageView)
    }
    private fun showPhotoList(list: List<PhotoItem>) {
        binding.tvPhotoCount.text = list.size.toString()
        binding.lylAttachmentPhotoListRow.visibility = View.VISIBLE
        renderAttachmentList(
            parent = binding.lylAttachmentPhotoListRow,
            list = list
        )
    }
    private fun renderAttachmentList(parent: LinearLayout, list: List<PhotoItem>) {
        parent.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())

        list.forEach { model ->
            val view = inflater.inflate(R.layout.layout_upload_photo, parent, false)

            val fileName = view.findViewById<TextView>(R.id.fileName)
            val downloadAttach = view.findViewById<ImageView>(R.id.downloadAttach)
            val deleteBtn = view.findViewById<ImageView>(R.id.imgDelete)
            deleteBtn.visibility = View.GONE
            fileName.text = model.filePath ?: "Unknown file"

            downloadAttach.setOnClickListener {
                openAttachment(model.filePath)
            }

            parent.addView(view)
        }
    }
    private fun openAttachment(fileUrl: String?) {
        if (fileUrl.isNullOrEmpty()) {
            showToast("File path is invalid")
            return
        }

        try {
            val uri = Uri.parse(fileUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Unable to open file")
        }
    }
    private fun observeGetPhotoData() {
        viewModel.photoState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is GetUploadedPhotoState.Loading -> showLoader()
                is GetUploadedPhotoState.Success -> {
                    hideLoader()
                    when (state.formType) {
                        "" -> {
                            // Main photos
                            state.data.forEach { item ->
                                when (item.formName) {
                                    "PackAI TopPhoto" -> {
                                        bodyImageUrl = item.filePath
                                        loadImage(item.filePath, binding.imgViewTopCrop)
                                    }
                                    "PackAI MotorPhoto" -> {
                                        motorImageUrl = item.filePath
                                        loadImage(item.filePath, binding.imgViewMidCropMotor)
                                    }
                                    "PackAI PumpPhoto" -> {
                                        pumpImageUrl = item.filePath
                                        loadImage(item.filePath, binding.imgViewMidCropPump)
                                    }
                                    "PackAI MainPhoto" -> {
                                        mainImageUrl = item.filePath
                                        loadImage(item.filePath, binding.imgUpload)
                                    }

                                    "PackAI MotorMainPhoto" -> {
                                        motorMainImageUrl = item.filePath
                                        loadImage(item.filePath, binding.imgUploadMotor)
                                    }
                                    "PackAI PumpSetPhoto" -> {
                                        pumpSetImageUrl = item.filePath
                                        loadImage(item.filePath, binding.imgUploadPumpSet)
                                    }
                                }
                            }
                        }
                        "PackAI OtherPhoto" -> {
                            showPhotoList(state.data)
                        }
                    }
                }
                is GetUploadedPhotoState.Empty -> {
                    hideLoader()
                    if (state.formType == "PackAI OtherPhoto") {
                        binding.lylAttachmentPhotoListRow.visibility = View.GONE
                    }
                }
                is GetUploadedPhotoState.Error -> {
                    hideLoader()
                    showToast(state.message)
                }
                else -> {}
            }
        }
    }

    private fun observeUpdatePackAIData() {
        viewModel.updatePackAIState.observe(viewLifecycleOwner) { state ->

            when (state) {

                is UpdatePackAIState.Idle -> {}

                is UpdatePackAIState.Loading -> {
                    showLoader()
                }

                is UpdatePackAIState.Success -> {
                    hideLoader()
                    showToast(state.message)
                    parentFragmentManager.popBackStackImmediate()
                }

                is UpdatePackAIState.Empty -> {
                    hideLoader()
                    showToast(state.message)
                }

                is UpdatePackAIState.Error -> {
                    hideLoader()
                    showToast(state.message)
                }
            }
        }
    }
    private fun showLoader() = with(binding){
        requireActivity().window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        progressBar.visibility = View.VISIBLE
        binding.constLayoutMain.alpha = 0.5f
    }
    private fun hideLoader() = with(binding){
        requireActivity().window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        progressBar.visibility = View.GONE
        binding.constLayoutMain.alpha = 1f
    }
}