package com.example.myapplication.data

/**
 * Created by wtuadn on 2025/12/15.
 */
data class MACrossData(
    val enterData: AlignedMAData,
    val exitData: AlignedMAData,
){
    val percentage: Double = (exitData.closePrice - enterData.closePrice) / enterData.closePrice
}
