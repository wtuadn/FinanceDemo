package com.example.myapplication.utils

import com.example.myapplication.data.AlignedMAData
import com.example.myapplication.data.GroupedMACrossData
import com.example.myapplication.data.KLineData
import com.example.myapplication.data.MACrossData
import com.example.myapplication.data.MACrossResult
import com.example.myapplication.data.MAData
import com.example.myapplication.data.MaxDrawDownData
import com.example.myapplication.data.SymbolData
import com.example.myapplication.data.TradeSignal
import com.example.myapplication.data.TradeSignalData
import kotlin.math.pow
import kotlin.math.round

/**
 * Created by wtuadn on 2025/12/15.
 */
object MACrossUtils {

    enum class MAType {
        SMA,
        EMA,
    }

    // --- Private Data Class for Drawdown State ---
    /**
     * 用于跟踪回撤和净值曲线的状态
     */
    private data class DrawdownState(
        var currentBalance: Double = 1.0, // 当前净值余额，初始值为1.0（代表100%本金）
        var peakBalance: Double = 1.0, // 历史最高净值，即历史高水位标记
        var currentPeakDate: String = "", // 当前历史最高净值对应的日期
        var maxDrawDown: Double = 0.0, // 最大回撤率，计算公式为(peakBalance - currentBalance) / peakBalance
        var maxDDPeakDate: String = "", // 最大回撤发生时的峰值日期（波峰）
        var maxDDValleyDate: String = "", // 最大回撤达到最低点的日期（波谷）
        var maxDDPeakValue: Double = 1.0, // 最大回撤发生时的峰值净值
        var maxDDRecoveryDate: String? = null, // 最大回撤的修复日期（净值重新回到峰值的日期），如果未修复则为 null
        var buyBalance: Double? = null, // 买入时的本金净值，用于计算买入后最大损失率
        var maxLossFromBuy: Double = 0.0, // 买入后相对于买入本金的最大损失率
        var maxLossFromBuyDate: String = "", // 买入后最大损失发生的日期
        var maxLossFromBuyStartDate: String = "", // 买入后最大损失开始的日期（首次达到最大损失的日期）
        var maxLossFromBuyRecoveryDate: String? = null, // 买入后最大损失的修复日期（净值回到买入本金水平的日期），如果未修复则为 null
    )
    // -------------------------------------------------

    /**
     * 获取当前交易信号
     */
    fun getTradeSignal(symbol: SymbolData): TradeSignalData? {
        val history = Utils.getSinaKLineData(symbol.copy(scale = 240), datalen = (symbol.longMA * 3).coerceAtLeast(200))
        Thread.sleep(Utils.httpDelay)
        val lastest = Utils.getSinaKLineData(symbol.copy(scale = 5), datalen = 1)
        val kLineData = if (history.isNotEmpty() && lastest.isNotEmpty()) {
            if (symbol.d == 1 && Utils.isSameDay(history.last().date, lastest.last().date)) {
                history.dropLast(1) + lastest // 用最新数据替换掉最末尾的
            } else if (symbol.d == 5 && Utils.isSameWeek(history.last().date, lastest.last().date)) {
                history.dropLast(1) + lastest // 用最新数据替换掉最末尾的
            } else {
                history + lastest //不是同一天，把最新数据合并过来
            }
        } else {
            emptyList()
        }
        if (kLineData.isEmpty()) {
            return TradeSignalData(TradeSignal.HOLD, "${Utils.timestampToDate(System.currentTimeMillis() / 1000)}-kLineData.isEmpty")
        }
        return calculateMACross(symbol, kLineData).latestTradeSignalData
    }

    /**
     * 计算均线交叉策略，包含最大回撤和修复时间计算
     */
    fun calculateMACross(
        symbol: SymbolData,
        kLineData: List<KLineData>,
    ): MACrossResult {

        // 1. 计算长短均线
        val (shortMADataList, longMADataList) = calculateMADataLists(kLineData, symbol)

        // 2. 对齐数据（确保日期匹配，过滤掉无效的 null 值）
        val alignedMAData = Utils.calculateAlignedMAData(shortMADataList, longMADataList)

        // 3. 策略与回撤计算变量初始化
        val crossDataList = mutableListOf<MACrossData>()
        var crossData: AlignedMAData? = null // 记录均线交叉点（用于判断阈值）
        var buyData: AlignedMAData? = null   // 当前持仓的买入点（不为 null 表示当前持仓）
        var latestTradeSignalData: TradeSignalData? = null

        var drawdownState = DrawdownState(
            currentPeakDate = if (alignedMAData.isNotEmpty()) alignedMAData[0].date else ""
        )

        // 4. 遍历数据 (从索引 1 开始，因为需要对比 yesterday)
        for (i in 1 until alignedMAData.size) {
            val today = alignedMAData[i]
            val yesterday = alignedMAData[i - 1]

            // ==========================================
            // Part A: 资金曲线与回撤计算 (每日盯市)
            // ==========================================
            // Note: handleDrawdownUpdate returns a new state object, which is cleaner
            drawdownState = handleDrawdownUpdate(drawdownState, today, yesterday, buyData)

            // ==========================================
            // Part B: 交易信号逻辑 (均线交叉)
            // ==========================================
            val (newCrossData, newBuyData) = handleTradingSignal(
                today = today,
                yesterday = yesterday,
                crossData = crossData,
                buyData = buyData,
                crossDataList = crossDataList,
                upCrossDiffRate = symbol.upCrossDiffRate,
                downCrossDiffRate = symbol.downCrossDiffRate
            )
            if (buyData == null && newBuyData != null) {
                latestTradeSignalData = TradeSignalData(TradeSignal.BUY, today.date)
            } else if (buyData != null && newBuyData == null) {
                latestTradeSignalData = TradeSignalData(TradeSignal.SELL, today.date)
            }
            crossData = newCrossData
            buyData = newBuyData
        }

        // 5. 结果组装
        val yearMap = crossDataList.groupBy { it.exitData.date.substring(0, 4).toInt() }
        return MACrossResult(
            totalCrossData = GroupedMACrossData("total:", crossDataList),
            yearCrossDataMap = linkedMapOf<Int, GroupedMACrossData>().apply {
                // 确保年份按照 longMADataList 存在的年份填充，即使该年没有交易
                longMADataList.map { it.date.substring(0, 4).toInt() }.distinct().forEach { year ->
                    this[year] = GroupedMACrossData("$year", yearMap[year] ?: emptyList())
                }
            },
            // 在 MACrossResult 的计算部分，需要更新 MaxDrawDownData
            maxDrawDownData = MaxDrawDownData(
                maxDrawDownRate = drawdownState.maxDrawDown,
                peakDate = drawdownState.maxDDPeakDate,
                valleyDate = drawdownState.maxDDValleyDate,
                recoveryDate = drawdownState.maxDDRecoveryDate,
                peakValue = drawdownState.maxDDPeakValue,
                maxLossFromBuyRate = drawdownState.maxLossFromBuy,
                maxLossFromBuyDate = drawdownState.maxLossFromBuyDate,
                maxLossFromBuyStartDate = drawdownState.maxLossFromBuyStartDate,
                maxLossFromBuyRecoveryDate = drawdownState.maxLossFromBuyRecoveryDate
            ),
            latestTradeSignalData = latestTradeSignalData,
        )
    }

    // --- Private Helper Functions for MACross Calculation ---

    /**
     * Helper to compute short and long MA data lists based on MA_TYPE.
     */
    private fun calculateMADataLists(
        kLineData: List<KLineData>,
        symbol: SymbolData,
    ): Pair<List<MAData>, List<MAData>> {
        return when (symbol.maType) {
            MAType.SMA -> {
                Pair(
                    calculateMAData(kLineData, symbol.shortMA),
                    calculateMAData(kLineData, symbol.longMA)
                )
            }

            MAType.EMA -> {
                Pair(
                    calculateEMAData(kLineData, symbol.shortMA),
                    calculateEMAData(kLineData, symbol.longMA)
                )
            }
        }
    }

    /**
     * Part A: 资金曲线与回撤计算 (每日盯市)
     * Updates the current balance and the drawdown state based on today's data.
     * @return The updated DrawdownState.
     */
    private fun handleDrawdownUpdate(
        state: DrawdownState,
        today: AlignedMAData,
        yesterday: AlignedMAData,
        buyData: AlignedMAData?, // Need to know if we hold a position
    ): DrawdownState {
        val newState = state.copy() // Work on a copy for cleaner state updates

        // 1. 如果昨天结束时持有仓位，今天的涨跌幅计入净值
        val changeRatio = today.closePrice / yesterday.closePrice
        if (buyData != null) {
            newState.currentBalance *= changeRatio
        }

        // 2. 更新买入后最大损失率 - 只有在持仓期间才计算
        if (buyData != null) {
            // 如果是第一次买入，记录初始本金
            if (newState.buyBalance == null) {
                newState.buyBalance = newState.currentBalance / changeRatio
            }

            // 计算从买入时的本金开始的损失率
            val lossFromBuy = (newState.currentBalance - newState.buyBalance!!) / newState.buyBalance!!

            // 更新买入后最大损失率
            if (lossFromBuy < newState.maxLossFromBuy) {
                newState.maxLossFromBuy = lossFromBuy
                newState.maxLossFromBuyDate = today.date
                newState.maxLossFromBuyStartDate = buyData.date
                // 既然创了新低，之前的修复记录失效
                newState.maxLossFromBuyRecoveryDate = null
            } else if (newState.maxLossFromBuyRecoveryDate == null && newState.maxLossFromBuy < 0) {
                // 检查是否从最大损失中恢复（净值回到买入时的本金水平）
                if (newState.currentBalance >= newState.buyBalance!!) {
                    newState.maxLossFromBuyRecoveryDate = today.date
                }
            }
        } else {
            newState.buyBalance = null
        }

        // 3. 更新历史最高净值 (High Water Mark)
        if (newState.currentBalance > newState.peakBalance) {
            newState.peakBalance = newState.currentBalance
            newState.currentPeakDate = today.date
        }

        // 4. 计算当前回撤
        val currentDrawDown = if (newState.peakBalance > 0) (newState.currentBalance - newState.peakBalance) / newState.peakBalance else 0.0

        // 5. 核心逻辑：更新最大回撤 或 检查修复
        if (currentDrawDown < newState.maxDrawDown) {
            // 发现更大的回撤：更新记录
            newState.maxDrawDown = currentDrawDown
            newState.maxDDPeakDate = newState.currentPeakDate
            newState.maxDDValleyDate = today.date
            newState.maxDDPeakValue = newState.peakBalance

            // 既然创了新低，说明之前的"修复"已经无效
            newState.maxDDRecoveryDate = null
        } else if (newState.maxDDRecoveryDate == null && newState.maxDrawDown < 0) {
            // 还没修复，检查今天是否爬出坑了
            if (newState.currentBalance >= newState.maxDDPeakValue) {
                newState.maxDDRecoveryDate = today.date
            }
        }

        return newState
    }

    /**
     * Part B: 交易信号逻辑 (均线交叉)
     * Processes trading signals and executes trades, updating the cross/buy data and the trade list.
     * @return A Pair containing the updated crossData (potential cross point) and buyData (current position).
     */
    private fun handleTradingSignal(
        today: AlignedMAData,
        yesterday: AlignedMAData,
        crossData: AlignedMAData?,
        buyData: AlignedMAData?,
        crossDataList: MutableList<MACrossData>, // Mutated by this function
        upCrossDiffRate: Double,
        downCrossDiffRate: Double,
    ): Pair<AlignedMAData?, AlignedMAData?> {
        var newCrossData = crossData
        var newBuyData = buyData

        val todayMADiffRate = if (today.longMAValue != 0.0)
            (today.shortMAValue - today.longMAValue) / today.longMAValue
        else 0.0

        // 1. 检查潜在的金叉/死叉状态变化
        if (newBuyData == null) {
            // 空仓状态：寻找金叉
            if (yesterday.shortMAValue < yesterday.longMAValue && today.shortMAValue >= today.longMAValue) {
                newCrossData = today // 记录金叉发生点
            }
            // 如果金叉后又跌回去了，重置
            if (newCrossData != null && today.shortMAValue < today.longMAValue) {
                newCrossData = null
            }
        } else {
            // 持仓状态：寻找死叉
            if (yesterday.shortMAValue > yesterday.longMAValue && today.shortMAValue <= today.longMAValue) {
                newCrossData = today // 记录死叉发生点
            }
            // 如果死叉后又涨回去了，重置
            if (newCrossData != null && today.shortMAValue > today.longMAValue) {
                newCrossData = null
            }
        }

        // 计算距离交叉点的偏离度
        val todayCrossDayMADiffRate = if (newCrossData != null && newCrossData.longMAValue != 0.0) {
            (today.shortMAValue - newCrossData.longMAValue) / newCrossData.longMAValue
        } else {
            null
        }

        // 2. 执行买入逻辑 (Check Buy Signal)
        if (newBuyData == null) {
            // 条件：(刚上穿且超过阈值) OR (上穿后某天超过阈值)
            val isJustCrossedUp = yesterday.shortMAValue < yesterday.longMAValue && todayMADiffRate >= upCrossDiffRate
            val isAfterCrossedUp = todayCrossDayMADiffRate != null && todayCrossDayMADiffRate >= upCrossDiffRate

            if (isJustCrossedUp || isAfterCrossedUp) {
                newBuyData = today // 执行买入
            }
        }
        // 3. 执行卖出逻辑 (Check Sell Signal)
        else {
            // 条件：(刚下穿且超过阈值) OR (下穿后某天超过阈值)
            val isJustCrossedDown = yesterday.shortMAValue > yesterday.longMAValue && todayMADiffRate <= downCrossDiffRate
            val isAfterCrossedDown = todayCrossDayMADiffRate != null && todayCrossDayMADiffRate <= downCrossDiffRate

            if (isJustCrossedDown || isAfterCrossedDown) {
                val exitData = today
                crossDataList.add(MACrossData(newBuyData, exitData)) // 记录一次完整交易
                newBuyData = null // 清空持仓，变为空仓
            }
        }

        return Pair(newCrossData, newBuyData)
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
                maList.add(MAData(currentDate, currentPrice, currentVolume, null))
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
                // 前 N-1 天：EMA 未有效形成, MA值设置为 null
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
            val roundedEma = round(previousEma!! * 10.0.pow(decimalPlaces.toDouble())) / 10.0.pow(decimalPlaces.toDouble())
            maList.add(MAData(currentDate, currentPrice, currentVolume, roundedEma))
        }
        return maList
    }
}