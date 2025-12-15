package com.example.myapplication.utils

import com.example.myapplication.data.AlignedMAData
import com.example.myapplication.data.KLineData
import com.example.myapplication.data.MAData
import kotlin.math.pow
import kotlin.math.round

/**
 * Created by wtuadn on 2025/12/15.
 */
object MACrossUtils {
    fun calculateMACross(
        kLineData: List<KLineData>,
        shortMA:Int,
        longMA: Int,
        upCrossMADiffRate: Double = 0.000,
        downCrossMADiffRate: Double = 0.000,
        logPerCross: Boolean = true,
    ): Triple<Double, Double, String> {
        val shortMADataList = calculateMAData(kLineData, shortMA)
        val longMADataList = calculateMAData(kLineData, longMA)
        val sb = StringBuilder()

        // 过滤掉 MA 值为 null 的数据，并确保两个列表日期对齐
        val alignedMAData = Utils.calculateAlignedMAData(shortMADataList, longMADataList)
        sb.append("--- 有效数据时间段为：${longMADataList.firstOrNull()?.date} - ${longMADataList.lastOrNull()?.date} ---\n")
        // 存储配对的交易信号
        val tradePairs = mutableListOf<Pair<AlignedMAData, AlignedMAData>>()
        var entryData: AlignedMAData? = null // 记录金叉时的 upData

        for (i in 2 until alignedMAData.size) {
            val today = alignedMAData[i]
            val yesterday = alignedMAData[i - 1] // 昨天的 MA 交叉状态
            val yesterday2 = alignedMAData[i - 2] // 前天的 MA 交叉状态

            val todayMADiffRate = (today.shortMAValue - today.longMAValue) / today.longMAValue
            // 1. 检查金叉 (Golden Cross)
            if (
                (yesterday.shortMAValue <= yesterday.longMAValue && todayMADiffRate > upCrossMADiffRate) //上穿时超过阈值
                || (yesterday.shortMAValue > yesterday.longMAValue && todayMADiffRate > upCrossMADiffRate) //上穿后某天超过阈值
            // || (today.shortMAValue > today.longMAValue && yesterday.shortMAValue > yesterday.longMAValue && yesterday2.shortMAValue > yesterday2.longMAValue) //上穿后连续3天稳住
            ) {
                // 发生金叉
                if (entryData == null) {
                    // 记录 upData (买入信号)
                    entryData = today
                }
            }

            // 2. 检查死叉 (Death Cross)
            else if (
                (yesterday.shortMAValue >= yesterday.longMAValue && todayMADiffRate < downCrossMADiffRate) //下穿时超过阈值
                || (yesterday.shortMAValue < yesterday.longMAValue && todayMADiffRate < downCrossMADiffRate) //下穿后某天超过阈值
                || (today.shortMAValue < today.longMAValue && yesterday.shortMAValue < yesterday.longMAValue && yesterday2.shortMAValue < yesterday2.longMAValue) //下穿后连续3天稳住
            ) {
                // 发生死叉
                if (entryData != null) {
                    // 记录 downData (卖出信号)，并完成配对
                    val exitData = today
                    tradePairs.add(Pair(entryData, exitData))
                    entryData = null // 清空入场数据
                }
            }
        }

        // ------------------ 计算和打印结果 ------------------

        // 用于存储所有计算出的百分比，便于后续按年累计
        val yearlyPercentageMap = mutableMapOf<Int, MutableList<Pair<Double, String>>>()
        longMADataList.groupBy { it.date.substring(0, 4).toInt() }.forEach {
            yearlyPercentageMap.getOrPut(it.key) { mutableListOf() }
        }

        var minPercentage = 0.0
        var minPercentageDisplay = ""
        var maxPercentage = 0.0
        var maxPercentageDisplay = ""
        for (pair in tradePairs) {
            val upData = pair.first
            val downData = pair.second

            val upValue = upData.closePrice
            val downValue = downData.closePrice

            // 计算百分比: (downData.value - upData.value) / upData.value * 100%
            val percentage = (downValue - upValue) / upValue
            val percentageDisplay = "%.2f%%".format(percentage * 100)

            // 打印结果 (upData.date - downData.date: downData.value - upData.value 的百分比)
            val display = "${upData.date} - ${downData.date} : $percentageDisplay"
            // 更新最大最小回撤
            if (percentage > maxPercentage) {
                maxPercentage = percentage
                maxPercentageDisplay = display
            }
            if (percentage < minPercentage) {
                minPercentage = percentage
                minPercentageDisplay = display
            }
            if (logPerCross) {
                sb.append("$display\n")
            }

            // 累计到年份映射中
            // 以金叉 (upData) 发生的年份为准进行累计
            val year = downData.date.substring(0, 4).toInt()
            yearlyPercentageMap.getOrPut(year) { mutableListOf() }.add(percentage to display)
        }

        // ------------------ 按年打印累计百分比 ------------------

        for (year in yearlyPercentageMap.keys.sorted()) {
            var minPercentage = 0.0
            var minPercentageDisplay = ""
            var maxPercentage = 0.0
            var maxPercentageDisplay = ""

            val percentages = yearlyPercentageMap[year]!!
            val cumulativePercentage = percentages?.sumOf { it.first } ?: 0.0

            percentages.forEach {
                val percentage = it.first
                val display = it.second
                // 更新最大最小回撤
                if (percentage > maxPercentage) {
                    maxPercentage = percentage
                    maxPercentageDisplay = display
                }
                if (percentage < minPercentage) {
                    minPercentage = percentage
                    minPercentageDisplay = display
                }
            }
            val cumulativeDisplay = "%.2f%%".format(cumulativePercentage * 100)
            val victoryRate = Utils.getVictoryRateString(percentages.count { it.first >= 0 }, percentages.count { it.first < 0 })
            sb.append("$year: $cumulativeDisplay $victoryRate 最大涨幅：$maxPercentageDisplay 最大回撤：$minPercentageDisplay\n")
        }

        val allPercentageDatas = yearlyPercentageMap.flatMap { it.value }
        val victoryRate = Utils.getVictoryRateString(allPercentageDatas.count { it.first >= 0 }, allPercentageDatas.count { it.first < 0 })
        val yearlyPercentage =
            if (yearlyPercentageMap.isEmpty()) 0.0 else yearlyPercentageMap.values.flatMap { it }.sumOf { it.first } / yearlyPercentageMap.size
        sb.append(
            "--- 平均年收益${Utils.getPercentageString(yearlyPercentage)} $victoryRate 最大涨幅：$maxPercentageDisplay 最大回撤：$minPercentageDisplay ---"
        )
        return Triple(yearlyPercentage, minPercentage, sb.toString())
    }

    /**
     * 根据 ChartData 列表计算 N 日简单移动平均线 (SMA)。
     *
     * @param dataList 原始 ChartData 列表（假设已按日期升序排列）。
     * @param period 均线的计算周期 N (例如：5, 10, 60)。
     * @return 包含日期和对应 MA 值的 MovingAverageData 列表。
     */
    fun calculateMAData(dataList: List<KLineData>, period: Int): List<MAData> {
        if (period <= 0) {
            throw IllegalArgumentException("均线周期 N 必须大于 0。")
        }

        // 提取所有收盘价，方便后续窗口计算
        val prices = dataList.map { it.closePrice }
        val maList = mutableListOf<MAData>()

        // 遍历所有数据点
        for (i in dataList.indices) {
            val currentDate = dataList[i].date
            val currentPrice = dataList[i].closePrice
            val currentPriceStr = dataList[i].closePriceStr
            val currentVolume = dataList[i].volume

            // MA 计算的起始索引：必须向前追溯 (period - 1) 天
            // 例如，计算 MA5，需要从 i 向前到 i - 4，一共 5 个点。
            val startIndex = i - period + 1

            // 1. 处理前 N-1 天数据：MA值设置为 null
            if (startIndex < 0) {
                maList.add(MAData(currentDate, currentPrice, 0, null))
                continue
            }

            // 2. 提取计算窗口内的 N 个收盘价
            // Kotlin 的 subList 索引是 [fromIndex, toIndex)，所以 toIndex = i + 1
            val windowPrices = prices.subList(startIndex, i + 1)

            // 3. 计算总和
            val sum = windowPrices.sum()

            // 4. 计算平均值
            val rawMa = sum / period

            // 5. 根据当前价格的小数位数四舍五入
            val decimalPlaces = currentPriceStr.substringAfter('.', "").length
            val roundedMa = round(rawMa * 10.0.pow(decimalPlaces.toDouble())) / 10.0.pow(decimalPlaces.toDouble())

            // 6. 添加到结果列表
            maList.add(MAData(currentDate, currentPrice, currentVolume, roundedMa))
        }

        return maList
    }
}