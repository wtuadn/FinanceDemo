package com.example.myapplication.data

import com.example.myapplication.utils.Utils

/**
 * Created by wtuadn on 2025/12/15.
 */
data class MACrossResult(
    val totalCrossData: GroupedMACrossData, //总结果
    val yearCrossDataMap: Map<Int, GroupedMACrossData>, // 按年结果
    val maxDrawDownData: MaxDrawDownData,
    val latestTradeSignalData: TradeSignalData?,
) {

    fun getTotalDesc(): String {
        val countlyPercentage = totalCrossData.countlyPercentage
        val dailyPercentage = totalCrossData.totalPercentage / totalCrossData.crossDataList.sumOf { it.dayCount }
        val yearlyPercentage = totalCrossData.totalPercentage / yearCrossDataMap.size
        val totalStr = totalCrossData.toString()
        val insert = totalCrossData.groupKey.length + 1
        return "${totalStr.take(insert)}平均次涨幅：${Utils.getPercentageString(countlyPercentage)} 平均天涨幅：${Utils.getPercentageString(dailyPercentage)} 平均年涨幅：${Utils.getPercentageString(yearlyPercentage)} ${totalStr.substring(insert)} \n$maxDrawDownData"
    }

    override fun toString(): String {
        return "${yearCrossDataMap.values.joinToString("\n")}\n${getTotalDesc()}"
    }
}
