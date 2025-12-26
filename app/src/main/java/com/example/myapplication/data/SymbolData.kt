package com.example.myapplication.data

import com.example.myapplication.utils.MACrossUtils.MAType

/**
 * Created by wtuadn on 2025/12/22.
 */
data class SymbolData(
    val code: String,
    val desc: String,
    var scale: Int = 240,
    var d: Int = 1,
    var shortMA: Int = 1,
    var longMA: Int = 20,
    var maType: MAType = MAType.SMA,
    var upCrossDiffRate: Double = 0.000,
    var downCrossDiffRate: Double = 0.000,
    var middleMA: Int = 10,
)
