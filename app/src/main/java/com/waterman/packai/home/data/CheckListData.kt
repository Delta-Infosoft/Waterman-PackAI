package com.waterman.packai.home.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CheckListData(
    val workOrder: String,
    val pumpDutId: String,
    val testDate: String,
    val pumpStatus: String,
    val motorStatus: String,
    val bodyStatus: String
): Parcelable