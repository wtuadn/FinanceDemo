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
    val peakValue: Double = 0.0,        // 当时的最高净值
    val maxLossFromBuyRate: Double = 0.0, // 买入后最大本金损失率
    val maxLossFromBuyDate: String = "",   // 买入后最大损失日期
    val maxLossFromBuyStartDate: String = "", // 买入后最大损失开始的日期（首次达到最大损失的日期）
    val maxLossFromBuyRecoveryDate: String? = null, // 买入后最大损失的修复日期
) {

    override fun toString(): String {
        var string = "最大回撤:${Utils.getPercentageString(maxDrawDownRate)}"
        if (maxDrawDownRate < 0) {
            string += " 持续天数:${(Utils.dateToTimestamp(valleyDate) - Utils.dateToTimestamp(peakDate)) / (24 * 60 * 60)} $peakDate - $valleyDate"
        }
        if (recoveryDate == null) {
            string += " 修复时间:修复中"
        } else {
            string += " 修复时间:${(Utils.dateToTimestamp(recoveryDate) - Utils.dateToTimestamp(valleyDate)) / (24 * 60 * 60)} $valleyDate - $recoveryDate"
        }
        string += "\n最大本金损失率:${Utils.getPercentageString(maxLossFromBuyRate)}"
        if (maxLossFromBuyRate < 0) {
            string += " 持续天数:${(Utils.dateToTimestamp(maxLossFromBuyDate) - Utils.dateToTimestamp(maxLossFromBuyStartDate)) / (24 * 60 * 60)} $maxLossFromBuyStartDate - $maxLossFromBuyDate"
        }
        if (maxLossFromBuyRecoveryDate == null) {
            string += " 修复时间:修复中"
        } else {
            string += " 修复时间:${(Utils.dateToTimestamp(maxLossFromBuyRecoveryDate) - Utils.dateToTimestamp(maxLossFromBuyDate)) / (24 * 60 * 60)} $maxLossFromBuyDate - $maxLossFromBuyRecoveryDate"
        }
        return string
    }
}
