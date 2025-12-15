package com.example.myapplication.data

data class AlignedMAData(
    val date: String,
    val closePrice: Double,
    val volume: Long,
    val shortMAValue: Double,
    val longMAValue: Double,
)