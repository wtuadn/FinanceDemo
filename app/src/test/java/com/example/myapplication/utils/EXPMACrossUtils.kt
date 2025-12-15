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
object EXPMACrossUtils {
    fun calculateEXPMACross(
        kLineData: List<KLineData>,
        shortMA:Int,
        longMA: Int,
        upCrossDiffRate: Double = 0.000,
        downCrossDiffRate: Double = 0.000,
    ): MACrossResult {
        val shortMADataList = calculateEMAData(kLineData, shortMA)
        val longMADataList = calculateEMAData(kLineData, longMA)
        // 过滤掉 MA 值为 null 的数据，并确保两个列表日期对齐
        val alignedMAData = Utils.calculateAlignedMAData(shortMADataList, longMADataList)

        // 存储配对的交易信号
        val crossDataList = mutableListOf<MACrossData>()
        var enterData: AlignedMAData? = null // 记录金叉时的 upData

        for (i in 2 until alignedMAData.size) {
            val today = alignedMAData[i]
            val yesterday = alignedMAData[i - 1] // 昨天的 MA 交叉状态
            val yesterday2 = alignedMAData[i - 2] // 前天的 MA 交叉状态

            val todayMADiffRate = (today.shortMAValue - today.longMAValue) / today.longMAValue
            // 1. 检查金叉 (Golden Cross)
            if (
                (yesterday.shortMAValue <= yesterday.longMAValue && todayMADiffRate > upCrossDiffRate) //上穿时超过阈值
                || (yesterday.shortMAValue > yesterday.longMAValue && todayMADiffRate > upCrossDiffRate) //上穿后某天超过阈值
            // || (today.shortMAValue > today.longMAValue && yesterday.shortMAValue > yesterday.longMAValue && yesterday2.shortMAValue > yesterday2.longMAValue) //上穿后连续3天稳住
            ) {
                // 发生金叉
                if (enterData == null) {
                    // 记录 upData (买入信号)
                    enterData = today
                }
            }

            // 2. 检查死叉 (Death Cross)
            else if (
                (yesterday.shortMAValue >= yesterday.longMAValue && todayMADiffRate < downCrossDiffRate) //下穿时超过阈值
                || (yesterday.shortMAValue < yesterday.longMAValue && todayMADiffRate < downCrossDiffRate) //下穿后某天超过阈值
                // || (today.shortMAValue < today.longMAValue && yesterday.shortMAValue < yesterday.longMAValue && yesterday2.shortMAValue < yesterday2.longMAValue) //下穿后连续3天稳住
            ) {
                // 发生死叉
                if (enterData != null) {
                    // 记录 downData (卖出信号)，并完成配对
                    val exitData = today
                    crossDataList.add(MACrossData(enterData, exitData))
                    enterData = null // 清空入场数据
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
            }else{
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