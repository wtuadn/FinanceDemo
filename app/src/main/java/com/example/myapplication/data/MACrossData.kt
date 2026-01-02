package com.example.myapplication.data

import com.example.myapplication.utils.Utils

/**
 * Created by wtuadn on 2025/12/15.
 */
data class MACrossData(
    val enterData: AlignedMAData,
    val exitData: AlignedMAData,
){
    val percentage: Double = (exitData.kLineData.closePrice - enterData.kLineData.closePrice) / enterData.kLineData.closePrice

    val dayCount :Int = ((Utils.dateToTimestamp(exitData.kLineData.date) - Utils.dateToTimestamp(enterData.kLineData.date)) / (24 * 60 * 60)).toInt()

    override fun toString(): String {
        return "${enterData.kLineData.date}——${exitData.kLineData.date} " +
            "涨幅: ${Utils.getPercentageString(percentage)} "
    }
}
