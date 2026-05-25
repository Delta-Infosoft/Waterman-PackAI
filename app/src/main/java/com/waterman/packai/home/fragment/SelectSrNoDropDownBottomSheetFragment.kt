package com.waterman.packai.home.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.waterman.packai.databinding.BottomSheetFragmentSelectTravelByBinding
import com.waterman.packai.home.adapter.SelectSrNoAdapter
import com.waterman.packai.network.response.BrandList
import kotlin.apply
import kotlin.collections.filter
import kotlin.collections.toList
import kotlin.text.contains
import kotlin.text.isNullOrBlank

class SelectSrNoDropDownBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetFragmentSelectTravelByBinding
    private lateinit var selectTravelByAdapter: SelectSrNoAdapter

    private var fullList: List<BrandList> = emptyList()
    private var districtList: ArrayList<BrandList> = arrayListOf()

    companion object {
        private const val ARG_LIST = "district_for_list"

        fun newInstance(list: List<BrandList>): SelectSrNoDropDownBottomSheetFragment {
            return SelectSrNoDropDownBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_LIST, kotlin.collections.ArrayList(list))
                }
            }
        }
    }

    private var dismissCallback: ((BrandList) -> Unit)? = null

    fun setDismissCallback(callback: (BrandList) -> Unit) {
        dismissCallback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ SAFE: Only read arguments here
        districtList =
            arguments?.getParcelableArrayList(ARG_LIST) ?: arrayListOf()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetFragmentSelectTravelByBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
        manageSearchView()
    }

    private fun setUpRecyclerView() = with(binding) {

        selectTravelByAdapter = SelectSrNoAdapter { selectedStatus ->
            dismissCallback?.invoke(selectedStatus)
            dismiss()
        }

        recyclerViewStatus.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectTravelByAdapter
        }

        fullList = districtList.toList()
        // ✅ Set data AFTER adapter initialization
        selectTravelByAdapter.submitList(districtList)
    }

    private fun manageSearchView(){
        binding.searchView.queryHint = "Search"
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                filterList(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText)
                return true
            }
        })
    }

    private fun filterList(query: String?) {
        if (query.isNullOrBlank()) {
            selectTravelByAdapter.submitList(fullList)
            return
        }

        val filteredList = fullList.filter {
            it.Text?.contains(query, ignoreCase = true) == true
        }

        selectTravelByAdapter.submitList(filteredList)
    }
}