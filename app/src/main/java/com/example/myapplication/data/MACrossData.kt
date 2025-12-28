package com.example.myapplication.data

import com.example.myapplication.utils.Utils

/**
 * Created by wtuadn on 2025/12/15.
 */
data class MACrossData(
    val enterData: AlignedMAData,
    val exitData: AlignedMAData,
){
    val percentage: Double = (exitData.closePrice - enterData.closePrice) / enterData.closePrice

    val dayCount :Int = ((Utils.dateToTimestamp(exitData.date) - Utils.dateToTimestamp(enterData.date)) / (24 * 60 * 60)).toInt()

    override fun toString(): String {
        return "${enterData.date}——${exitData.date} " +
            "涨幅: ${Utils.getPercentageString(percentage)} "
    }
}
