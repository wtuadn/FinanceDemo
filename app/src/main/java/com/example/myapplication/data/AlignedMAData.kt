package com.example.myapplication.data

import com.example.myapplication.utils.Utils

data class AlignedMAData(
    val date: String = Utils.timestampToDate(System.currentTimeMillis() / 1000),
    val closePrice: Double = 0.0,
    val volume: Long = 0L,
    val shortMAValue: Double = 0.0,
    val middleMAValue: Double = 0.0,
    val longMAValue: Double = 0.0,
)