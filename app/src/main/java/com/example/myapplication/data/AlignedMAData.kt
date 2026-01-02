package com.example.myapplication.data

data class AlignedMAData(
    val kLineData: KLineData,
    val shortMAValue: Double = 0.0,
    val longMAValue: Double = 0.0,
)