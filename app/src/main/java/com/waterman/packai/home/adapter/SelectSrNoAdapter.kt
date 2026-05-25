package com.waterman.packai.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.waterman.packai.databinding.RowItemSelectStatusBinding
import com.waterman.packai.network.response.BrandList

class SelectSrNoAdapter(
    private val onItemClick: (BrandList) -> Unit
) : ListAdapter<BrandList, SelectSrNoAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(
        private val binding: RowItemSelectStatusBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: BrandList) = with(binding) {
            txtViewStatus.text = data.Text

            root.setOnClickListener {
                onItemClick(data)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            RowItemSelectStatusBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<BrandList>() {
        override fun areItemsTheSame(oldItem: BrandList, newItem: BrandList): Boolean {
            return oldItem.BrandId == newItem.BrandId
        }

        override fun areContentsTheSame(oldItem: BrandList, newItem: BrandList): Boolean {
            return oldItem == newItem
        }
    }
}