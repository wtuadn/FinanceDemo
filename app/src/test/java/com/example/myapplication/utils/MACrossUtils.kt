package com.example.myapplication.utils

import com.example.myapplication.data.AlignedMAData
import com.example.myapplication.data.GroupedMACrossData
import com.example.myapplication.data.KLineData
import com.example.myapplication.data.MACrossData
import com.example.myapplication.data.MACrossResult
import com.example.myapplication.data.MAData
import kotlin.math.pow
import kotlin.math.round

/**
 * Created by wtuadn on 2025/12/15.
 */
object MACrossUtils {
    enum class MA_TYPE {
        SMA,
        EMA,
    }

    fun calculateMACross(
        kLineData: List<KLineData>,
        shortMA: Int,
        longMA: Int,
        maType: MA_TYPE = MA_TYPE.SMA,
        upCrossDiffRate: Double = 0.000,
        downCrossDiffRate: Double = 0.000,
        volumeDiffRate: Double = 0.000,
    ): MACrossResult {
        val shortMADataList: List<MAData>
        val longMADataList: List<MAData>
        when (maType) {
            MA_TYPE.SMA -> {
                shortMADataList = calculateMAData(kLineData, shortMA)
                longMADataList = calculateMAData(kLineData, longMA)
            }
            MA_TYPE.EMA -> {
                shortMADataList = calculateEMAData(kLineData, shortMA)
                longMADataList = calculateEMAData(kLineData, longMA)
            }
        }
        // 过滤掉 MA 值为 null 的数据，并确保两个列表日期对齐
        val alignedMAData = Utils.calculateAlignedMAData(shortMADataList, longMADataList)

        // 存储配对的交易信号
        val crossDataList = mutableListOf<MACrossData>()
        var crossData: AlignedMAData? = null // 记录交叉data
        var buyData: AlignedMAData? = null // 记录买入data

        for (i in 1 until alignedMAData.size) {
            val today = alignedMAData[i]
            val yesterday = alignedMAData[i - 1] // 昨天的 MA 交叉状态

            val todayMADiffRate = (today.shortMAValue - today.longMAValue) / today.longMAValue
            // 1. 检查金叉 (Golden Cross)
            if (buyData == null) {
                if (yesterday.shortMAValue < yesterday.longMAValue && today.shortMAValue >= today.longMAValue) {
                    crossData = today //记录金叉
                }
                if (crossData != null && today.shortMAValue < today.longMAValue) {
                    crossData = null
                }
            } else {
                if (yesterday.shortMAValue > yesterday.longMAValue && today.shortMAValue <= today.longMAValue) {
                    crossData = today //记录死叉
                }
                if (crossData != null && today.shortMAValue > today.longMAValue) {
                    crossData = null
                }
            }
            val todayCrossDayMADiffRate = if (crossData != null) {
                (today.shortMAValue - crossData.longMAValue) / crossData.longMAValue
            } else {
                null
            }
            if (
                (yesterday.shortMAValue < yesterday.longMAValue && todayMADiffRate >= upCrossDiffRate) //上穿时超过阈值
                || (todayCrossDayMADiffRate != null && todayCrossDayMADiffRate >= upCrossDiffRate) //上穿后某天超过阈值
            ) {
                // 发生金叉
                if (buyData == null) {
                    // 记录 upData (买入信号)
                    buyData = today
                }
            }

            // 2. 检查死叉 (Death Cross)
            else if (
                (yesterday.shortMAValue > yesterday.longMAValue && todayMADiffRate <= downCrossDiffRate) //下穿时超过阈值
                || (todayCrossDayMADiffRate != null && todayCrossDayMADiffRate <= downCrossDiffRate) //下穿后某天超过阈值
            ) {
                // 发生死叉
                if (buyData != null) {
                    // 记录 downData (卖出信号)，并完成配对
                    val exitData = today
                    crossDataList.add(MACrossData(buyData, exitData))
                    buyData = null // 清空入场数据
                }
            }
        }
        val yearMap = crossDataList.groupBy { it.exitData.date.substring(0, 4).toInt() }
        return MACrossResult(
            totalCrossData = GroupedMACrossData("total:", crossDataList),
            yearCrossDataMap = linkedMapOf<Int, GroupedMACrossData>().apply {
                longMADataList.groupBy { it.date.substring(0, 4).toInt() }.forEach {
                    this.getOrPut(it.key) { GroupedMACrossData("${it.key}", yearMap[it.key] ?: emptyList()) }
                }
            },
        )
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

    /**
     * 根据 KLineData 列表计算 N 日 EXPMA (EMA)
     *
     * @param dataList 原始 KLineData 列表（必须按日期升序排列）。
     * @param period   EMA 的计算周期 N (例如：5, 10, 60)。
     * @return 包含日期和对应 EMA 值的 MAData 列表。
     */
    fun calculateEMAData(dataList: List<KLineData>, period: Int): List<MAData> {
        if (period <= 0) {
            throw IllegalArgumentException("EMA周期 N 必须大于 0。")
        }
        if (dataList.isEmpty()) {
            return emptyList()
        }

        val maList = mutableListOf<MAData>()
        val prices = dataList.map { it.closePrice }

        // 平滑系数 α = 2 / (N + 1)
        val alpha = 2.0 / (period + 1)

        // 用于存储上一个 EMA 值
        var previousEma: Double? = null

        for (i in dataList.indices) {
            val currentDate = dataList[i].date
            val currentPrice = dataList[i].closePrice
            val currentPriceStr = dataList[i].closePriceStr
            val currentVolume = dataList[i].volume

            // —————— 第一步：初始化（前 period-1 天无 EMA，设为 null）——————
            if (i < period - 1) {
                // 前 N-1 天：EMA 未有效形成，设为 null（或沿用 SMA 初始化方式，此处按主流设为 null）
                maList.add(MAData(currentDate, currentPrice, currentVolume, null))
                continue
            }

            // —————— 第二步：第 period 天（i == period - 1），用 SMA 初始化首个 EMA ——————
            if (i == period - 1) {
                val windowPrices = prices.subList(0, period) // [0, period)
                val sma = windowPrices.sum() / period.toDouble()
                previousEma = sma
            } else {
                // —————— 第三步：递推计算后续 EMA ——————
                val ema = alpha * currentPrice + (1 - alpha) * previousEma!!
                previousEma = ema
            }

            // 四舍五入，与原始价格小数位一致
            val decimalPlaces = currentPriceStr.substringAfter('.', "").length
            val roundedEma = round(previousEma * 10.0.pow(decimalPlaces.toDouble())) / 10.0.pow(decimalPlaces.toDouble())
            maList.add(MAData(currentDate, currentPrice, currentVolume, roundedEma))
        }
        return maList
    }
}