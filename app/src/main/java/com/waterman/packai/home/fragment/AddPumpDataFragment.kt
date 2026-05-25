package com.waterman.packai.home.fragment

import ProductCodeExtractor
import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.VectorDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.waterman.packai.FullImageActivity
import com.waterman.packai.R
import com.waterman.packai.base.BaseFragment
import com.waterman.packai.databinding.FragmentAddPumpDataBinding
import com.waterman.packai.home.activity.HomeActivity
import com.waterman.packai.home.data.CommonSelect
import com.waterman.packai.home.data.MediaPickType
import com.waterman.packai.home.data.PhotoFiles
import com.waterman.packai.home.viewmodel.BrandListState
import com.waterman.packai.home.viewmodel.DeletePhotoState
import com.waterman.packai.home.viewmodel.GetUploadedPhotoState
import com.waterman.packai.home.viewmodel.HomeViewModel
import com.waterman.packai.home.viewmodel.OcrState
import com.waterman.packai.home.viewmodel.SavePackState
import com.waterman.packai.home.viewmodel.SrNoListState
import com.waterman.packai.home.viewmodel.SrNoState
import com.waterman.packai.home.viewmodel.UploadAttachmentState
import com.waterman.packai.network.request.DeletePhotoRequest
import com.waterman.packai.network.request.GetSrNoListRequest
import com.waterman.packai.network.request.SaveMultiplePhotoRequest
import com.waterman.packai.network.request.SavePackAIRequest
import com.waterman.packai.network.response.PhotoItem
import com.waterman.packai.utils.BrandMatcher
import com.waterman.packai.utils.Constants
import com.waterman.packai.utils.Constants.getTrimmedText
import com.waterman.packai.utils.Constants.setSafeOnClickListener
import com.waterman.packai.utils.Constants.showBrandSelectionDialog
import com.waterman.packai.utils.Constants.showConfirmDialog
import com.waterman.packai.utils.EncryptedPrefHelper
import com.waterman.packai.utils.TextPresenceChecker
import dagger.hilt.android.AndroidEntryPoint
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanCustomCode
import io.github.g00fy2.quickie.config.BarcodeFormat
import io.github.g00fy2.quickie.config.ScannerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class AddPumpDataFragment : BaseFragment() {
    private lateinit var binding: FragmentAddPumpDataBinding
    private val viewModel: HomeViewModel by viewModels()
    @Inject
    lateinit var sharedPref: EncryptedPrefHelper

    private var brandName: String? = null
    private var brandId: String? = null
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    companion object {
        private const val KEY_BRAND_ID = "key_brand_id"
        private const val KEY_BRAND_NAME = "key_brand_name"

        fun newInstance(brandId: String, brandName: String): AddPumpDataFragment {
            val fragment = AddPumpDataFragment()

            val bundle = Bundle()
            bundle.putString(KEY_BRAND_ID, brandId)
            bundle.putString(KEY_BRAND_NAME, brandName)

            fragment.arguments = bundle
            return fragment
        }
    }

    val scanCustomCode = registerForActivityResult(ScanCustomCode(), ::handleResult)
    private var selectedImageUri: Uri? = null
    private var selectedImageUriMotor: Uri? = null
    private var selectedImageUriPumpSet: Uri? = null
    private var selectedImageUriMultiple: Uri? = null
    private var cameraImageUri: Uri? = null
    private var cameraImageUriMotor:Uri? = null
    private var cameraImageUriPumpSet:Uri? = null
    private var cameraImageUriMultiple:Uri? = null
    private var selectedBitmap: Bitmap? = null
    private var selectedBitmapMotor: Bitmap? = null
    private var selectedBitmapPumpSet: Bitmap? = null
    private var topCropped: Bitmap? = null
    private var middleCroppedPump: Bitmap? = null
    private var middleCroppedMotor: Bitmap? = null
    private var motorStatus: String = "FAILED"
    private var motorType: String = "FAILED"
    private var pumpStatus: String = "FAILED"
    private var topBodyStatus: String = "FAILED"

    private var pumpTypeStatus: String = "FAILED"
    private var motorTypeStatus: String = "FAILED"
    private val attachmentList = mutableListOf<Uri>()
    private var itemName = ""
    private var customerName = ""
    private var serialNo = ""

    private var extractedSerialNoPump = ""
    private var extractedSerialNoMotor = ""
    private var extractedTypePump = ""
    private var extractedTypeMotor = ""

    private var fullTextFromAIPump = ""
    private var fullTextFromAIMotor = ""
    private var fullTextFromAILogo = ""
    private var packAIEntryId: String? = null


    // -------------------- CAMERA Body Scan--------------------
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture())  { result ->
            //Log.e("CameraDebug", "Success=$result  URI=$cameraImageUri")
            val uri = cameraImageUri
            if (!result || uri == null) {
                showToast("Failed to capture image")
                return@registerForActivityResult
            }

            viewLifecycleOwner.lifecycleScope.launch {
                if (!isAdded) return@launch

                binding.cropViewContainer.visibility = View.VISIBLE
                binding.constAiAnalysis.visibility = View.VISIBLE
                binding.constVarificationStatus.visibility = View.VISIBLE
                binding.linearAttachPhoto.visibility = View.VISIBLE
                binding.lylAttachPhotoList.visibility = View.VISIBLE
                //val uri = cameraImageUri
                selectedImageUri = uri
                //binding.imgUpload.setImageURI(uri)
                Glide.with(this@AddPumpDataFragment).load(uri).into(binding.imgUpload)

                val bitmap = withContext(Dispatchers.IO) {
                    uriToBitmap(uri)
                }
                if (bitmap == null) {
                    showToast("Failed to process image")
                    return@launch
                }
                selectedBitmap = bitmap

                val topCrop = withContext(Dispatchers.IO) {
                    cropTopPortionFast(bitmap)
                }
                topCropped = topCrop
                //binding.imgViewTopCrop.setImageBitmap(topCrop)
                Glide.with(this@AddPumpDataFragment)
                    .load(topCrop)
                    .into(binding.imgViewTopCrop)
                selectedBitmap?.let { cropBitmap ->
                    val isWithOcr = true
                        //binding.radioGroupOcrType.checkedRadioButtonId == R.id.radioWithOcr
                    if (isWithOcr) {
                        val file = withContext(Dispatchers.IO) {
                            File(requireContext().cacheDir, "ocr_image.jpg").apply {
                                FileOutputStream(this).use {
                                    cropBitmap.compress(Bitmap.CompressFormat.JPEG, 70, it)
                                }
                            }
                        }
                        viewModel.uploadToOcr(file,"Pump")
                    } else {
                        updateBodyScanUI("Pump")
                    }
                } ?: showToast("Select pump image first")
            }
        }
    private val cameraLauncherMotor =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (!success) return@registerForActivityResult
            viewLifecycleOwner.lifecycleScope.launch {
                if (!isAdded) return@launch

                val uri = cameraImageUriMotor
                if (!success || uri == null) {
                    showToast("Failed to capture image")
                    return@launch
                }

                binding.cropViewContainer.visibility = View.VISIBLE
                binding.constAiAnalysis.visibility = View.VISIBLE
                binding.constVarificationStatus.visibility = View.VISIBLE
                binding.linearAttachPhoto.visibility = View.VISIBLE
                binding.lylAttachPhotoList.visibility = View.VISIBLE

                //val uri = cameraImageUriMotor
                selectedImageUriMotor = uri
                //binding.imgUploadMotor.setImageURI(uri)
                Glide.with(this@AddPumpDataFragment).load(uri).into(binding.imgUploadMotor)

                val bitmap = withContext(Dispatchers.IO) {
                    uriToBitmap(uri)
                }
                if (bitmap == null) {
                    showToast("Failed to load image")
                    return@launch
                }
                selectedBitmapMotor = bitmap

                detectAndCropSerialAreaWithGeminiFallback(context = requireContext(),bitmap = bitmap,photoType = "MOTOR") { cropped, fullText, serialNoExtracted, productTypeExtracted  ->
                    if (!isAdded) return@detectAndCropSerialAreaWithGeminiFallback  // prevent crash if fragment detached
                    if (cropped != null) {
                        //Log.e("Text For Motor", fullText.toString())
                       // Log.e("Text For Motor sr no", serialNo)
                        fullTextFromAIMotor = fullText

                        val extractedText = extractSerialNumber(fullText)
                        if (extractedText == null) {
                            showToast("Please select motor photo again")
                            return@detectAndCropSerialAreaWithGeminiFallback
                        }
                        Log.e("Extracted Motor", extractedText)

                        val extractTypeText = ProductCodeExtractor.extract(fullText)
                        if (extractTypeText == null) {
                            showToast("Type extract failed")
                            return@detectAndCropSerialAreaWithGeminiFallback
                        }

                        extractedSerialNoMotor = extractedText

                        binding.txtPercent.text = "100%"
                        binding.progressBarAnalysis.progress = 100
                        middleCroppedPump = cropped
                        //binding.imgViewMidCropPump.setImageBitmap(cropped)

                        Glide.with(this@AddPumpDataFragment)
                            .load(cropped)
                            .into(binding.imgViewMidCropPump)

                        binding.txtViewPumpText.setText(fullText)
                        val isMatched = extractedText.contains(serialNo, ignoreCase = true)
                        motorStatus = if (isMatched) "OK" else "FAILED"


                        val extractedType = extractTypeText
                        val actualItemName = binding.txtItemName.getTrimmedText()

                      /*  Log.d("TextPresenceChecker", "========================================")
                        Log.d("TextPresenceChecker", "Checking Extracted : $extractedType")
                        Log.d("TextPresenceChecker", "Against Actual     : $actualItemName")*/

                        val result = TextPresenceChecker.check(extractedType, actualItemName)

                        if (result.isValid) {
                            motorTypeStatus = "OK"
                            extractedTypeMotor = result.commonMatchedText
                            //Log.d("TextPresenceChecker", "FINAL RESULT    : ✅ Valid")
                            //Log.d("TextPresenceChecker", "Common Matched Text for API : ${result.commonMatchedText}")

                        } else {
                            motorTypeStatus = "FAILED"
                            extractedTypeMotor = extractTypeText
                            //Log.d("TextPresenceChecker", "FINAL RESULT: ❌ Invalid — Extracted NOT found in Actual")
                        }

                        //Log.d("TextPresenceChecker", "========================================")

                        // Sr.No Text
                        updateStatusUI(
                            status = motorStatus,
                            textView = binding.txtMotorSerialNo,
                            matchedText = "Motor serial number matched\n(Extracted : $extractedText)",
                            notMatchedText = "Motor serial number mismatched\n(Extracted : $extractedText)"
                        )
                        // Type Text
                        updateStatusUI(
                            status = motorTypeStatus,
                            textView = binding.txtMotorTypeText,
                            matchedText = "Motor type matched\n(Extracted : $extractedTypeMotor)",
                            notMatchedText = "Motor type mismatched\n(Extracted : $extractTypeText)"
                        )

                    } else {
                        //Log.e("Extracted motor", fullText)
                        showToast("Please select photo again")
                    }
                }
            }
        }

    private val cameraLauncherPumpSet =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (!success) return@registerForActivityResult
            viewLifecycleOwner.lifecycleScope.launch {
                if (!isAdded) return@launch

                val uri = cameraImageUriPumpSet
                if (!success || uri == null) {
                    showToast("Failed to capture image")
                    return@launch
                }

                //val uri = cameraImageUriMotor
                selectedImageUriPumpSet = uri
                binding.imgUploadPumpSet.setImageURI(uri)
                val bitmap = withContext(Dispatchers.IO) {
                    uriToBitmap(uri)
                }
                if (bitmap == null) {
                    showToast("Failed to load image")
                    return@launch
                }
                selectedBitmapPumpSet = bitmap
            }
        }

    private val cameraLauncherMultiple =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {

                val uri = cameraImageUriMultiple

                if (!success || uri == null) {
                    showToast("Failed to capture image")
                    return@registerForActivityResult
                }

                selectedImageUriMultiple = uri
                handleSelectedUri(uri)
            }
        }
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCameraInternal()
            else showToast("Camera permission denied")
        }
    private val cameraPermissionLauncherMotor =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCameraInternalMotor()
            else showToast("Camera permission denied")
        }

    private val cameraPermissionLauncherPumpSet =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCameraInternalPumpSet()
            else showToast("Camera permission denied")
        }

    // -------------------- CAMERA Multiple--------------------
    private val cameraPermissionLauncherMultiple =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCameraInternalMultiple()
            else showToast("Camera permission denied")
        }

    // -------------------- GALLERY Body Scan --------------------
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri ?: return@registerForActivityResult
            viewLifecycleOwner.lifecycleScope.launch {
                if (!isAdded) return@launch

                // ✅ Instant UI
                binding.cropViewContainer.visibility = View.VISIBLE
                binding.constAiAnalysis.visibility = View.VISIBLE
                binding.constVarificationStatus.visibility = View.VISIBLE
                binding.linearAttachPhoto.visibility = View.VISIBLE
                binding.lylAttachPhotoList.visibility = View.VISIBLE

                selectedImageUri = uri
                Glide.with(this@AddPumpDataFragment).load(uri).into(binding.imgUpload)
                //binding.imgUpload.setImageURI(uri)

                // 🔥 Decode in background
                val bitmap = withContext(Dispatchers.IO) {
                    uriToBitmap(uri)
                }

                if (bitmap == null || !isAdded) {
                    showToast("Failed to load image")
                    return@launch
                }

                selectedBitmap = bitmap

                // 🔥 Run cropTop + OCR in parallel
                val cropTopDeferred = async(Dispatchers.IO) {
                    cropTopPortionFast(bitmap)
                }

                topCropped = cropTopDeferred.await()
                //binding.imgViewTopCrop.setImageBitmap(topCropped)

                Glide.with(this@AddPumpDataFragment)
                    .load(topCropped)
                    .into(binding.imgViewTopCrop)

                selectedBitmap?.let { cropBitmap ->
                    val isWithOcr =
                        binding.radioGroupOcrType.checkedRadioButtonId == R.id.radioWithOcr
                    if (isWithOcr) {
                        val file = withContext(Dispatchers.IO) {
                            File(requireContext().cacheDir, "ocr_image.jpg").apply {
                                FileOutputStream(this).use {
                                    cropBitmap.compress(Bitmap.CompressFormat.JPEG, 73, it)
                                }
                            }
                        }
                        viewModel.uploadToOcr(file,"Pump")
                    } else {
                        updateBodyScanUI("Pump")
                    }
                } ?: showToast("Select pump image first")
            }
        }

    // -------------------- GALLERY Motor --------------------
    private val galleryLauncherMotor =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri ?: return@registerForActivityResult
            viewLifecycleOwner.lifecycleScope.launch {
                if (!isAdded) return@launch
                // ✅ Instant UI response
                binding.cropViewContainer.visibility = View.VISIBLE
                binding.constAiAnalysis.visibility = View.VISIBLE
                binding.constVarificationStatus.visibility = View.VISIBLE
                binding.linearAttachPhoto.visibility = View.VISIBLE
                binding.lylAttachPhotoList.visibility = View.VISIBLE

                selectedImageUriMotor = uri
                Glide.with(this@AddPumpDataFragment).load(uri).into(binding.imgUploadMotor)
                //binding.imgUploadMotor.setImageURI(uri)

                // 🔥 Decode in background
                val bitmapDeferred = async(Dispatchers.IO) {
                    uriToBitmap(uri)
                }

                val bitmap = bitmapDeferred.await()

                if (bitmap == null || !isAdded) {
                    showToast("Failed to load image")
                    return@launch
                }

                selectedBitmapMotor = bitmap

                // 🔥 Start MLKit immediately (no extra blocking)
                detectAndCropSerialAreaWithGeminiFallback(context = requireContext(),bitmap = bitmap,photoType = "MOTOR") { cropped, fullText, serialNoExtracted, productTypeExtracted  ->
                    if (!isAdded) return@detectAndCropSerialAreaWithGeminiFallback
                    Log.e("Text For Motor", fullText.toString())
                    fullTextFromAIMotor = fullText

                    if (cropped == null) {
                        showToast("Please select motor photo again")
                        return@detectAndCropSerialAreaWithGeminiFallback
                    }

                    val extractedText = extractSerialNumber(fullText)
                    if (extractedText == null) {
                        showToast("Please select motor photo again")
                        return@detectAndCropSerialAreaWithGeminiFallback
                    }

                    val extractTypeText = ProductCodeExtractor.extract(fullText)
                    if (extractTypeText == null) {
                        showToast("Type extract failed")
                        return@detectAndCropSerialAreaWithGeminiFallback
                    }

                    extractedSerialNoMotor = extractedText

                    // ✅ UI Update
                    binding.txtPercent.text = "100%"
                    binding.progressBarAnalysis.progress = 100

                    middleCroppedPump = cropped
                    //binding.imgViewMidCropPump.setImageBitmap(cropped)

                    Glide.with(this@AddPumpDataFragment)
                        .load(cropped)
                        .into(binding.imgViewMidCropPump)

                    binding.txtViewPumpText.setText(fullText)
                    val isMatched = extractedText.contains(serialNo, ignoreCase = true)
                    motorStatus = if (isMatched) "OK" else "FAILED"

                    val extractedType = extractTypeText
                    val actualItemName = binding.txtItemName.getTrimmedText()

                    Log.d("TextPresenceChecker", "========================================")
                    Log.d("TextPresenceChecker", "Checking Extracted : $extractedType")
                    Log.d("TextPresenceChecker", "Against Actual     : $actualItemName")

                    val result = TextPresenceChecker.check(extractedType, actualItemName)

                    if (result.isValid) {
                        motorTypeStatus = "OK"
                        extractedTypeMotor = result.commonMatchedText
                        Log.d("TextPresenceChecker", "FINAL RESULT    : ✅ Valid")
                        Log.d(
                            "TextPresenceChecker",
                            "Common Matched Text for API : ${result.commonMatchedText}"
                        )

                    } else {
                        motorTypeStatus = "FAILED"
                        extractedTypeMotor = extractTypeText
                        Log.d(
                            "TextPresenceChecker",
                            "Common Matched Text for API : ${result.commonMatchedText}"
                        )
                        Log.d(
                            "TextPresenceChecker",
                            "FINAL RESULT: ❌ Invalid — Extracted NOT found in Actual"
                        )
                    }

                    Log.d("TextPresenceChecker", "========================================")

                    // Sr.No Text
                    updateStatusUI(
                        status = motorStatus,
                        textView = binding.txtMotorSerialNo,
                        matchedText = "Motor serial number matched\n(Extracted: $extractedText)",
                        notMatchedText = "Motor serial number mismatched\n(Extracted: $extractedText)"
                    )
                    // Type Text
                    updateStatusUI(
                        status = motorTypeStatus,
                        textView = binding.txtMotorTypeText,
                        matchedText = "Motor type matched\n(Extracted: $extractedTypeMotor)",
                        notMatchedText = "Motor type mismatched\n(Extracted: $extractedTypeMotor)"
                    )
                }
            }
        }

    // -------------------- GALLERY Multiple --------------------
    private val galleryLauncherMultiple =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                selectedImageUriMultiple = it
                handleSelectedUri(it)
                //addAttachmentView(selectedImageUriMultiple!!)
            }
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("camera_uri", cameraImageUri?.toString())
        outState.putString("camera_uri_motor", cameraImageUriMotor?.toString())
        outState.putString("camera_uri_pumpset", cameraImageUriPumpSet?.toString())
        outState.putString("camera_uri_multiple", cameraImageUriMultiple?.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        brandName = arguments?.getString(KEY_BRAND_NAME)
        brandId = arguments?.getString(KEY_BRAND_ID)

        /*brandId?.let {
            val request = GetSrNoDropDownListRequest(brandId = brandId?:"")
            viewModel.getSrNoList(request)
        }*/
    }

    override fun onResume() {
        super.onResume()
        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.toolbarHome)
        toolbar.background = null  // ✅ Removes background completely

        val logOutIcon = requireActivity().findViewById<AppCompatImageView>(R.id.imgViewLogOut)
        val toolbarTitle = requireActivity().findViewById<AppCompatTextView>(R.id.txtViewTitle)
        toolbarTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        ImageViewCompat.setImageTintList(
            logOutIcon,
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.black))
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer.close()
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddPumpDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraImageUri = savedInstanceState
            ?.getString("camera_uri")
            ?.let { Uri.parse(it) }

        cameraImageUriMotor = savedInstanceState
            ?.getString("camera_uri_motor")
            ?.let { Uri.parse(it) }

        cameraImageUriPumpSet = savedInstanceState
            ?.getString("camera_uri_pumpset")
            ?.let { Uri.parse(it) }

        cameraImageUriMultiple = savedInstanceState
            ?.getString("camera_uri_multiple")
            ?.let { Uri.parse(it) }

        moveOnClickListeners()
        manageToolBar()

        observeSrNoData()
        observeSavePackAIData()
        observeSaveMultiplePhoto()
        observeGetPhotoData()
        observeDeletePhotoData()
        observeBrandName()
        observeSrNoDropDown()
        observeOcrApiCall()
    }

    private fun updateBodyScanUI(isFrom: String) {
        when(isFrom){
            "Pump" -> {
                topBodyStatus = "FAILED"
                fullTextFromAILogo = "FAILED"
                updateStatusUI(
                    status = topBodyStatus,
                    textView = binding.txtBodyScan,
                    matchedText = "Logo scan matched",
                    notMatchedText = "Logo scan mismatched"
                )
                pumpStatus = "FAILED"
                pumpTypeStatus = "FAILED"
                extractedTypePump = "FAILED"
                fullTextFromAIPump = "FAILED"
                // Sr.No Text
                updateStatusUI(
                    status = pumpStatus,
                    textView = binding.txtPumpSerialNo,
                    matchedText = "Pump serial number matched\n(Extracted : FAILED)",
                    notMatchedText = "Pump serial number mismatched\n(Extracted : FAILED)"
                )
                // Type Text
                updateStatusUI(
                    status = pumpTypeStatus,
                    textView = binding.txtPumpTypeText,
                    matchedText = "Pump type matched\n(Extracted: FAILED)",
                    notMatchedText = "Pump type mismatched\n(Extracted: FAILED)"
                )
            }
            "Motor" -> {
                motorStatus = "FAILED"
                motorTypeStatus = "FAILED"
                extractedTypeMotor = "FAILED"
                fullTextFromAIMotor = "FAILED"
                // Sr.No Text
                updateStatusUI(
                    status = motorStatus,
                    textView = binding.txtMotorSerialNo,
                    matchedText = "Motor serial number matched\n(Extracted: FAILED)",
                    notMatchedText = "Motor serial number mismatched\n(Extracted: FAILED)"
                )
                // Type Text
                updateStatusUI(
                    status = motorTypeStatus,
                    textView = binding.txtMotorTypeText,
                    matchedText = "Motor type matched\n(Extracted: FAILED)",
                    notMatchedText = "Motor type mismatched\n(Extracted: FAILED)"
                )
            }
        }

    }

    private fun manageToolBar() {
        binding.txtBrandName.setText(brandName)
        (activity as HomeActivity).apply {
            manageToolBar(isVisible = true)
            manageToolBarTitle("Add Product Data")
            manageBackButtonClick(true)
            manageDrawerLock(false)
            setDrawerEnabled(false)
            logOutButtonManage(false)
        }
    }

    private fun moveOnClickListeners() = with(binding) {
        /*txtSerialNumber.setSafeOnClickListener {
            val list = viewModel.cachedBrandList
            val bottomSheet = list?.let { it1 -> SelectSrNoDropDownBottomSheetFragment.newInstance(it1) }
            bottomSheet?.setDismissCallback { selected ->
                txtSerialNumber.setText(selected.Text)
            }
            bottomSheet?.show(childFragmentManager, "SelectPlanFor")
        }*/

        txtPumpSerialNo.setSafeOnClickListener {
            val messageText = when (pumpStatus) {
                "FAILED" -> getString(
                    R.string.msg_serial_failed,
                    txtSerialNumber.getTrimmedText(),
                    extractedSerialNoPump ?: "-"
                )
                "OK" -> getString(
                    R.string.msg_serial_success,
                    txtSerialNumber.getTrimmedText(),
                    extractedSerialNoPump ?: "-"
                )
                else -> getString(R.string.msg_serial_unknown)
            }

            showConfirmDialog(
                context = requireContext(),
                title = getString(R.string.title_pump_sr_no),
                message = messageText,
                onOk = {}
            )
        }

        txtMotorSerialNo.setSafeOnClickListener {
            val messageText = when (motorStatus) {
                "FAILED" -> getString(
                    R.string.msg_serial_failed,
                    txtSerialNumber.getTrimmedText(),
                    extractedSerialNoMotor ?: "-"
                )
                "OK" -> getString(
                    R.string.msg_serial_success,
                    txtSerialNumber.getTrimmedText(),
                    extractedSerialNoMotor ?: "-"
                )
                else -> getString(R.string.msg_serial_unknown)
            }

            showConfirmDialog(
                context = requireContext(),
                title = getString(R.string.title_motor_sr_no),
                message = messageText,
                onOk = {}
            )
        }

        txtBodyScan.setSafeOnClickListener {
            val messageText = when (topBodyStatus) {
                "FAILED" -> getString(
                    R.string.msg_logo_failed,
                    txtCustomerName.getTrimmedText(),
                    fullTextFromAILogo ?: "-"
                )
                "OK" -> getString(
                    R.string.msg_logo_success,
                    txtCustomerName.getTrimmedText(),
                    fullTextFromAILogo ?: "-"
                )
                else -> getString(R.string.msg_logo_unknown)
            }

            showConfirmDialog(
                context = requireContext(),
                title = getString(R.string.title_logo),
                message = messageText,
                onOk = {}
            )
        }

        txtPumpTypeText.setSafeOnClickListener {
            val messageText = when (pumpTypeStatus) {
                "FAILED" -> getString(
                    R.string.msg_type_failed,
                    txtItemName.getTrimmedText(),
                    extractedTypePump ?: "-"
                )
                "OK" -> getString(
                    R.string.msg_type_success,
                    txtItemName.getTrimmedText(),
                    extractedTypePump ?: "-"
                )
                else -> getString(R.string.msg_type_unknown)
            }

            showConfirmDialog(
                context = requireContext(),
                title = getString(R.string.title_pump_type),
                message = messageText,
                onOk = {}
            )
        }

        txtMotorTypeText.setSafeOnClickListener {
            val messageText = when (motorTypeStatus) {
                "FAILED" -> getString(
                    R.string.msg_type_failed,
                    txtItemName.getTrimmedText(),
                    extractedTypeMotor ?: "-"
                )
                "OK" -> getString(
                    R.string.msg_type_success,
                    txtItemName.getTrimmedText(),
                    extractedTypeMotor ?: "-"
                )
                else -> getString(R.string.msg_type_unknown)
            }

            showConfirmDialog(
                context = requireContext(),
                title = getString(R.string.title_motor_type),
                message = messageText,
                onOk = {}
            )
        }
        txtViewChangeBrandName.setSafeOnClickListener {
            if (sharedPref.isBrandSelected()) {
                viewModel.getBrandList()
            }
        }
        imgViewScanSerialNo.setSafeOnClickListener {
            Constants.hideKeyboard(it)
            clearFocus()
            binding.txtSerialNumber.setText("")
            scanQRCode()
        }
        constLayoutCamGallery.setSafeOnClickListener {
            Constants.hideKeyboard(it)
            clearFocus()
            val bottomSheet = CommonSelectBottomSheetFragment.newInstance(CommonSelect.CAMERA_GALLERY)
            bottomSheet.setDismissCallback {
                view?.post {
                    when (it) {
                        MediaPickType.CAMERA -> {
                            openCamera()
                        }
                        MediaPickType.GALLERY -> openGallery()
                        else -> Unit
                    }
                }
            }
            bottomSheet.show(parentFragmentManager, "MediaPicker")
        }
        constLayoutCamGalleryMotor.setSafeOnClickListener {
            Constants.hideKeyboard(it)
            clearFocus()
            val bottomSheet = CommonSelectBottomSheetFragment.newInstance(CommonSelect.CAMERA_GALLERY)
            bottomSheet.setDismissCallback {
                view?.post {
                    when (it) {
                        MediaPickType.CAMERA -> openCameraMotor()
                        MediaPickType.GALLERY -> openGalleryMotor()
                        else -> Unit
                    }
                }
            }
            bottomSheet.show(parentFragmentManager, "MediaPicker")
        }
        constLayoutCamGalleryPumpSet.setSafeOnClickListener {
            Constants.hideKeyboard(it)
            clearFocus()
            val bottomSheet =
                CommonSelectBottomSheetFragment.newInstance(CommonSelect.CAMERA_GALLERY)
            bottomSheet.setDismissCallback {
                view?.post {
                    when (it) {
                        MediaPickType.CAMERA -> openCameraPumpSet()
                        else -> Unit
                    }
                }
            }
            bottomSheet.show(parentFragmentManager, "MediaPicker")
        }
        imgUpload.setSafeOnClickListener {
            Constants.hideKeyboard(it)
            clearFocus()
            selectedBitmap?.let { bitmap ->
                val uri = bitmapToUri(requireContext(), bitmap)

                val intent = Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("image_uri", uri.toString())
                startActivity(intent)
            } ?: run {
                showToast(getString(R.string.validation_please_select_image_pump))
            }
        }
        imgUploadMotor.setSafeOnClickListener {
            Constants.hideKeyboard(it)
            clearFocus()
            selectedBitmapMotor?.let { bitmap ->
                val uri = bitmapToUri(requireContext(), bitmap)

                val intent = Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("image_uri", uri.toString())
                startActivity(intent)
            } ?: run {
                showToast(getString(R.string.validation_please_select_image_motor))
            }
        }
        imgUploadPumpSet.setSafeOnClickListener {
            Constants.hideKeyboard(it)
            clearFocus()
            selectedBitmapPumpSet?.let { bitmap ->
                val uri = bitmapToUri(requireContext(), bitmap)

                val intent = Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("image_uri", uri.toString())
                startActivity(intent)
            } ?: run {
                showToast(getString(R.string.validation_please_select_image_pump_set))
            }
        }
        txtViewCheckSrNo.setSafeOnClickListener {
            Constants.hideKeyboard(it)
            clearFocus()
            if (txtSerialNumber.getTrimmedText().isEmpty()) {
                showToast("Please enter or scan SR NO")
            } else {
                callSrNoApi()
            }
        }
        imgViewMidCropPump.setSafeOnClickListener {
            Constants.hideKeyboard(it)
            clearFocus()
            middleCroppedPump?.let { bitmap ->
                val uri = bitmapToUri(requireContext(), bitmap)

                val intent = Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("image_uri", uri.toString())
                startActivity(intent)
            }
        }
        imgViewMidCropMotor.setSafeOnClickListener {
            Constants.hideKeyboard(it)
            clearFocus()
            middleCroppedMotor?.let { bitmap ->
                val uri = bitmapToUri(requireContext(), bitmap)

                val intent = Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("image_uri", uri.toString())
                startActivity(intent)
            }
        }
        imgViewTopCrop.setSafeOnClickListener {
            Constants.hideKeyboard(it)
            clearFocus()
            topCropped?.let { bitmap ->
                val uri = bitmapToUri(requireContext(), bitmap)

                val intent = Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("image_uri", uri.toString())
                startActivity(intent)
            }
        }
        linearAttachPhoto.setSafeOnClickListener {
            Constants.hideKeyboard(it)
            clearFocus()
            val bottomSheet =
                CommonSelectBottomSheetFragment.newInstance(CommonSelect.CAMERA_GALLERY)
            bottomSheet.setDismissCallback {
                when (it) {
                    MediaPickType.CAMERA -> openCameraMultiple()
                    MediaPickType.GALLERY -> openGalleryMultiple()
                    else -> Unit
                }
            }
            bottomSheet.show(parentFragmentManager, "MediaPicker")
        }
        btnSubmit.setSafeOnClickListener {
            Constants.hideKeyboard(it)
            clearFocus()
            if (txtSerialNumber.getTrimmedText().isEmpty()) {
                showToast(getString(R.string.validation_please_enter_or_scan_sr_no))
                return@setSafeOnClickListener
            }
            if (txtSoNo.getTrimmedText().isEmpty()) {
                showToast(getString(R.string.validation_please_press_the_check_serial_no))
                return@setSafeOnClickListener
            }
            if (!isImageLoaded(imgUpload)) {
                showToast(getString(R.string.validation_please_select_image_pump))
                return@setSafeOnClickListener
            }
            if (!isImageLoaded(imgUploadMotor)) {
                showToast(getString(R.string.validation_please_select_image_motor))
                return@setSafeOnClickListener
            }
            if (!isImageLoaded(imgUploadPumpSet)) {
                showToast(getString(R.string.validation_please_select_image_pump_set))
                return@setSafeOnClickListener
            }
            if (topCropped == null) {
                showToast(getString(R.string.validation_please_upload_photo))
                return@setSafeOnClickListener
            }
            if (middleCroppedMotor == null) {
                showToast(getString(R.string.validation_please_select_image_pump))
                return@setSafeOnClickListener
            }
            if (middleCroppedPump == null) {
                showToast(getString(R.string.validation_please_select_image_motor))
                return@setSafeOnClickListener
            }
            if (selectedBitmapMotor == null) {
                showToast(getString(R.string.validation_please_select_image_motor))
                return@setSafeOnClickListener
            }
            if (selectedBitmapPumpSet == null) {
                showToast(getString(R.string.validation_please_select_image_pump_set))
                return@setSafeOnClickListener
            }
            callSavePackApi()
        }
    }

    private fun clearFocus() = with(binding) {
        txtSerialNumber.clearFocus()
        txtOtherRemark.clearFocus()
    }

    private fun callSavePackApi() = with(binding) {
        viewLifecycleOwner.lifecycleScope.launch {
            showLoader()
            try {
                //val fileStartTime = System.currentTimeMillis()
                val timeStamp = System.currentTimeMillis()
                val files = withContext(Dispatchers.IO) {
                    // All 6 conversions run in PARALLEL instead of sequentially
                    val mainPhotoDeferred = async { bitmapToFile(requireContext(), selectedBitmap!!, "main_photo_$timeStamp") }
                    val topPhotoDeferred = async { bitmapToFile(requireContext(), topCropped!!, "top_photo_$timeStamp") }
                    val motorPhotoDeferred = async { bitmapToFile(requireContext(), middleCroppedMotor!!, "motor_photo_$timeStamp") }
                    val pumpPhotoDeferred = async { bitmapToFile(requireContext(), middleCroppedPump!!, "pump_photo_$timeStamp") }
                    val motorMainPhotoDeferred = async { bitmapToFile(requireContext(), selectedBitmapMotor!!, "motor_main_photo_$timeStamp") }
                    val pumpSetPhotoDeferred = async { bitmapToFile(requireContext(), selectedBitmapPumpSet!!, "pump_set_photo_$timeStamp") }

                    PhotoFiles(
                        mainPhoto = mainPhotoDeferred.await(),
                        topPhoto = topPhotoDeferred.await(),
                        motorPhoto = motorPhotoDeferred.await(),
                        pumpPhoto = pumpPhotoDeferred.await(),
                        motorMainPhoto = motorMainPhotoDeferred.await(),
                        pumpSetPhoto = pumpSetPhotoDeferred.await()
                    )
                }
                packAIEntryId?.let {
                    val request = SavePackAIRequest(
                        brandId = brandId?:"",
                        userId = sharedPref.getUser()?.UserId ?: "",
                        srNo = serialNo,
                        sONo = txtSoNo.getTrimmedText(),
                        customerName = txtCustomerName.getTrimmedText(),
                        itemName = txtItemName.getTrimmedText(),
                        motorSerialNoStatus = motorStatus,
                        pumpSerialNoStatus = pumpStatus,
                        topBodyStatus = topBodyStatus,
                        mainPhoto = files.mainPhoto,
                        topPhoto = files.topPhoto,
                        motorPhoto = files.motorPhoto,
                        pumpPhoto = files.pumpPhoto,
                        motorMainPhoto = files.motorMainPhoto,
                        pumpSetPhoto = files.pumpSetPhoto,
                        extractedPumpSrNo = extractedSerialNoPump,
                        extractedMotorSrNo = extractedSerialNoMotor,
                        extractedPumpType = extractedTypePump,
                        extractedMotorType = extractedTypeMotor,
                        pumpTypeStatus = pumpTypeStatus,
                        motorTypeStatus = motorTypeStatus,
                        fullAiPumpText = fullTextFromAIPump,
                        fullAiMotorText = fullTextFromAIMotor,
                        fullAiLogoText = fullTextFromAILogo,
                        itemArray = "[]",
                        remark = txtOtherRemark.getTrimmedText(),
                        packAiEntryId = it
                    )
                    viewModel.savePack(request)
                }
            } catch (e: Exception) {
                hideLoader()
                e.printStackTrace()
            }
        }
    }

    private fun handleSelectedUri(uri: Uri) {
        val file = copyUriToCacheSafe(requireContext(), uri)
        val reportId = packAIEntryId
        if (reportId.isNullOrEmpty()) {
            showToast("Record ID not ready")
            return
        }
        viewModel.uploadAttachment(
            request = SaveMultiplePhotoRequest(
                packAiEntryId = reportId,
                userId = sharedPref.getUser()?.UserId ?: "",
                photo = file
            )
        )
    }

    @Throws(IOException::class)
    fun copyUriToCacheSafe(context: Context, uri: Uri): File {

        val cacheDir = context.cacheDir

        // -------- CASE 1: file:// (MediaRecorder, Camera cache) --------
        if (ContentResolver.SCHEME_FILE == uri.scheme) {
            val file = File(uri.path!!)
            if (file.exists() && file.length() > 0) {
                return file
            }
            throw IOException("Invalid file URI")
        }

        // -------- CASE 2: content:// (SAF, Gallery, Docs, OEM pickers) --------
        val resolver = context.contentResolver

        // Try to get original name (best effort)
        val fileName = resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                cursor.getString(nameIndex)
            } else null
        }

        // Extension fallback logic
        val extension = fileName?.substringAfterLast('.', "")
            ?.takeIf { it.length <= 5 }
            ?.let { ".$it" }
            ?: guessExtension(resolver.getType(uri))

        val outputFile = File(
            cacheDir,
            "upload_${System.currentTimeMillis()}$extension"
        )

        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output, DEFAULT_BUFFER_SIZE)
            }
        } ?: throw IOException("Unable to open input stream")

        if (!outputFile.exists() || outputFile.length() == 0L) {
            throw IOException("Copied file is empty")
        }

        return outputFile
    }

    fun guessExtension(mimeType: String?): String {
        return when (mimeType) {
            "image/jpeg" -> ".jpg"
            "image/png" -> ".png"
            "audio/mpeg" -> ".mp3"
            "audio/mp4" -> ".m4a"
            "audio/aac" -> ".aac"
            "audio/wav" -> ".wav"
            "application/pdf" -> ".pdf"
            else -> ".bin"
        }
    }

    private fun bitmapToUri(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val file = File(
                context.cacheDir,
                "full_image_${System.currentTimeMillis()}.jpg"
            )

            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            }

            Uri.fromFile(file)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isImageLoaded(imageView: AppCompatImageView): Boolean {
        val drawable = imageView.drawable ?: return false

        return drawable !is VectorDrawable && drawable !is VectorDrawableCompat
    }

    private fun addAttachmentView(uri: Uri) {
        try {
            attachmentList.add(uri)

            binding.lylAttachPhotoList.visibility = View.VISIBLE
            binding.tvPhotoCount.text = attachmentList.size.toString()

            val fileName = getFileNameFromUri(uri)

            // Parent Row Layout
            val rowLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 12, 0, 12)
                }
                setPadding(20, 10, 20, 10)
                background = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.bg_edit_text_white_rounded_15
                )
                gravity = Gravity.CENTER_VERTICAL
            }

            // File Name TextView
            val textView = AppCompatTextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = fileName
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }

            // Remove Icon
            val removeIcon = AppCompatImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(50, 50)
                setImageResource(R.drawable.ic_close) // your remove icon
                setColorFilter(ContextCompat.getColor(requireContext(), R.color.red_3))
            }

            // Click to open image
            textView.setOnClickListener {
                val intent = Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("image_uri", uri.toString())
                startActivity(intent)
            }

            // Remove click
            removeIcon.setOnClickListener {

                attachmentList.remove(uri)
                binding.lylAttachmentPhotoListRow.removeView(rowLayout)

                binding.tvPhotoCount.text = attachmentList.size.toString()

                if (attachmentList.isEmpty()) {
                    binding.lylAttachPhotoList.visibility = View.GONE
                }
            }

            // Add views to row
            rowLayout.addView(textView)
            rowLayout.addView(removeIcon)

            // Add row to main layout
            binding.lylAttachmentPhotoListRow.addView(rowLayout)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "Attachment"
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }

        return name
    }

    private fun bitmapToFile(context: Context, bitmap: Bitmap, fileName: String): File {
        // Create file inside cache directory
        val file = File(context.cacheDir, "$fileName.jpg")
        file.createNewFile()

        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, bos)
        val bitmapData = bos.toByteArray()

        FileOutputStream(file).use { fos ->
            fos.write(bitmapData)
            fos.flush()
        }

        return file
    }

    // -------------------- MEDIA OPENERS --------------------
    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED) {
           openCameraInternal()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCameraMotor() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCameraInternalMotor()
        } else {
            cameraPermissionLauncherMotor.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCameraPumpSet() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCameraInternalPumpSet()
        } else {
            cameraPermissionLauncherPumpSet.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCameraMultiple() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCameraInternalMultiple()
        } else {
            cameraPermissionLauncherMultiple.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCameraInternal() {
        val file = File(requireContext().cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )

        cameraImageUri?.let {
            cameraLauncher.launch(it)
        }

    }

    private fun openCameraInternalMotor() {
        val file = File(requireContext().cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        cameraImageUriMotor = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )

        cameraImageUriMotor?.let {
            cameraLauncherMotor.launch(it)
        }
    }

    private fun openCameraInternalPumpSet() {
        val file = File(requireContext().cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        cameraImageUriPumpSet = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )

        cameraImageUriPumpSet?.let {
            cameraLauncherPumpSet.launch(it)
        }
    }

    private fun openCameraInternalMultiple() {
        val file = File(requireContext().cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        cameraImageUriMultiple = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )

        cameraImageUriMultiple?.let {
            cameraLauncherMultiple.launch(it)
        }
    }

    private fun openGallery() {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun openGalleryMotor() {
        galleryLauncherMotor.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun openGalleryMultiple() {
        galleryLauncherMultiple.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun uriToBitmap(uri: Uri, maxWidth: Int = 1024, maxHeight: Int = 1024): Bitmap? {
        return try {
            val resolver = requireContext().contentResolver

            // Step 1: Decode bounds only (no memory used)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            // Step 2: Calculate sample size
            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Uses 2 bytes vs 4 bytes (ARGB_8888)

            // Step 3: Decode with sampling
            val bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            // Step 4: Handle orientation
            val orientation = resolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        ).also {
            if (it != bitmap) {
                bitmap.recycle() // free old bitmap memory
            }
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height, width) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /*======================================================================================*/
    private fun cropTopPortionFast(src: Bitmap): Bitmap {

        val width = src.width
        val height = src.height

        if (width <= 0 || height <= 0) return src

        var detectedTop = 0
        val threshold = 200  // simple fixed threshold (fast)

        // Scan only first 25% height
        val scanHeight = height / 4

        for (y in 0 until scanHeight step 2) {

            var darkCount = 0

            // Sample every 20 pixels horizontally (very fast)
            for (x in 0 until width step 20) {

                val pixel = src.getPixel(x, y)
                val r = (pixel shr 16) and 0xff
                val g = (pixel shr 8) and 0xff
                val b = pixel and 0xff

                val brightness = (r + g + b) / 3

                if (brightness < threshold) {
                    darkCount++
                }
            }

            // If enough dark pixels found, stop immediately
            if (darkCount > 5) {
                detectedTop = y
                break
            }
        }

        // Fallback
        if (detectedTop == 0) {
            detectedTop = height * 5 / 100
        }

        val cropHeight = (height * 0.40f).toInt()

        return Bitmap.createBitmap(
            src,
            0,
            detectedTop.coerceIn(0, height - 1),
            width,
            cropHeight.coerceAtMost(height - detectedTop)
        )
    }

    private fun detectAndCropSerialArea(
        context: Context,
        originalBitmap: Bitmap,
        onResult: (croppedBitmap: Bitmap?, fullText: String) -> Unit
    ) {
        try {
            val image = InputImage.fromBitmap(originalBitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image).addOnSuccessListener { visionText ->
                val fullText = visionText.text
                var serialBox: Rect? = null

                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val text = line.text.uppercase()

                        if (text.contains("SR") || text.contains("SR.") ||
                            text.contains("SR NO") || text.contains("SR. NO") ||
                            text.contains("SR.NO") || text.contains("ST. NO.") ||
                            text.contains("ST.") || text.contains("R NO.") ||
                            text.contains("TYPE")
                        ) {

                            serialBox = line.boundingBox
                            break
                        }
                    }
                }

                if (serialBox == null) {
                    onResult(null, fullText)
                    return@addOnSuccessListener
                }

                // Expand bounding box safely
                val paddingX = (originalBitmap.width * 0.20f).toInt()
                val paddingY = (originalBitmap.height * 0.08f).toInt()

                val left = (serialBox.left - paddingX).coerceAtLeast(0)
                val top = (serialBox.top - paddingY).coerceAtLeast(0)
                val right = (serialBox.right + paddingX).coerceAtMost(originalBitmap.width)
                val bottom = (serialBox.bottom + paddingY * 3).coerceAtMost(originalBitmap.height)
                val width = right - left
                val height = bottom - top

                if (width <= 0 || height <= 0) {
                    onResult(null, fullText)
                }

                if (width <= 0 || height <= 0) {
                    onResult(null, fullText)
                    return@addOnSuccessListener
                }
                val croppedBitmap = Bitmap.createBitmap(originalBitmap, left, top, width, height)
                onResult(croppedBitmap, fullText)
            }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "OCR Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    onResult(null, "")
                }

        } catch (e: Exception) {
            e.printStackTrace()
            onResult(null, "")
        }
    }

    /**
     * ML Kit first (fast, on-device).
     * If ML Kit fails to find crop OR serial → fallback to Gemini Vision (cloud).
     * Cropping: use ML Kit crop if available, else use ORIGINAL bitmap (no crop).
     */
    private suspend fun detectAndCropSerialAreaWithGeminiFallback(
        context: Context,
        bitmap: Bitmap,
        photoType: String, // "PUMP" or "MOTOR"
        onResult: (croppedBitmap: Bitmap?, fullText: String, serialNoExtracted: String?, productTypeExtracted: String?) -> Unit
    ) {
        // Step 1: Try ML Kit first (zero network cost, fast)
        val image = InputImage.fromBitmap(bitmap, 0)


        recognizer.process(image)
            .addOnSuccessListener { visionText ->

                if (!isAdded) return@addOnSuccessListener

                val fullText = visionText.text
                var serialBox: Rect? = null

                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val text = line.text.uppercase()
                        if (text.contains("SR") || text.contains("SR.") ||
                            text.contains("SR NO") || text.contains("SR. NO") ||
                            text.contains("SR.NO") || text.contains("ST. NO.") ||
                            text.contains("ST.") || text.contains("TYPE")
                        ) {
                            serialBox = line.boundingBox
                            break
                        }
                    }
                }

                val extractedSerial = extractSerialNumber(fullText)
                val extractedType = ProductCodeExtractor.extract(fullText)
                val mlKitSucceeded = serialBox != null && extractedSerial != null && !extractedType.isNullOrBlank()

                if (mlKitSucceeded) {
                    // ✅ ML Kit worked — crop and return normally
                    val paddingX = (bitmap.width * 0.20f).toInt()
                    val paddingY = (bitmap.height * 0.08f).toInt()
                    val left = (serialBox.left - paddingX).coerceAtLeast(0)
                    val top = (serialBox.top - paddingY).coerceAtLeast(0)
                    val right = (serialBox.right + paddingX).coerceAtMost(bitmap.width)
                    val bottom = (serialBox.bottom + paddingY * 3).coerceAtMost(bitmap.height)
                    val w = right - left
                    val h = bottom - top

                    val cropped = if (w > 0 && h > 0)
                        Bitmap.createBitmap(bitmap, left, top, w, h)
                    else
                        bitmap // safety fallback to original

                    onResult(cropped, fullText, extractedSerial, extractedType)

                } else {
                      Log.w("OCR_FALLBACK", "[$photoType] ML Kit failed → trying Gemini")
                    viewLifecycleOwner.lifecycleScope.launch {
                        selectedBitmapMotor?.let { cropBitmap ->
                            val isWithOcr =
                                binding.radioGroupOcrType.checkedRadioButtonId == R.id.radioWithOcr
                            if (isWithOcr) {
                                val file = withContext(Dispatchers.IO) {
                                    File(requireContext().cacheDir, "ocr_image.jpg").apply {
                                        FileOutputStream(this).use {
                                            cropBitmap.compress(Bitmap.CompressFormat.JPEG, 73, it)
                                        }
                                    }
                                }
                                viewModel.uploadToOcr(file,"Motor")
                            } else {
                                updateBodyScanUI("Motor")
                            }
                        } ?: showToast("Select pump image first")
                    }
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Log.e("OCR_FALLBACK", "[$photoType] ML Kit exception → trying Gemini", e)

                // ML Kit threw exception → go straight to Gemini
                viewLifecycleOwner.lifecycleScope.launch {
                    selectedBitmapMotor?.let { cropBitmap ->
                        val isWithOcr =
                            binding.radioGroupOcrType.checkedRadioButtonId == R.id.radioWithOcr
                        if (isWithOcr) {
                            val file = withContext(Dispatchers.IO) {
                                File(requireContext().cacheDir, "ocr_image.jpg").apply {
                                    FileOutputStream(this).use {
                                        cropBitmap.compress(Bitmap.CompressFormat.JPEG, 73, it)
                                    }
                                }
                            }
                            viewModel.uploadToOcr(file,"Motor")
                        } else {
                            updateBodyScanUI("Motor")
                        }
                    } ?: showToast("Select motor image first")
                }
            }
    }

    private fun extractSerialNumber(fullText: String): String? {
        if (fullText.isBlank()) return null
        try {
            val lines = fullText.split("\n")

            for (line in lines) {
                val upperLine = line.uppercase()

                if (upperLine.contains("SR")) {

                    // Merge all digits from SR line
                    val mergedDigits = line.filter { it.isDigit() }
                    if (mergedDigits.length in 11..15) {
                        return mergedDigits
                    }

                    val digitRegex = Regex("\\d+")
                    val matches = digitRegex.findAll(line)

                    for (match in matches) {
                        val number = match.value
                        if (number.length in 11..15) {
                            return number
                        }
                    }

                    val spacedRegex = Regex("\\d[\\d\\s]{10,20}\\d")
                    val spacedMatch = spacedRegex.find(line)
                    spacedMatch?.let {
                        val cleaned = it.value.replace(" ", "")
                        if (cleaned.length in 11..15) {
                            return cleaned
                        }
                    }
                }
            }

            val longRegex = Regex("\\d[\\d\\s]{10,20}\\d")
            val longMatches = longRegex.findAll(fullText)

            for (match in longMatches) {
                val cleaned = match.value.replace(" ", "")
                if (cleaned.length in 11..15) {
                    return cleaned
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun showLoader() = with(binding) {
        requireActivity().window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        constLayoutMain.alpha = 0.5f
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoader() = with(binding) {
        constLayoutMain.alpha = 1f
        requireActivity().window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        progressBar.visibility = View.GONE
    }

    private fun handleResult(result: QRResult) {
        when (result) {
            is QRResult.QRSuccess -> {
                binding.txtSerialNumber.setText(result.content.rawValue)
                //showToast(result.content.rawValue.toString())//
                //Log.e("Scanned text", result.content.rawValue.toString())

                val rawText = result.content.rawValue
                val serialNumber = rawText?.let { extractValidatedSerial(it) }

                if (!serialNumber.isNullOrEmpty()) {
                    binding.txtSerialNumber.setText(serialNumber)
                    //showToast(serialNumber)
                    //Log.e("Scanned Serial", serialNumber)
                    clearFocus()
                    if (binding.txtSerialNumber.getTrimmedText().isEmpty()) {
                        showToast("Please enter or scan SR NO")
                    } else {
                        callSrNoApi()
                    }
                } else {
                    showToast("Invalid Serial Number")
                    Log.e("Scanned Serial", "No valid number found")
                }
            }

            QRResult.QRUserCanceled -> Log.e("handleResult", "User canceled")
            QRResult.QRMissingPermission -> Log.e("handleResult", "Missing permission")
            is QRResult.QRError -> Log.e(
                "handleResult",
                "${result.exception.javaClass.simpleName}: ${result.exception.localizedMessage}"
            )
        }
    }

    private fun extractValidatedSerial(text: String?): String? {

        if (text.isNullOrBlank()) return null

        return try {
            Regex("""\d{4,}""")  // minimum 4 digits
                .findAll(text)
                .map { it.value }
                .maxByOrNull { it.length }
        } catch (e: Exception) {
            null
        }
    }

    private fun scanQRCode() {
        scanCustomCode.launch(
            ScannerConfig.build {
                setBarcodeFormats(
                    listOf(
                        BarcodeFormat.FORMAT_QR_CODE,
                        BarcodeFormat.FORMAT_CODE_128,
                        BarcodeFormat.FORMAT_CODE_39
                    )
                )
                setOverlayStringRes(R.string.app_name) // string resource used for the scanner overlay
                setOverlayDrawableRes(R.mipmap.ic_launcher) // drawable resource used for the scanner overlay
                setHapticSuccessFeedback(false) // enable (default) or disable haptic feedback when a barcode was detected
                setShowTorchToggle(true) // show or hide (default) torch/flashlight toggle button
                setHorizontalFrameRatio(2.2f) // set the horizontal overlay ratio (default is 1 / square frame)
                setUseFrontCamera(false) // use the front camera
            }
        )
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
            deleteBtn.visibility = View.VISIBLE
            fileName.text = model.filePath ?: "Unknown file"

            downloadAttach.setOnClickListener {
                openAttachment(model.filePath)
            }
            // 🗑️ Delete (API call)
            deleteBtn.setSafeOnClickListener {
                viewModel.deleteMultiplePhotoAPI(
                    DeletePhotoRequest(
                        fUId = model.fUId ?: ""
                    )
                )
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

    private fun updateStatusUI(
        status: String?,
        textView: AppCompatTextView,
        matchedText: String,
        notMatchedText: String
    ) {
        val isMatched = status.orEmpty().contains("ok", true)

        val iconRes = if (isMatched) R.drawable.ic_verified else R.drawable.ic_danger
        val tintColor = ContextCompat.getColor(
            requireContext(),
            if (isMatched) R.color.white else R.color.red_1
        )
        val bgRes =
            if (isMatched) R.drawable.bg_rounded_fill_secondary else R.drawable.bg_rounded_fill_red
        textView.setTextColor(tintColor)
        textView.setBackgroundResource(bgRes)
        val drawable = ContextCompat.getDrawable(requireContext(), iconRes)?.mutate()
        drawable?.setTint(tintColor)

        textView.apply {
            text = if (isMatched) matchedText else notMatchedText
            setCompoundDrawablesWithIntrinsicBounds(drawable, null, ContextCompat.getDrawable(requireContext(),  R.drawable.ic_info)?.mutate(), null)
        }
    }

    /*=========================================================================================*/
    /*========================== Sr No Data   =============================================*/
    private fun callSrNoApi() = with(binding) {
        val request = GetSrNoListRequest(SrNo = txtSerialNumber.getTrimmedText())
        viewModel.getSrNoData(request)
    }

    private fun observeSrNoData() = with(binding) {
        //constLayoutCamGallery.visibility = View.GONE
        cropViewContainer.visibility = View.GONE
        constAiAnalysis.visibility = View.GONE
        constVarificationStatus.visibility = View.GONE
        linearAttachPhoto.visibility = View.GONE
        lylAttachPhotoList.visibility = View.GONE
        viewModel.srNoState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SrNoState.Idle -> {}
                is SrNoState.Loading -> showLoader()
                is SrNoState.Success -> {
                    hideLoader()
                    if (state.list.isNullOrEmpty()) {
                        showToast("No data found")
                    } else {
                        packAIEntryId = state.list[0].PackAIEntryId
                        state.list[0].SONo?.let {
                            customerName = it
                            txtSoNo.setText(it)
                        }
                        state.list[0].SerialNo?.let {
                            serialNo = it
                            constLayoutCamGallery.visibility = View.VISIBLE
                        }
                        state.list[0].CustomerName?.let {
                            customerName = it
                            txtCustomerName.setText(it)
                        }
                        state.list[0].ItmName?.let {
                            itemName = it
                            txtItemName.setText(it)
                        }
                        state.list[0].SoLnRemarks?.let {
                            txtSoRemark.setText(it)
                        }
                    }
                }

                is SrNoState.Empty -> {
                    hideLoader()
                    showToast(state.message)
                }

                is SrNoState.Error -> {
                    hideLoader()
                    showToast(state.message)
                }
            }
        }
    }

    private fun observeSavePackAIData() = with(binding) {
        viewModel.savePackState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SavePackState.Idle -> {}
                is SavePackState.Loading -> {
                    showLoader()
                }

                is SavePackState.Success -> {
                    hideLoader()
                    showToast(state.message)
                    parentFragmentManager.popBackStackImmediate()
                }

                is SavePackState.Error -> {
                    hideLoader()
                    showToast(state.message)
                }
            }
        }
    }

    private fun observeSaveMultiplePhoto() {
        viewModel.uploadAttachmentState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UploadAttachmentState.Loading -> {
                    showLoader()
                }

                is UploadAttachmentState.Success -> {
                    hideLoader()
                    packAIEntryId?.let {
                        viewModel.getUploadedPhotoAPI(recordId = it, formType = "PackAI OtherPhoto")
                    }
                }

                is UploadAttachmentState.Empty -> {
                    hideLoader()
                    showToast(state.message)
                }

                is UploadAttachmentState.Error -> {
                    hideLoader()
                    showToast(state.message)
                }

                else -> Unit
            }
        }
    }

    private fun observeGetPhotoData() {
        viewModel.photoState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is GetUploadedPhotoState.Loading -> showLoader()
                is GetUploadedPhotoState.Success -> {
                    hideLoader()
                    when (state.formType) {
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

    private fun observeDeletePhotoData() {
        viewModel.deletePhotoState.observe(viewLifecycleOwner) { state ->

            when (state) {
                is DeletePhotoState.Idle -> {}
                is DeletePhotoState.Loading -> showLoader()
                is DeletePhotoState.Success -> {
                    hideLoader()
                    showToast(state.message)
                    packAIEntryId?.let {
                        viewModel.getUploadedPhotoAPI(recordId = it, formType = "PackAI OtherPhoto")
                    }
                }

                is DeletePhotoState.Empty -> {
                    hideLoader()
                    showToast(state.message)
                }

                is DeletePhotoState.Error -> {
                    hideLoader()
                    showToast(state.message)
                }
            }
        }
    }

    private fun observeBrandName() = with(binding){
        viewModel.brandListState.observe(viewLifecycleOwner) { state ->

            when (state) {

                is BrandListState.Loading -> showLoader()

                is BrandListState.Success -> {
                    hideLoader()
                    val brandList = state.data
                    Log.e("BrandList",brandList.toString())
                    showBrandSelectionDialog(requireContext(), brandList) { selectedBrand ->
                        sharedPref.saveSelectedBrand(selectedBrand.BrandId ?: "", selectedBrand.Text ?: "")
                        txtBrandName.setText(selectedBrand.Text)
                    }
                    //manageInitApiCall()
                }

                is BrandListState.Empty -> {
                    hideLoader()
                    showToast(state.message)
                }

                is BrandListState.Error -> {
                    hideLoader()
                    showToast(state.message)
                }

                else -> {}
            }
        }
    }

    private fun observeSrNoDropDown(){
        viewModel.srNoListState.observe(viewLifecycleOwner) { state ->

            when (state) {

                is SrNoListState.Loading -> showLoader()

                is SrNoListState.Success -> {
                    hideLoader()
                    val srNoList = state.data.result
                }

                is SrNoListState.Empty -> {
                    hideLoader()
                    showToast(state.message)
                }

                is SrNoListState.Error -> {
                    hideLoader()
                    showToast(state.message)
                }

                else -> {}
            }
        }
    }

    private fun observeOcrApiCall() = with(binding) {
        viewModel.ocrState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is OcrState.Loading -> {}
                is OcrState.Success -> {
                    when (state.sourceType) {
                        "Pump" -> {
                            val ocrText = state.data.embossedText?.trim()?:""
                            val hasText = ocrText.isNotEmpty()
                            if (hasText) {
                                Toast.makeText(context, "OCR Completed", Toast.LENGTH_SHORT).show()
                                Log.e("Success", ocrText)
                                fullTextFromAILogo = ocrText
                            }
                            val customerName = txtCustomerName.getTrimmedText()
                            val brand = brandName
                            //val isMatched = hasText && customerName.contains(ocrText, ignoreCase = true)
                            val isMatched = hasText && !brand.isNullOrBlank() && BrandMatcher.isMatch(brand, customerName, ocrText)
                            topBodyStatus = if (isMatched) "OK" else "FAILED"
                            updateStatusUI(
                                status = if (isMatched) "OK" else "FAILED",
                                textView = txtBodyScan,
                                matchedText = "Logo scan matched",
                                notMatchedText = "Logo scan mismatched"
                            )

                            /*====================================================================*/
                            // 🔥 Start MLKit immediately (do NOT wait for crop)

                            fullTextFromAIPump = state.data.toString()

                            extractedSerialNoPump = state.data.serialNumber ?: "N/A"

                            binding.txtPercent.text = "50%"
                            binding.progressBarAnalysis.progress = 50

                            middleCroppedMotor = selectedBitmap
                            //binding.imgViewMidCropMotor.setImageBitmap(selectedBitmap)

                            Glide.with(this@AddPumpDataFragment).load(selectedBitmap).into(binding.imgViewMidCropMotor)
                            val isMatchedSrNo = (state.data.serialNumber ?: "").contains(serialNo, ignoreCase = true)
                            pumpStatus = if (isMatchedSrNo) "OK" else "FAILED"
                            val extractedType = ProductCodeExtractor.extract(state.data.type?:"")
                            val actualItemName = binding.txtItemName.getTrimmedText()

                            //Log.d("TextPresenceChecker", "========================================")
                            //Log.d("TextPresenceChecker", "Checking Extracted : $extractedType")
                            //Log.d("TextPresenceChecker", "Against Actual     : $actualItemName")
                            val result = TextPresenceChecker.check(extractedType, actualItemName)
                            if (result.isValid) {
                                pumpTypeStatus = "OK"
                                extractedTypePump = result.commonMatchedText
                                //Log.d("TextPresenceChecker", "FINAL RESULT    : ✅ Valid")
                                //Log.d("TextPresenceChecker", "Common Matched Text for API : ${result.commonMatchedText}")
                            } else {
                                pumpTypeStatus = "FAILED"
                                extractedTypePump = extractedType
                                //Log.d("TextPresenceChecker", "Common Matched Text for API : $extractedType")
                                //Log.d("TextPresenceChecker", "FINAL RESULT: ❌ Invalid — Extracted NOT found in Actual")
                            }

                            //Log.d("TextPresenceChecker", "========================================")

                            // Sr.No Text
                            updateStatusUI(
                                status = pumpStatus,
                                textView = binding.txtPumpSerialNo,
                                matchedText = "Pump serial number matched\n(Extracted : ${state.data.serialNumber ?: ""})",
                                notMatchedText = "Pump serial number mismatched\n(Extracted : ${state.data.serialNumber ?: ""})"
                            )
                            // Type Text
                            updateStatusUI(
                                status = pumpTypeStatus,
                                textView = binding.txtPumpTypeText,
                                matchedText = "Pump type matched\n(Extracted: $extractedTypePump)",
                                notMatchedText = "Pump type mismatched\n(Extracted: $extractedTypePump)"
                            )
                        }
                        "Motor" -> {
                            fullTextFromAIMotor = state.data.toString()
                            extractedSerialNoMotor = state.data.serialNumber ?: "N/A"
                            binding.txtPercent.text = "100%"
                            binding.progressBarAnalysis.progress = 100
                            middleCroppedPump = selectedBitmapMotor
                            //binding.imgViewMidCropPump.setImageBitmap(selectedBitmapMotor)
                            Glide.with(this@AddPumpDataFragment).load(selectedBitmapMotor).into(binding.imgViewMidCropPump)
                            val isMatched = (state.data.serialNumber ?: "").contains(serialNo, ignoreCase = true)
                            motorStatus = if (isMatched) "OK" else "FAILED"
                            val extractedType = ProductCodeExtractor.extract(state.data.type?:"")
                            val actualItemName = binding.txtItemName.getTrimmedText()
                            //Log.d("TextPresenceChecker", "========================================")
                            //Log.d("TextPresenceChecker", "Checking Extracted : $extractedType")
                            //Log.d("TextPresenceChecker", "Against Actual     : $actualItemName")
                            val result = TextPresenceChecker.check(extractedType, actualItemName)
                            if (result.isValid) {
                                motorTypeStatus = "OK"
                                extractedTypeMotor = result.commonMatchedText
                               // Log.d("TextPresenceChecker", "FINAL RESULT    : ✅ Valid")
                                //Log.d("TextPresenceChecker", "Common Matched Text for API : ${result.commonMatchedText}")

                            } else {
                                motorTypeStatus = "FAILED"
                                extractedTypeMotor = extractedType
                                //Log.d("TextPresenceChecker", "Common Matched Text for API : ${result.commonMatchedText}")
                                //Log.d("TextPresenceChecker", "FINAL RESULT: ❌ Invalid — Extracted NOT found in Actual")
                            }

                            //Log.d("TextPresenceChecker", "========================================")

                            // Sr.No Text
                            updateStatusUI(
                                status = motorStatus,
                                textView = binding.txtMotorSerialNo,
                                matchedText = "Motor serial number matched\n(Extracted: ${state.data.serialNumber ?: "N/A"})",
                                notMatchedText = "Motor serial number mismatched\n(Extracted: ${state.data.serialNumber ?: "N/A"})"
                            )
                            // Type Text
                            updateStatusUI(
                                status = motorTypeStatus,
                                textView = binding.txtMotorTypeText,
                                matchedText = "Motor type matched\n(Extracted: $extractedTypeMotor)",
                                notMatchedText = "Motor type mismatched\n(Extracted: $extractedTypeMotor)"
                            )
                        }
                    }
                }
                is OcrState.Error -> {
                    showToast(state.message)
                }
                else -> Unit
            }
        }
    }
}
