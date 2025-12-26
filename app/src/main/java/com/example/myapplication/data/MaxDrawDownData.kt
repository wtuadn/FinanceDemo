package com.example.myapplication.data

import com.example.myapplication.utils.Utils

/**
 * Created by wtuadn on 2025/12/24.
 */
data class MaxDrawDownData(
    val maxDrawDownRate: Double = 0.0, // 最大回撤率
    val peakDate: String = "",         // 回撤前的最高点日期（波峰）
    val valleyDate: String = "",       // 跌到最低点的日期（波谷）
    val recoveryDate: String? = null,  // 修复日期（净值重新回到 peakValue 的日期），如果未修复则为 null
    val peakValue: Double = 0.0        // 当时的最高净值
) {

    override fun toString(): String {
        var string = "最大回撤:${Utils.getPercentageString(maxDrawDownRate)}" +
            " 持续天数:${(Utils.dateToTimestamp(valleyDate) - Utils.dateToTimestamp(peakDate)) / (24 * 60 * 60)} $peakDate - $valleyDate"
        if (recoveryDate == null) {
            string += " 修复时间:修复中"
        } else {
            string += " 修复时间:${(Utils.dateToTimestamp(recoveryDate) - Utils.dateToTimestamp(valleyDate)) / (24 * 60 * 60)} $valleyDate - $recoveryDate"
        }
        return string
    }
}
