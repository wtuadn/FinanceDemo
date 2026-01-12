package com.example.myapplication.utils

import android.os.Build
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
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

/**
 * Created by wtuadn on 2025/12/15.
 */
object MACrossUtils {

    enum class MAType {
        SMA,
        EMA,
        RSI,
        OBV,
        SKDJ,
        MACD,
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
        var currentBuyBalance: Double? = null, // 当前交易的买入时本金净值
        var currentBuyDate: String = "", // 当前交易的买入日期
        var currentLossFromBuyState: LossFromBuyState = LossFromBuyState(), // 当前最大损失记录
        var maxLossFromBuyState: LossFromBuyState? = null, // 历史最大损失记录
    )

    private data class LossFromBuyState(
        var lossFromBuy: Double = 0.0, // 所有交易中，持仓期间相对于买入本金的最大损失率
        var lossFromBuyDate: String = "", // 发生最大本金损失的日期（波谷）
        var lossFromBuyStartDate: String = "", // 发生最大本金损失的交易的开始日期
        var lossFromBuyStartBalance: Double = 0.0, // 发生最大本金损失的交易的起始资金
        var lossFromBuyRecoveryDate: String? = null, // 最大本金损失的修复日期（净值回到买入本金水平的日期），如果未修复则为 null
    )
    // -------------------------------------------------

    /**
     * 获取当前交易信号
     */
    fun getTradeSignal(symbol: SymbolData, backtestLog: String? = null): List<TradeSignalData> {
        val maxArg = symbol.shortMA.coerceAtLeast(symbol.longMA).coerceAtLeast(symbol.extN)
        val atLeastLen = maxArg * if (symbol.maType == MAType.OBV) 100 else if (symbol.maType == MAType.RSI) 5 else 3
        var datalen = atLeastLen.coerceAtLeast(100)
        if (!backtestLog.isNullOrBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                // 正则表达式匹配 YYYY-MM-DD—— 格式的日期
                val regex = "(\\d{4}-\\d{2}-\\d{2})——".toRegex()
                val matches = regex.findAll(backtestLog).map { it.groupValues[1] }.toList()

                if (matches.isNotEmpty()) {
                    // 找到倒数第二个起始日期
                    val secondToLastDateStr = matches.takeLast(2).first()
                    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                    val startDate = LocalDate.parse(secondToLastDateStr, formatter)
                    val today = LocalDate.now()
                    // 计算日期差并加上最小周期
                    val daysBetween = ChronoUnit.DAYS.between(startDate, today)
                    datalen = atLeastLen + daysBetween.toInt()
                }
            }
        }

        val history = Utils.getSinaKLineData(symbol = symbol.copy(scale = 240), datalen = datalen)
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
            return listOf(TradeSignalData(TradeSignal.HOLD, "${Utils.timestampToDate(System.currentTimeMillis() / 1000)}-kLineData.isEmpty"))
        }
        if (symbol.maType == MAType.OBV && kLineData.find { it.volume <= 0 } != null) {
            return listOf(TradeSignalData(TradeSignal.HOLD, "${Utils.timestampToDate(System.currentTimeMillis() / 1000)}-kLineData.volume<=0"))
        }
        return calculateMACross(symbol, kLineData).tradeSignalDataList
    }

    private val cachedAlignedMADataMap = mutableMapOf<String, List<AlignedMAData>>()

    /**
     * 计算均线交叉策略，包含最大回撤和修复时间计算
     */
    fun calculateMACross(
        symbol: SymbolData,
        kLineData: List<KLineData>,
    ): MACrossResult {
        val cacheKey = "${symbol.code}-${symbol.d}-${symbol.maType}-${symbol.shortMA}-${symbol.longMA}-${symbol.extN}"
        val alignedMAData: List<AlignedMAData> = cachedAlignedMADataMap[cacheKey] ?: run {
            when (symbol.maType) {
                MAType.SKDJ -> {
                    calculateSKDJData(kLineData, symbol.extN, symbol.shortMA, symbol.longMA)
                }
                MAType.MACD -> {
                    calculateMACDData(kLineData, symbol.longMA, symbol.shortMA, symbol.extN)
                }
                else -> {
                    // 1. 计算长短均线
                    val (shortMADataList, longMADataList) = calculateMADataLists(kLineData, symbol)
                    // 2. 对齐数据（确保日期匹配，过滤掉无效的 null 值）
                    Utils.calculateAlignedMAData(shortMADataList, longMADataList)
                }
            }
        }
        cachedAlignedMADataMap[cacheKey] = alignedMAData

        // 3. 策略与回撤计算变量初始化
        val crossDataList = mutableListOf<MACrossData>()
        var crossData: AlignedMAData? = null // 记录均线交叉点（用于判断阈值）
        var buyData: AlignedMAData? = null   // 当前持仓的买入点（不为 null 表示当前持仓）
        var latestTradeSignalData = mutableListOf<TradeSignalData>()

        var drawdownState = DrawdownState(
            currentPeakDate = if (alignedMAData.isNotEmpty()) alignedMAData[0].kLineData.date else ""
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
                symbol = symbol,
                today = today,
                yesterday = yesterday,
                crossData = crossData,
                buyData = buyData,
                crossDataList = crossDataList,
            )
            if (buyData == null && newBuyData != null) {
                latestTradeSignalData += TradeSignalData(TradeSignal.BUY, today.kLineData.date)
            } else if (buyData != null && newBuyData == null) {
                latestTradeSignalData += TradeSignalData(TradeSignal.SELL, today.kLineData.date)
            }
            crossData = newCrossData
            buyData = newBuyData
        }

        // 5. 结果组装
        val yearMap = crossDataList.groupBy { it.exitData.kLineData.date.substring(0, 4).toInt() }
        return MACrossResult(
            alignedMAData = alignedMAData,
            totalCrossData = GroupedMACrossData("total:", crossDataList),
            yearCrossDataMap = linkedMapOf<Int, GroupedMACrossData>().apply {
                // 确保年份按照 longMADataList 存在的年份填充，即使该年没有交易
                alignedMAData.map { it.kLineData.date.substring(0, 4).toInt() }.distinct().forEach { year ->
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
                maxLossFromBuyRate = drawdownState.maxLossFromBuyState?.lossFromBuy ?: 0.0,
                maxLossFromBuyDate = drawdownState.maxLossFromBuyState?.lossFromBuyDate ?: "",
                maxLossFromBuyStartDate = drawdownState.maxLossFromBuyState?.lossFromBuyStartDate ?: "",
                maxLossFromBuyRecoveryDate = drawdownState.maxLossFromBuyState?.lossFromBuyRecoveryDate
            ),
            tradeSignalDataList = latestTradeSignalData,
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

            MAType.RSI -> {
                val rsi = calculateRSI(kLineData, symbol.shortMA)
                Pair(rsi, rsi)
            }

            MAType.OBV -> {
                Pair(
                    calculateOBVMAData(kLineData, symbol.shortMA),
                    calculateOBVMAData(kLineData, symbol.longMA)
                )
            }

            else -> throw IllegalArgumentException("Invalid MA type: ${symbol.maType}")
        }
    }

    /**
     * Part A: 资金曲线与回撤计算 (每日盯市)
     *
     * 该方法在每个交易日被调用，负责更新资金曲线，并基于此计算和跟踪两个核心的风险指标：
     *
     * 1.  **全局最大回撤 (Max Drawdown)**:
     *     - **定义**: 衡量策略从其历史净值最高点回落的最大百分比。
     *     - **目的**: 评估策略在整个回测期间（无论是否持仓）所面临的最坏情况下的资金缩水风险。
     *     - **计算**: 每日更新净值，并与历史最高净值比较，若当前回撤超过历史记录，则更新最大回撤。
     *
     * 2.  **最大持仓亏损 (Max Loss From Buy)**:
     *     - **定义**: 指策略在某次买入后，净值跌破该次买入时的水平，直到净值完全恢复到该水平之前，所经历的最大资金损失百分比。这个亏损周期可能跨越多次买卖操作。
     *     - **目的**: 评估策略在进入一个新的“盈利高点”后可能面临的最坏的连续亏损情况。
     *     - **计算**:
     *       - 当一次新的买入发生时，如果当时的资金净值创下了新高（或高于上一个亏损周期的起点），则这个点的净值被视为新的“高水位线”。
     *       - 在此之后，每日计算当前净值相对于这个“高水位线”的亏损。
     *       - 如果该亏损超过了历史上记录的 `maxLossFromBuy`，则更新最大持仓亏损。
     *       - 修复时间的计算：只有当资金净值从“高水位线”以下 **向上穿越** 回“高水位线”之上时，才记录修复日期。这避免了在从未跌破时就错误地记录“修复”。
     *
     * @param state 当前的回撤状态对象。
     * @param today 今日的对齐均线数据。
     * @param yesterday 昨日的对齐均线数据。
     * @param buyData 若昨日持仓，则为对应的买入点数据；否则为 null。
     * @return 返回更新后的回撤状态对象 (DrawdownState)。
     */
    private fun handleDrawdownUpdate(
        state: DrawdownState,
        today: AlignedMAData,
        yesterday: AlignedMAData,
        buyData: AlignedMAData?, // 若昨日持仓，则不为 null
    ): DrawdownState {
        val newState = state.copy()

        // --- 步骤 1: 更新当前净值 (Equity Curve) ---
        val balanceBeforeToday = state.currentBalance
        if (buyData != null) {
            // 持仓中，净值根据当日价格变动
            val priceChangeRatio = today.kLineData.closePrice / yesterday.kLineData.closePrice
            newState.currentBalance *= priceChangeRatio
        }
        val balanceAfterToday = newState.currentBalance

        // --- 步骤 2: 计算并更新全局最大回撤 (Max Drawdown) ---
        // 跟踪净值曲线的历史高点
        if (balanceAfterToday > newState.peakBalance) {
            newState.peakBalance = balanceAfterToday
            newState.currentPeakDate = today.kLineData.date
        }
        // 计算从历史高点到当前点的回撤
        val currentDrawdown = (balanceAfterToday - newState.peakBalance) / newState.peakBalance
        if (currentDrawdown < newState.maxDrawDown) {
            // 发现新的最大回撤
            newState.maxDrawDown = currentDrawdown
            newState.maxDDPeakDate = newState.currentPeakDate
            newState.maxDDValleyDate = today.kLineData.date
            newState.maxDDPeakValue = newState.peakBalance
            newState.maxDDRecoveryDate = null // 创了新低，重置修复日期
        } else if (newState.maxDDRecoveryDate == null && newState.maxDrawDown < 0) {
            // 如果处于回撤中且尚未修复，检查净值是否已恢复到前高
            if (balanceAfterToday >= newState.maxDDPeakValue) {
                newState.maxDDRecoveryDate = today.kLineData.date
            }
        }

        // --- 步骤 3: (新逻辑) 计算并更新所有交易中的最大持仓亏损 ---

        // A. 识别新交易的开始
        val justBought = buyData != null && state.currentBuyBalance == null
        if (justBought) {
            newState.currentBuyDate = yesterday.kLineData.date
            newState.currentBuyBalance = balanceBeforeToday
        } else if (buyData == null) {
            newState.currentBuyDate = ""
            newState.currentBuyBalance = null // 空仓时清除
        }

        // C. 一旦“高水位线”被设定，并且我们还未从中恢复，就持续追踪亏损
        //    这个逻辑无论当前是否持仓都应执行，以正确计算修复时间
        if (newState.currentLossFromBuyState.lossFromBuyStartBalance > 0) {
            // 1. 计算从“高水位线”到当前净值的亏损率
            val lossFromHighWaterMark =
                (balanceAfterToday - newState.currentLossFromBuyState.lossFromBuyStartBalance) / newState.currentLossFromBuyState.lossFromBuyStartBalance

            // 2. 如果当前亏损低于历史最大亏损，则更新记录
            if (lossFromHighWaterMark < newState.currentLossFromBuyState.lossFromBuy) {
                newState.currentLossFromBuyState.lossFromBuy = lossFromHighWaterMark
                newState.currentLossFromBuyState.lossFromBuyDate = today.kLineData.date // 更新亏损谷底日期
                newState.currentLossFromBuyState.lossFromBuyRecoveryDate = null // 重置修复日期
                if (newState.maxLossFromBuyState == null || lossFromHighWaterMark < newState.maxLossFromBuyState!!.lossFromBuy) {
                    newState.maxLossFromBuyState = newState.currentLossFromBuyState
                }
            }

            // 3. 检查净值是否已从亏损中恢复到“高水位线”
            // 恢复的条件是：当天净值超过高水位，且前一天净值低于高水位，代表一次“向上穿越”
            if (balanceAfterToday >= newState.currentLossFromBuyState.lossFromBuyStartBalance && balanceBeforeToday < newState.currentLossFromBuyState.lossFromBuyStartBalance) {
                newState.currentLossFromBuyState.lossFromBuyRecoveryDate = today.kLineData.date
            }
        }

        // B. 如果开始一笔新交易，并且当前净值已高于上个亏损周期的起点（或从未有过亏损），
        //    则将此交易的起点设置为新的“高水位线”，用于追踪未来的潜在亏损。
        if (justBought && balanceBeforeToday >= newState.currentLossFromBuyState.lossFromBuyStartBalance) {
            newState.currentLossFromBuyState = LossFromBuyState() // 新的周期开始，重置
            newState.currentLossFromBuyState.lossFromBuyStartBalance = balanceBeforeToday
            newState.currentLossFromBuyState.lossFromBuyStartDate = buyData.kLineData.date
        }

        return newState
    }

    /**
     * Part B: 交易信号逻辑 (均线交叉)
     * Processes trading signals and executes trades, updating the cross/buy data and the trade list.
     * @return A Pair containing the updated crossData (potential cross point) and buyData (current position).
     */
    private fun handleTradingSignal(
        symbol: SymbolData,
        today: AlignedMAData,
        yesterday: AlignedMAData,
        crossData: AlignedMAData?,
        buyData: AlignedMAData?,
        crossDataList: MutableList<MACrossData>, // Mutated by this function
    ): Pair<AlignedMAData?, AlignedMAData?> {
        return when (symbol.maType) {
            MAType.RSI -> handleRSIValueTradingSignal(buyData, today, symbol, crossDataList)
            else -> handleCrossTradingSignal(crossData, buyData, today, yesterday, symbol, crossDataList)
        }
    }

    private fun handleRSIValueTradingSignal(
        buyData: AlignedMAData?,
        today: AlignedMAData,
        symbol: SymbolData,
        crossDataList: MutableList<MACrossData>,
    ): Pair<AlignedMAData?, AlignedMAData?> {
        var newBuyData = buyData
        val isHolding = newBuyData != null

        // Buy condition: shortMA < extN (e.g., RSI < 30 indicates oversold)
        val shouldBuy = today.shortMAValue < symbol.longMA
        // Sell condition: shortMA > extN (e.g., RSI > 70 indicates overbought)
        val shouldSell = today.shortMAValue > symbol.extN

        if (!isHolding && shouldBuy) {
            // Currently not holding, but the buy signal is active. Execute buy.
            newBuyData = today
        } else if (isHolding && shouldSell) {
            // Currently holding, but the sell signal is active. Execute sell.
            crossDataList.add(MACrossData(newBuyData, today))
            newBuyData = null
        }
        // In all other cases, maintain the current position (or lack thereof).
        return null to newBuyData
    }

    private fun handleCrossTradingSignal(
        crossData: AlignedMAData?,
        buyData: AlignedMAData?,
        today: AlignedMAData,
        yesterday: AlignedMAData,
        symbol: SymbolData,
        crossDataList: MutableList<MACrossData>,
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
            val isJustCrossedUp = yesterday.shortMAValue < yesterday.longMAValue && todayMADiffRate >= symbol.upCrossDiffRate
            val isAfterCrossedUp = todayCrossDayMADiffRate != null && todayCrossDayMADiffRate >= symbol.upCrossDiffRate
            if (isJustCrossedUp || isAfterCrossedUp) {
                newBuyData = today // 执行买入
            }
        }
        // 3. 执行卖出逻辑 (Check Sell Signal)
        else {
            // 条件：(刚下穿且超过阈值) OR (下穿后某天超过阈值)
            val isJustCrossedDown = yesterday.shortMAValue > yesterday.longMAValue && todayMADiffRate <= symbol.downCrossDiffRate
            val isAfterCrossedDown = todayCrossDayMADiffRate != null && todayCrossDayMADiffRate <= symbol.downCrossDiffRate
            if (isJustCrossedDown || isAfterCrossedDown) {
                crossDataList.add(MACrossData(newBuyData, today)) // 记录一次完整交易
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
        val ma1DataList = dataList.map { MAData(it, it.closePrice) }
        return calculateMAValue(ma1DataList, period)
    }

    /**
     * 根据 KLineData 列表计算 N 日 EXPMA (EMA)
     *
     * @param dataList 原始 KLineData 列表（必须按日期升序排列）。
     * @param period   EMA 的计算周期 N (例如：5, 10, 60)。
     * @return 包含日期和对应 EMA 值的 MAData 列表。
     */
    fun calculateEMAData(dataList: List<KLineData>, period: Int): List<MAData> {
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
            val kLineData = dataList[i]

            // —————— 第一步：初始化（前 period-1 天无 EMA，设为 null）——————
            if (i < period - 1) {
                // 前 N-1 天：EMA 未有效形成, MA值设置为 null
                maList.add(MAData(kLineData, null))
                continue
            }

            // —————— 第二步：第 period 天（i == period - 1），用 SMA 初始化首个 EMA ——————
            if (i == period - 1) {
                val windowPrices = prices.subList(0, period) // [0, period)
                val sma = windowPrices.sum() / period.toDouble()
                previousEma = sma
            } else {
                // —————— 第三步：递推计算后续 EMA ——————
                val ema = alpha * kLineData.closePrice + (1 - alpha) * previousEma!!
                previousEma = ema
            }

            // 四舍五入，保留3位小数
            val roundedEma = previousEma.roundTo3Decimals()
            maList.add(MAData(kLineData, roundedEma))
        }
        return maList
    }

    /**
     * 根据 KLineData 列表计算相对强弱指数 (RSI)。
     *
     * @param dataList 原始 KLineData 列表（必须按日期升序排列）。
     * @param period RSI 的计算周期 N (例如：6, 12, 24)。
     * @return 包含日期和对应 RSI 值的 MAData 列表。
     */
    fun calculateRSI(dataList: List<KLineData>, period: Int): List<MAData> {
        if (dataList.size <= period) {
            return dataList.map { MAData(it, null) }
        }

        val result = mutableListOf<MAData>()
        // 在 RSI 无法计算的初始阶段，添加 null 值
        for (i in 0 until period) {
            result.add(MAData(dataList[i], null))
        }

        // 计算每日价格变化
        val changes = (1..dataList.lastIndex).map { dataList[it].closePrice - dataList[it - 1].closePrice }

        // 计算第一个周期的初始平均收益和损失
        var sumGains = 0.0
        var sumLosses = 0.0
        for (i in 0 until period) {
            val change = changes[i]
            if (change > 0) {
                sumGains += change
            } else {
                sumLosses -= change // 损失值为正
            }
        }

        var avgGain = sumGains / period
        var avgLoss = sumLosses / period

        // 计算第一个 RSI 值
        val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
        val rsi = 100.0 - (100.0 / (1.0 + rs))
        result.add(MAData(dataList[period], rsi.roundTo3Decimals()))

        // 使用平滑方法计算后续的 RSI 值
        for (i in period until changes.size) {
            val change = changes[i]
            val currentGain = if (change > 0) change else 0.0
            val currentLoss = if (change < 0) -change else 0.0

            avgGain = (avgGain * (period - 1) + currentGain) / period
            avgLoss = (avgLoss * (period - 1) + currentLoss) / period

            val nextRs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            val nextRsi = 100.0 - (100.0 / (1.0 + nextRs))
            result.add(MAData(dataList[i + 1], nextRsi.roundTo3Decimals()))
        }

        return result
    }

    fun calculateOBVMAData(dataList: List<KLineData>, period: Int): List<MAData> {
        val obvList = mutableListOf<MAData>()

        // 第一天 OBV 设为 0.0
        var currentOBV = 0.000

        // 遍历所有数据点
        for (i in 1 until dataList.size) {
            val preKLineData = dataList[i - 1]
            val kLineData = dataList[i]
            val prevClose = preKLineData.closePrice
            val currClose = kLineData.closePrice
            val currVolume = kLineData.volume

            // 检查数据有效性（可选增强健壮性）
            if (prevClose < 0 || currClose < 0 || currVolume < 0) {
                // 若数据无效，OBV 中断，继承上一日值
                obvList.add(MAData(kLineData, value = currentOBV))
                continue
            }

            // 根据收盘价变化更新 OBV
            when {
                currClose > prevClose -> currentOBV += currVolume.toDouble()
                currClose < prevClose -> currentOBV -= currVolume.toDouble()
                else -> {
                    // 收盘价相等，OBV 不变
                }
            }
            // 四舍五入，保留3位小数
            val roundedMa = currentOBV.roundTo3Decimals()

            // 6. 添加到结果列表
            obvList.add(MAData(kLineData, roundedMa))
        }

        return calculateMAValue(obvList, period)
    }

    /**
     * 计算均线数据
     */
    private fun calculateMAValue(ma1DataList: List<MAData>, period: Int): List<MAData> {
        if (period == 1) {
            return ma1DataList
        }

        val values = ma1DataList.map { it.value ?: 0.0 }
        val maList = mutableListOf<MAData>()

        // 遍历所有数据点
        for (i in ma1DataList.indices) {
            val kLineData = ma1DataList[i].kLineData

            // MA 计算的起始索引：必须向前追溯 (period - 1) 天
            // 例如，计算 MA5，需要从 i 向前到 i - 4，一共 5 个点。
            val startIndex = i - period + 1

            // 1. 处理前 N-1 天数据：MA值设置为 null
            if (startIndex < 0) {
                maList.add(MAData(kLineData, null))
                continue
            }

            // 2. 提取计算窗口内的 N 个收盘价
            // Kotlin 的 subList 索引是 [fromIndex, toIndex)，所以 toIndex = i + 1
            val windowValues = values.subList(startIndex, i + 1)

            // 3. 计算总和
            val sum = windowValues.sum()

            // 4. 计算平均值
            val rawMa = sum / period

            // 四舍五入，保留3位小数
            val roundedMa = rawMa.roundTo3Decimals()

            // 6. 添加到结果列表
            maList.add(MAData(kLineData, roundedMa))
        }

        return maList
    }

    /**
     * 根据 KLineData 列表计算 SKDJ (慢速随机指标)
     *
     * 计算逻辑：
     * 1. KDJ: 原始 K -> 快K; 原始 D -> 快D (即快K的平滑)
     * 2. SKDJ: 慢K = 快D; 慢D = 对慢K进行M2周期平滑
     *
     * @param dataList 原始 K 线数据列表（必须按日期升序排列）
     * @param n RSV 计算周期，默认 9
     * @param m1 K 值平滑周期，默认 3 (用于计算原始 K 值)
     * @param m2 D 值平滑周期，默认 3 (用于计算原始 D 值 和 慢 D 值)
     * @return List<AlignedMAData>，其中：
     * - shortMAValue = 慢 K 值 (SK)
     * - longMAValue = 慢 D 值 (SD)
     * - jValue = 慢 J 值 (SJ)
     */
    fun calculateSKDJData(
        dataList: List<KLineData>,
        n: Int = 9,
        m1: Int = 3,
        m2: Int = 3,
    ): List<AlignedMAData> {
        if (dataList.isEmpty()) {
            return emptyList()
        }

        val size = dataList.size

        // 存储 KDJ 计算过程中的 RSV 和 K 值 (快线 K)
        val rsv = DoubleArray(size) { Double.NaN }
        val fastKValues = DoubleArray(size) { Double.NaN }

        // 存储 SKDJ 最终需要的 K 值和 D 值 (慢 K 和 慢 D)
        val slowKValues = DoubleArray(size) { Double.NaN } // SK，即原 KDJ 的 D 值
        val slowDValues = DoubleArray(size) { Double.NaN } // SD，即慢 K 的平滑

        // --- Part 1: 计算原始 RSV ---
        for (i in (n - 1) until size) {
            var highestHigh = Double.MIN_VALUE
            var lowestLow = Double.MAX_VALUE

            for (j in i - n + 1..i) {
                highestHigh = max(highestHigh, dataList[j].highPrice)
                lowestLow = min(lowestLow, dataList[j].lowPrice)
            }

            rsv[i] = if (highestHigh != lowestLow) {
                100.0 * (dataList[i].closePrice - lowestLow) / (highestHigh - lowestLow)
            } else {
                // 当价格持平时，RSV 通常定义为 100 或 0，这里选择 0.0
                0.0
            }
        }

        // --- Part 2: 计算 KDJ 的 K 值 (Fast K) 和 D 值 (Fast D / Slow K) ---
        // Fast K = RSV 的 M1 周期平滑
        // Fast D (即 Slow K) = Fast K 的 M2 周期平滑

        // 初始值：通常设定 K0=D0=50 或取第一个 RSV 的值
        var prevFastK = 50.0
        var prevSlowK = 50.0 // 慢 K 的前值，即快 D 的前值

        for (i in (n - 1) until size) {
            val currentRsv = rsv[i]

            // 1. 计算 Fast K (快 K)
            // K = (prevK * (M1 - 1) + RSV) / M1
            val currentFastK = if (i == n - 1) {
                currentRsv // 第一个 K 值初始化
            } else {
                (prevFastK * (m1 - 1) + currentRsv) / m1
            }

            // 2. 计算 Slow K (慢 K, 实际上是 KDJ 中的 D 值)
            // Slow K = (prevSlowK * (M2 - 1) + Fast K) / M2
            val currentSlowK = if (i == n - 1) {
                currentFastK // 第一个 D 值初始化
            } else {
                (prevSlowK * (m2 - 1) + currentFastK) / m2
            }

            fastKValues[i] = currentFastK // 暂存备用
            slowKValues[i] = currentSlowK // 慢 K 值 (SK)

            prevFastK = currentFastK
            prevSlowK = currentSlowK
        }

        // --- Part 3: 计算 Slow D (慢 D) ---
        // Slow D = Slow K 的 M2 周期平滑

        var prevSlowD = 50.0 // 慢 D 的前值，通常设为 50 或第一个慢 K 值

        for (i in (n - 1) until size) {
            val currentSlowK = slowKValues[i]

            // Slow D = (prevSlowD * (M2 - 1) + Slow K) / M2
            val currentSlowD = if (i == n - 1) {
                currentSlowK // 第一个慢 D 值初始化为第一个慢 K 值
            } else {
                (prevSlowD * (m2 - 1) + currentSlowK) / m2
            }

            slowDValues[i] = currentSlowD // 慢 D 值 (SD)
            prevSlowD = currentSlowD
        }

        // --- Part 4: 构建结果 (SK, SD, SJ) ---
        val result = mutableListOf<AlignedMAData>()
        for (i in dataList.indices) {
            val kLine = dataList[i]

            val sk = if (i >= n - 1) slowKValues[i].roundTo3Decimals() else 0.0
            val sd = if (i >= n - 1) slowDValues[i].roundTo3Decimals() else 0.0

            // 慢 J 值 (SJ) = 3 * SK - 2 * SD
            val sj = if (sk != 0.0 || sd != 0.0) {
                (3 * sk - 2 * sd).roundTo3Decimals()
            } else {
                0.0
            }

            val aligned = AlignedMAData(
                kLineData = kLine,
                shortMAValue = sk, // 对应慢 K (SK)
                longMAValue = sd   // 对应慢 D (SD)
            )
            aligned.extValue = sj    // 对应慢 J (SJ)
            result.add(aligned)
        }

        return result
    }

    /**
     * 根据 KLineData 列表计算 MACD 指标。
     *
     * @param dataList 原始 K 线数据列表（必须按日期升序排列）
     * @param fastPeriod 短周期（通常 12）
     * @param slowPeriod 长周期（通常 26）
     * @param signalPeriod DIF 平滑周期（通常 9）
     * @return List<AlignedMAData>，其中：
     * - shortMAValue = DIF 值 (快线)
     * - longMAValue = DEA 值 (慢线)
     * - jValue = MACD Bar 柱状图值 (2 * (DIF - DEA))
     */
    fun calculateMACDData(
        dataList: List<KLineData>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9,
    ): List<AlignedMAData> {
        if (dataList.isEmpty() || slowPeriod > dataList.size) {
            return emptyList()
        }

        val size = dataList.size
        val closePrices = dataList.map { it.closePrice }

        // --- Step 1: 计算 Fast EMA (短周期 EMA) ---
        val fastEMA = calculateEMA(closePrices, fastPeriod)

        // --- Step 2: 计算 Slow EMA (长周期 EMA) ---
        val slowEMA = calculateEMA(closePrices, slowPeriod)

        // --- Step 3: 计算 DIF (离差值 / 快线) ---
        val difValues = DoubleArray(size) { Double.NaN }
        for (i in 0 until size) {
            // DIF = Fast EMA - Slow EMA
            difValues[i] = fastEMA[i] - slowEMA[i]
        }

        // --- Step 4: 计算 DEA (平滑离差 / 慢线) ---
        // DEA 是 DIF 的 Signal 周期 EMA
        // 注意：这里的 EMA 计算应该从 DIF 序列开始。
        val deaValues = calculateEMA(difValues.toList(), signalPeriod)

        // --- Step 5: 构建结果 (DIF, DEA, MACD Bar) ---
        val result = mutableListOf<AlignedMAData>()

        // 从长周期 (slowPeriod) 开始，指标值才相对稳定。
        val validStartIndex = slowPeriod - 1

        for (i in dataList.indices) {
            val kLine = dataList[i]

            val dif = if (i >= validStartIndex) difValues[i].roundTo3Decimals() else 0.0
            val dea = if (i >= validStartIndex) deaValues[i].roundTo3Decimals() else 0.0

            // MACD Bar = 2 * (DIF - DEA)
            val macdBar = if (i >= validStartIndex) {
                (2.0 * (difValues[i] - deaValues[i])).roundTo3Decimals()
            } else {
                0.0
            }

            val aligned = AlignedMAData(
                kLineData = kLine,
                shortMAValue = dif,      // 对应 DIF (快线)
                longMAValue = dea        // 对应 DEA (慢线)
            )
            aligned.extValue = macdBar     // 对应 MACD Bar (柱状图)
            result.add(aligned)
        }

        return result
    }

    /**
     * 辅助函数：计算指定周期的指数移动平均 (EMA)。
     * EMA 的权重系数（平滑因子）为: 2 / (period + 1)
     *
     * @param dataList 原始数据点列表 (通常是收盘价)
     * @param period EMA 周期
     * @return 包含 EMA 值的 DoubleArray，与输入列表大小相同
     */
    private fun calculateEMA(dataList: List<Double>, period: Int): DoubleArray {
        val size = dataList.size
        if (size == 0) return DoubleArray(0)

        val emaValues = DoubleArray(size) { Double.NaN }
        // 平滑因子：2 / (N + 1)
        val alpha = 2.0 / (period + 1)

        // 第一个 EMA 值等于第一个数据点的值
        var currentEMA = dataList[0]
        emaValues[0] = currentEMA

        // 从第二个数据点开始迭代计算
        for (i in 1 until size) {
            val price = dataList[i]
            // EMA_t = alpha * Price_t + (1 - alpha) * EMA_{t-1}
            currentEMA = alpha * price + (1.0 - alpha) * currentEMA
            emaValues[i] = currentEMA
        }
        return emaValues
    }

    // 辅助函数：四舍五入保留3位小数
    private fun Double.roundTo3Decimals(): Double {
        return toBigDecimal()
            .setScale(3, RoundingMode.HALF_UP)
            .toDouble()
    }
}
