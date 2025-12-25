package com.example.myapplication.data

import com.example.myapplication.utils.Utils

/**
 * Created by wtuadn on 2025/12/24.
 */
data class MaxDrawDownData(
    var topData: AlignedMAData = AlignedMAData(),
    var bottomData: AlignedMAData = AlignedMAData(),
) {
    var fixedData: AlignedMAData? = null
    val drawDown: Double get() = if (bottomData.closePrice == 0.0) 0.0 else (bottomData.closePrice - topData.closePrice) / topData.closePrice

    override fun toString(): String {
        var string = "最大回撤:${Utils.getPercentageString(drawDown)}" +
            " 持续天数:${(Utils.dateToTimestamp(bottomData.date) - Utils.dateToTimestamp(topData.date)) / (24 * 60 * 60)} ${topData.date} - ${bottomData.date}"
        if (fixedData == null) {
            string += " 修复时间:修复中"
        } else {
            string += " 修复时间:${(Utils.dateToTimestamp(fixedData!!.date) - Utils.dateToTimestamp(bottomData.date)) / (24 * 60 * 60)} ${bottomData.date} - ${fixedData!!.date}"
        }
        return string
    }
}
