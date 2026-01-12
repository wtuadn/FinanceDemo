package com.example.myapplication.data

import com.example.myapplication.utils.Utils

/**
 * Created by wtuadn on 2025/12/15.
 */
data class MACrossResult(
    val alignedMAData: List<AlignedMAData>,
    val totalCrossData: GroupedMACrossData, //总结果
    val yearCrossDataMap: Map<Int, GroupedMACrossData>, // 按年结果
    val maxDrawDownData: MaxDrawDownData,
    val tradeSignalDataList: List<TradeSignalData>,
) {
    val yearlyPercentage = totalCrossData.totalPercentage / yearCrossDataMap.size

    fun getTotalDesc(): String {
        val countlyPercentage = totalCrossData.countlyPercentage
        val dailyPercentage = totalCrossData.dailyPercentage
        val yearlyPercentage = yearlyPercentage
        val totalStr = totalCrossData.toString()
        val insert = totalCrossData.groupKey.length + 1
        return "${totalStr.take(insert)}" +
            "平均次涨幅：${Utils.getPercentageString(countlyPercentage)} " +
            "平均天涨幅：${Utils.getPercentageString(dailyPercentage)} " +
            "平均年涨幅：${Utils.getPercentageString(yearlyPercentage)} " +
            "${totalStr.substring(insert)} \n$maxDrawDownData"
    }

    override fun toString(): String {
        return "${yearCrossDataMap.values.joinToString("\n")}\n${getTotalDesc()}"
    }
}
