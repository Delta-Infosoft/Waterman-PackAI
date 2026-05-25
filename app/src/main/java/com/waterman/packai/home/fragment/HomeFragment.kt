package com.waterman.packai.home.fragment

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.waterman.packai.R
import com.waterman.packai.base.BaseFragment
import com.waterman.packai.databinding.FragmentHomeBinding
import com.waterman.packai.home.activity.HomeActivity
import com.waterman.packai.home.adapter.ProductListAdapter
import com.waterman.packai.home.viewmodel.BrandListState
import com.waterman.packai.home.viewmodel.HomeViewModel
import com.waterman.packai.home.viewmodel.ProductListState
import com.waterman.packai.network.response.ProductList
import com.waterman.packai.utils.Constants
import com.waterman.packai.utils.Constants.setSafeOnClickListener
import com.waterman.packai.utils.Constants.showBrandSelectionDialog
import com.waterman.packai.utils.EncryptedPrefHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment() {
    private lateinit var binding : FragmentHomeBinding
    @Inject lateinit var sharedPref: EncryptedPrefHelper
    private var activeFilter: String = "ALL"

    // Pagination
    private var currentPage: Int = 1
    private val pageSize: Int = 10

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    private var filterFromDate: Date = dateFormatter.parse(dateFormatter.format(Date()))!!
    private var filterToDate: Date = dateFormatter.parse(dateFormatter.format(Date()))!!
    private var fullList: List<ProductList> = emptyList()
    private val viewModel: HomeViewModel by viewModels()
    private val checkListAdapter by lazy {
        ProductListAdapter { selectedItem ->
            filterFromDate = dateFormatter.parse(dateFormatter.format(Date()))!!
            filterToDate   = dateFormatter.parse(dateFormatter.format(Date()))!!
            val fragment = DetailsScreenFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("CHECK_ITEM", selectedItem)
                }
            }
            loadFragment(fragment = fragment, isAdd = false, isAddBackStack = true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater,container,false)
        return binding.root
    }

    // In your Fragment
    override fun onResume() {
        super.onResume()
        val logOutIcon = requireActivity().findViewById<AppCompatImageView>(R.id.imgViewLogOut)
        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.toolbarHome)
        val toolbarTitle = requireActivity().findViewById<AppCompatTextView>(R.id.txtViewTitle)
        // Set gradient background
        toolbar.setBackgroundResource(R.drawable.bg_gradient)
        toolbarTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        ImageViewCompat.setImageTintList(logOutIcon,
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
        )
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        manageToolBar()
        setUpRecyclerView()
        moveOnClickListeners()
        manageSearchView()

        callInitApi()
        observeListingData()
        observeBrandName()
        Log.e("User", sharedPref.getUser().toString() )

        binding.txtTodaySummary.post {

            val textView = binding.txtTodaySummary

            val shader = LinearGradient(
                0f, 0f,
                0f, textView.height.toFloat(),   // vertical gradient
                Color.parseColor("#48CFD6"),
                Color.parseColor("#CE82E7"),
                Shader.TileMode.CLAMP
            )

            textView.paint.shader = shader
        }
    }
    private fun callInitApi() {
        Log.e("Date Filter","${dateFormatter.format(filterFromDate)}, ${dateFormatter.format(filterToDate)}")
        viewModel.productListingAPI(filterFromDate,filterToDate)
    }
    private fun moveOnClickListeners() = with(binding){
        imgViewAdd.setSafeOnClickListener {
            filterFromDate = dateFormatter.parse(dateFormatter.format(Date()))!!
            filterToDate   = dateFormatter.parse(dateFormatter.format(Date()))!!
            if (!sharedPref.isBrandSelected()) {
                viewModel.getBrandList()
            }else{
                val brandId = sharedPref.getSelectedBrandId() ?: ""
                val brandName = sharedPref.getSelectedBrandName() ?: ""
                loadFragment(fragment = AddPumpDataFragment.newInstance(brandId, brandName), isAdd = false, isAddBackStack = true)
            }
        }

        // ── Verified filter ──────────────────────────────────────────
        constViewLeft.setSafeOnClickListener {
            activeFilter = if (activeFilter == "VERIFIED") "ALL" else "VERIFIED"
            applyFilter()
        }

        // ── Rejected filter ──────────────────────────────────────────
        constViewRight.setSafeOnClickListener {
            activeFilter = if (activeFilter == "REJECTED") "ALL" else "REJECTED"
            applyFilter()
        }
    }

    private fun applyFilter() {
        var result = fullList.toList()

        // DEBUG — see exact status values coming from API
        fullList.forEach {
            Log.d("FilterDebug", "Pump=${it.PumpSerialNoStatus} Motor=${it.MotorSerialNoStatus} Body=${it.TopBodyStatus}")
        }

        result = when (activeFilter) {
            "VERIFIED" -> result.filter {
                it.PumpSerialNoStatus.equals("ok", true) &&
                        it.MotorSerialNoStatus.equals("ok", true) &&
                        it.TopBodyStatus.equals("ok", true)&&
                        it.PumpTypeStatus.equals("ok", true) &&
                        it.MotorTypeStatus.equals("ok", true)
            }
            "REJECTED" -> result.filter {
                !(it.PumpSerialNoStatus.equals("ok", true) &&
                        it.MotorSerialNoStatus.equals("ok", true) &&
                        it.TopBodyStatus.equals("ok", true)&&
                        it.PumpTypeStatus.equals("ok", true) &&
                                it.MotorTypeStatus.equals("ok", true))
            }
            else -> result
        }

        Log.d("FilterDebug", "activeFilter=$activeFilter  result=${result.size}  fullList=${fullList.size}")

        checkListAdapter.submitList(result.toList())
    }

    private fun setUpRecyclerView() = with(binding) {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerViewList.apply {
            this.layoutManager = layoutManager
            adapter = checkListAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy <= 0) return

                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount   = layoutManager.itemCount
                    val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()
                    val isAtBottom       = (visibleItemCount + firstVisibleItem) >= totalItemCount - 2

                    //Log.d("PAGINATION", "Scroll → visible=$visibleItemCount  total=$totalItemCount  first=$firstVisibleItem  atBottom=$isAtBottom")

                    if (isAtBottom && hasMorePages()) {
                        Log.d("PAGINATION", "▶ Loading next page → ${currentPage + 1}")
                        currentPage++
                        applyListState(resetPage = false)
                    } else if (isAtBottom && !hasMorePages()) {
                        Log.d("PAGINATION", "✓ All ${fullList.size} items loaded — no more pages")
                    }
                }
            })
        }
    }

    private fun applyListState(resetPage: Boolean = true) {
        if (resetPage) currentPage = 1

        val filtered = getFilteredList()
        val totalPages = if (filtered.isEmpty()) 1 else (filtered.size + pageSize - 1) / pageSize
        currentPage = currentPage.coerceIn(1, totalPages)

        val fromIndex = 0
        val toIndex   = (currentPage * pageSize).coerceAtMost(filtered.size)

        val pageData = filtered.subList(fromIndex, toIndex)
        checkListAdapter.submitList(pageData.toList())

        // ── Pagination Log ────────────────────────────────────────────
        Log.d("PAGINATION", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d("PAGINATION", "API total    : ${fullList.size} items")
        Log.d("PAGINATION", "After filter : ${filtered.size} items  [filter=$activeFilter]")
        Log.d("PAGINATION", "Page         : $currentPage of $totalPages")
        Log.d("PAGINATION", "Showing      : ${pageData.size} items  (index $fromIndex–$toIndex)")
        Log.d("PAGINATION", "Has more     : ${currentPage < totalPages}")
        Log.d("PAGINATION", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private fun hasMorePages(): Boolean {
        val filtered = getFilteredList()
        val totalPages = (filtered.size + pageSize - 1) / pageSize
        return currentPage < totalPages
    }

    private fun getFilteredList(): List<ProductList> {
        var result = fullList.toList()
        result = when (activeFilter) {
            "VERIFIED" -> result.filter {
                it.PumpSerialNoStatus.equals("ok", true) &&
                        it.MotorSerialNoStatus.equals("ok", true) &&
                        it.TopBodyStatus.equals("ok", true)&&
                        it.PumpTypeStatus.equals("ok", true) &&
                        it.MotorTypeStatus.equals("ok", true)
            }
            "REJECTED" -> result.filter {
                !(it.PumpSerialNoStatus.equals("ok", true) &&
                        it.MotorSerialNoStatus.equals("ok", true) &&
                        it.TopBodyStatus.equals("ok", true)&&
                        it.PumpTypeStatus.equals("ok", true) &&
                        it.MotorTypeStatus.equals("ok", true))
            }
            else -> result
        }
        return result
    }
    private fun manageToolBar() {
        (activity as HomeActivity).apply {
            manageToolBar(isVisible = true)
            manageToolBarTitle(title = "Home")
            manageBackButtonClick(isVisible = true)
            manageDrawerLock(isDrawerVisible = true)
            setDrawerEnabled(enabled = true)
            logOutButtonManage(false)
            manageHomeToolbar()
            setFilterClickListener(object : HomeActivity.OnFilterClickListener {
                    override fun onFilterClicked() {
                        // Show filter options — Verified / Rejected / All
                        showDateFilterDialog()
                    }
                })
        }
    }

    override fun onPause() {
        super.onPause()
        (activity as HomeActivity).setFilterClickListener(null)
    }
    private fun observeBrandName() = with(binding){
        viewModel.brandListState.observe(viewLifecycleOwner) { state ->

            when (state) {

                is BrandListState.Loading -> showLoader()

                is BrandListState.Success -> {
                    hideLoader()
                    if (!sharedPref.isBrandSelected()) {
                        val brandList = state.data
                        Log.e("BrandList",brandList.toString())
                        showBrandSelectionDialog(requireContext(), brandList) { selectedBrand ->
                            val brandId = selectedBrand.BrandId ?: ""
                            val brandName = selectedBrand.Text ?: ""

                            sharedPref.saveSelectedBrand(brandId, brandName)
                            loadFragment(fragment = AddPumpDataFragment.newInstance(brandId, brandName), isAdd = false, isAddBackStack = true)
                        }
                    }
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
    private fun observeListingData(){
        viewModel.productListState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProductListState.Idle -> {}
                is ProductListState.Loading -> showLoader()
                is ProductListState.Success -> {
                    hideLoader()

                    val list = state.data ?: emptyList()

                    val verifiedCount = list.count {
                        it.PumpSerialNoStatus.equals("ok", true) &&
                                it.MotorSerialNoStatus.equals("ok", true) &&
                                it.TopBodyStatus.equals("ok", true)&&
                                it.PumpTypeStatus.equals("ok", true) &&
                                it.MotorTypeStatus.equals("ok", true)
                    }

                    val rejectedCount = list.size - verifiedCount

                    binding.txtViewVerifiedCount.text = verifiedCount.toString()
                    binding.txtViewRejectedCount.text = rejectedCount.toString()


                    val todayFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
                    val todayDate = todayFormat.format(Date())

                    // ✅ Filter today's records
                    val todayList = list.filter { item ->
                        item.InsertedOn?.startsWith(todayDate) == true
                    }

                    val totalScanToday = todayList.size

                    val totalVerifiedToday = todayList.count {
                        it.PumpSerialNoStatus.equals("ok", true) &&
                                it.MotorSerialNoStatus.equals("ok", true) &&
                                it.TopBodyStatus.equals("ok", true)&&
                                it.PumpTypeStatus.equals("ok", true) &&
                                it.MotorTypeStatus.equals("ok", true)
                    }

                    val totalRejectedToday = totalScanToday - totalVerifiedToday

                    // ✅ Set values
                    binding.txtScanCount.text = totalScanToday.toString()
                    binding.txtVerifiedCount.text = totalVerifiedToday.toString()
                    binding.txtRejectedCount.text = totalRejectedToday.toString()

                    fullList = list.reversed().toList()
                    checkListAdapter.submitList(fullList.toList())
                }
                is ProductListState.Empty -> {
                    //hideLoader()
                    //showToast(state.message)

                    hideLoader()

                    // Clear all stored data
                    fullList = emptyList()

                    // Clear recycler view
                    checkListAdapter.submitList(emptyList())

                    // Reset counts
                    binding.txtViewVerifiedCount.text = "0"
                    binding.txtViewRejectedCount.text = "0"

                    binding.txtScanCount.text = "0"
                    binding.txtVerifiedCount.text = "0"
                    binding.txtRejectedCount.text = "0"

                    // Reset pagination
                    currentPage = 1

                    showToast(state.message)
                }
                is ProductListState.Error -> {
                    //hideLoader()
                    //showToast(state.message)

                    hideLoader()

                    fullList = emptyList()
                    checkListAdapter.submitList(emptyList())

                    binding.txtViewVerifiedCount.text = "0"
                    binding.txtViewRejectedCount.text = "0"

                    binding.txtScanCount.text = "0"
                    binding.txtVerifiedCount.text = "0"
                    binding.txtRejectedCount.text = "0"

                    currentPage = 1

                    showToast(state.message)
                }
            }
        }
    }
    private fun showLoader() = with(receiver = binding){
        requireActivity().window?.setFlags(/* flags = */ WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, /* mask = */WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        progressBar.visibility = View.VISIBLE
    }
    private fun hideLoader() = with(receiver = binding){
        requireActivity().window?.clearFlags(/* flags = */ WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        progressBar.visibility = View.GONE
    }
    private fun manageSearchView(){
        binding.searchView.queryHint = "Search by sr.no"
        val closeButton = binding.searchView.findViewById<ImageView>(
            androidx.appcompat.R.id.search_close_btn
        )
        // Remove underline
        val searchPlate = binding.searchView.findViewById<View>(
            androidx.appcompat.R.id.search_plate
        )
        searchPlate.background = null

        closeButton.visibility = View.VISIBLE
        closeButton.setOnClickListener {
            Constants.hideKeyboard(it)
            binding.searchView.clearFocus()
            binding.searchView.setQuery("", false)
        }
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d("Search", "onQueryTextSubmit: $query")
                filterList(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.d("Search", "onQueryTextChange: $newText")
                filterList(newText)
                return true
            }
        })
    }
    private fun filterList(query: String?) {
        Log.d("Search", "Full List Size: ${fullList.size}")
        Log.d("Search", "Search Query: $query")


        if (query.isNullOrBlank()) {
            Log.d("Search", "Query Empty -> Showing Full List")
            checkListAdapter.submitList(fullList.toList())
            return
        }

        val filteredList = fullList.filter {
            it.SrNo?.contains(query, ignoreCase = true) == true
        }
        checkListAdapter.submitList(filteredList.toList())
    }

    private fun showDateFilterDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_date_filter, null)

        val txtFromDate = dialogView.findViewById<AppCompatTextView>(R.id.txtFromDate)
        val txtToDate   = dialogView.findViewById<AppCompatTextView>(R.id.txtToDate)
        val btnApply    = dialogView.findViewById<AppCompatTextView>(R.id.btnApply)
        val btnReset    = dialogView.findViewById<AppCompatTextView>(R.id.btnReset)

        val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        var selectedFrom: Date? = filterFromDate
        var selectedTo:   Date? = filterToDate

        // Show already selected dates if any
        txtFromDate.text = selectedFrom?.let { displayFormat.format(it) } ?: "Select From Date"
        txtToDate.text   = selectedTo?.let   { displayFormat.format(it) } ?: "Select To Date"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        txtFromDate.setOnClickListener {
            val cal = Calendar.getInstance()
            selectedFrom?.let { cal.time = it }

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                R.style.MyDatePickerTheme,
                { _, year, month, day ->
                    val picked = Calendar.getInstance().apply {
                        set(year, month, day, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                    selectedFrom = picked
                    txtFromDate.text = displayFormat.format(picked)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )

            // ❗ Restrict future dates
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

            datePickerDialog.show()
        }

        txtToDate.setOnClickListener {
            val cal = Calendar.getInstance()
            selectedTo?.let { cal.time = it }

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                R.style.MyDatePickerTheme,
                { _, year, month, day ->
                    val picked = Calendar.getInstance().apply {
                        set(year, month, day, 23, 59, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.time

                    selectedTo = picked
                    txtToDate.text = displayFormat.format(picked)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )

            // ❗ Restrict future dates
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

            datePickerDialog.show()
        }

        // Apply
        btnApply.setOnClickListener {
            if (selectedFrom == null || selectedTo == null) {
                showToast("Please select both from and to date")
                return@setOnClickListener
            }
            if (selectedFrom!!.after(selectedTo)) {
                showToast("From date cannot be after To date")
                return@setOnClickListener
            }
            filterFromDate = selectedFrom
            filterToDate   = selectedTo
            dialog.dismiss()
            applyListState(resetPage = true)
            Log.d("DateFilter", "From: ${displayFormat.format(filterFromDate!!)}  To: ${displayFormat.format(filterToDate!!)}")
            callInitApi()
        }

        // Reset
        btnReset.setOnClickListener {
            filterFromDate = dateFormatter.parse(dateFormatter.format(Date()))!!
            filterToDate   = dateFormatter.parse(dateFormatter.format(Date()))!!
            dialog.dismiss()
            applyListState(resetPage = true)
            Log.d("DateFilter", "Date filter cleared")
            callInitApi()
        }

        dialog.show()
    }


}