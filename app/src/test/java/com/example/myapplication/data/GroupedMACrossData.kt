package com.example.myapplication.data

import com.example.myapplication.utils.Utils

/**
 * Created by wtuadn on 2025/12/15.
 */
data class GroupedMACrossData(
    val groupKey: String,  // 分组键
    val crossDataList: List<MACrossData>, //交叉数据列表
) {
    val totalCount: Int = crossDataList.size
    val victoryRate: Double = if (totalCount == 0) 0.0 else crossDataList.count { it.percentage > 0 }.toDouble() / totalCount
    val totalPercentage: Double = if (totalCount == 0) 0.0 else crossDataList.sumOf { it.percentage }
    val maxPercentage: Double = if (totalCount == 0) 0.0 else crossDataList.maxOf { if (it.percentage > 0) it.percentage else 0.0 }
    val minPercentage: Double = if (totalCount == 0) 0.0 else crossDataList.minOf { if (it.percentage < 0) it.percentage else 0.0 }

    override fun toString(): String {
        return "$groupKey " +
            "总次数: $totalCount " +
            "胜率: ${Utils.getPercentageString(victoryRate)} " +
            "涨幅: ${Utils.getPercentageString(totalPercentage)} " +
            "单次最大涨幅: ${Utils.getPercentageString(maxPercentage)} " +
            "单次最大回撤: ${Utils.getPercentageString(minPercentage)}"
    }
}
