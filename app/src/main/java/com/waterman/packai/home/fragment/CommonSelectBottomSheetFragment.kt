package com.waterman.packai.home.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.waterman.packai.databinding.BottomSheetFragmentCommonSelectBinding
import com.waterman.packai.home.data.CommonSelect
import com.waterman.packai.home.data.MediaPickType

class CommonSelectBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetFragmentCommonSelectBinding

    private var mode: CommonSelect? = null
    private var onItemSelected: ((MediaPickType) -> Unit)? = null

    companion object {
        private const val ARG_MODE = "arg_mode"

        fun newInstance(mode: CommonSelect): CommonSelectBottomSheetFragment {
            return CommonSelectBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_MODE, mode)
                }
            }
        }
    }

    fun setDismissCallback(callback: (MediaPickType) -> Unit) {
        onItemSelected = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = arguments?.getSerializable(ARG_MODE) as? CommonSelect
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BottomSheetFragmentCommonSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupClicks()
    }

    private fun setupUI() = with(binding) {
        when (mode) {

            CommonSelect.CAMERA_GALLERY -> {
                constCameraGallery.visibility = View.VISIBLE
                constDocumentFile.visibility = View.GONE
            }

            CommonSelect.DOCUMENT_FILE -> {
                constCameraGallery.visibility = View.GONE
                constDocumentFile.visibility = View.VISIBLE
            }

            else -> Unit
        }
    }

    private fun setupClicks() = with(binding) {

        // 📸 Camera
        txtViewCamera.setOnClickListener {
            onItemSelected?.invoke(MediaPickType.CAMERA)
            dismiss()
        }

        // 🖼 Gallery
        txtViewGallery.setOnClickListener {
            onItemSelected?.invoke(MediaPickType.GALLERY)
            dismiss()
        }

        // 📄 Document/File
        txtViewDocumentFile.setOnClickListener {
            onItemSelected?.invoke(MediaPickType.DOCUMENT)
            dismiss()
        }
    }

}