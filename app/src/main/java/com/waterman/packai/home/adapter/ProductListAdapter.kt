package com.waterman.packai.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.waterman.packai.R
import com.waterman.packai.databinding.RowItemListBinding
import com.waterman.packai.network.response.ProductList
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProductListAdapter(
    private val onItemClick: (ProductList) -> Unit
) : ListAdapter<ProductList, ProductListAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(
        private val binding: RowItemListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProductList) = with(binding) {

            txtViewSoNo.text = item.SONo
            txtSerialValue.text = item.SrNo
            txtDateValue.text = formatDateTime(item.InsertedOn)

            root.setOnClickListener {
                onItemClick(item)
            }

            val pumpOk = item.PumpSerialNoStatus.equals(other = "ok", ignoreCase = true)
            val motorOk = item.MotorSerialNoStatus.equals(other = "ok", ignoreCase = true)
            val bodyOk = item.TopBodyStatus.equals(other = "ok", ignoreCase = true)
            val pumpTypeStatus = item.PumpTypeStatus.equals(other = "ok", ignoreCase = true)
            val motorTypeStatus = item.MotorTypeStatus.equals(other = "ok", ignoreCase = true)


            val isVerified = pumpOk && motorOk && bodyOk && pumpTypeStatus && motorTypeStatus

            if (isVerified) {
                constLayMain.setBackgroundResource(R.drawable.bg_bordered_rounded_10_primary)
                txtVerified.text = "Verified"
                txtVerified.setTextColor(root.context.getColor(R.color.white))
                txtVerified.setBackgroundResource(R.drawable.bg_rounded_fill_secondary)
                txtVerified.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_verified, 0, 0, 0)

                viewStart.setBackgroundResource(R.color.primary)

                txtAiConfirm.text = "AI confirms 100%"
            } else {
                constLayMain.setBackgroundResource(R.drawable.bg_bordered_rounded_10_red)

                txtVerified.text = "Rejected"
                txtVerified.setTextColor(root.context.getColor(R.color.red_1))
                txtVerified.setBackgroundResource(R.drawable.bg_rounded_fill_red)
                txtVerified.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_close_new, 0, 0, 0)

                viewStart.setBackgroundResource(R.color.red_3)
                txtAiConfirm.text = "AI confirms 0%"
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            RowItemListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun formatDateTime(input: String?): String {
        if (input.isNullOrEmpty()) return ""

        return try {
            val inputFormat = SimpleDateFormat(
                "dd-MMM-yyyy hh:mm:ss a",
                Locale.ENGLISH   // IMPORTANT for "Feb"
            )

            val outputFormat = SimpleDateFormat(
                "dd-MMM-yy hh:mm a",
                Locale.ENGLISH
            )

            inputFormat.parse(input)?.let {
                outputFormat.format(it)
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ProductList>() {

        override fun areItemsTheSame(
            oldItem: ProductList,
            newItem: ProductList
        ): Boolean {
            return oldItem.PackAIEntryId == newItem.PackAIEntryId &&
                    oldItem.SrNo == newItem.SrNo
        }

        override fun areContentsTheSame(
            oldItem: ProductList,
            newItem: ProductList
        ): Boolean {
            return oldItem == newItem
        }
    }
}