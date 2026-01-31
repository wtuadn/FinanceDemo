package com.example.myapplication

import com.example.myapplication.data.AlignedMAData
import com.example.myapplication.data.KLineData
import com.example.myapplication.data.MACrossResult
import com.example.myapplication.data.SymbolData
import com.example.myapplication.utils.MACrossUtils
import com.example.myapplication.utils.MACrossUtils.MAType
import com.example.myapplication.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicInteger

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 * 651ED65C-9613-4143-8A5F-8DB6E1D235B6
 */
class SinaFinance {
    private val symbols = listOf(
        SymbolData("sh588200", "科创芯片ETF", 240, 1, 9, 2, 46, MAType.SKDJ, 0.050, 0.000, 0.256, 0.00322, -0.088),
        SymbolData("sh512880", "证券ETF", 240, 1, 8, 0, 380, MAType.CMF, 0.000, 0.000, 0.092, 0.01031, -0.016),
        SymbolData("sz159326", "电网设备ETF", 240, 1, 3, 3, 37, MAType.SKDJ, 0.050, 0.000, 0.178, 0.00224, -0.013),
        SymbolData("sh563360", "A500ETF", 240, 1, 3, 12, 4, MAType.SKDJ, 0.050, -0.050, 0.114, 0.00131, -0.022),
        SymbolData("sh513500", "标普500ETF", 240, 1, 3, 24, 27, MAType.SKDJ, 0.000, -0.100, 0.147, 0.00067, -0.059),
        SymbolData("sh510500", "中证500ETF", 240, 1, 6, 17, 35, MAType.RSI, 0.000, 0.000, 0.053, 0.00279, -0.075),
        SymbolData("sh510050", "上证50ETF", 240, 5, 5, 18, 69, MAType.RSI, 0.000, 0.000, 0.066, 0.00108, -0.049),
        SymbolData("sh510300", "沪深300ETF", 240, 1, 4, -500, 740, MAType.CMF, 0.000, 0.000, 0.077, 0.00094, -0.045),
        SymbolData("sh512040", "价值100ETF", 240, 1, 7, 19, 55, MAType.RSI, 0.000, 0.000, 0.092, 0.00299, -0.050),
        SymbolData("sz159883", "医疗器械ETF", 240, 1, 30, 38, 44, MAType.RSI, 0.000, 0.000, 0.119, 0.00241, -0.068),
        SymbolData("sz159928", "消费ETF", 240, 5, 51, 56, 11, MAType.MACD, 0.000, 0.000, 0.145, 0.00135, -0.067),
        SymbolData("sh512980", "传媒ETF", 240, 1, 9, 20, 30, MAType.RSI, 0.000, 0.000, 0.048, 0.00650, -0.041),
        SymbolData("sz159869", "游戏ETF", 240, 1, 33, 36, 23, MAType.MACD, 0.100, -0.100, 0.221, 0.00288, -0.079),
        SymbolData("sz159852", "软件ETF", 240, 1, 33, 3, 2, MAType.SKDJ, 0.100, 0.000, 0.149, 0.00358, -0.047),
        SymbolData("sh516510", "云计算ETF", 240, 1, 1, 5, 0, MAType.SMA, 0.100, -0.040, 0.193, 0.00359, -0.072),
        SymbolData("sz159998", "计算机ETF", 240, 1, 38, 2, 3, MAType.SKDJ, 0.100, 0.000, 0.100, 0.00404, -0.027),
        SymbolData("sh515400", "大数据ETF", 240, 1, 1, 5, 0, MAType.VWAP, 0.060, -0.020, 0.197, 0.00312, -0.050),
        SymbolData("sh601398", "工商银行", 240, 1, 7, -340, 620, MAType.CMF, 0.000, 0.000, 0.075, 0.00059, -0.054),
        SymbolData("sh600036", "招商银行", 240, 1, 13, 23, 55, MAType.RSI, 0.000, 0.000, 0.059, 0.00337, -0.067),
        SymbolData("sh513120", "港股创新药ETF", 240, 1, 5, 25, 42, MAType.RSI, 0.000, 0.000, 0.127, 0.00375, -0.092),
        SymbolData("sh515790", "光伏ETF", 240, 1, 20, 29, 42, MAType.RSI, 0.000, 0.000, 0.079, 0.00374, -0.077),
        SymbolData("sh513550", "港股通50ETF", 240, 1, 27, -140, 80, MAType.CMF, 0.000, 0.000, 0.105, 0.00105, -0.050),
        SymbolData("sh512710", "军工龙头ETF", 240, 1, 6, 15, 39, MAType.RSI, 0.000, 0.000, 0.073, 0.00588, -0.039),
        SymbolData("sz159227", "航空航天ETF", 240, 1, 19, 34, 7, MAType.MACD, 0.050, 0.000, 0.252, 0.00398, -0.019),
        SymbolData("sz159218", "卫星产业ETF", 240, 1, 2, 2, 4, MAType.SKDJ, 0.000, -0.100, 0.545, 0.00649, -0.019),
        SymbolData("sz159813", "半导体ETF", 240, 1, 45, 60, 5, MAType.MACD, 0.100, 0.000, 0.125, 0.00251, -0.062),
        SymbolData("sz159713", "稀土ETF", 240, 1, 1, 5, 0, MAType.SMA, 0.080, -0.010, 0.139, 0.00416, -0.048),
        SymbolData("sz159985", "豆粕ETF", 240, 5, 14, 2, 2, MAType.SKDJ, 0.000, -0.100, 0.162, 0.00087, -0.035),
        SymbolData("sh561330", "矿业ETF", 240, 1, 9, 26, 45, MAType.RSI, 0.000, 0.000, 0.099, 0.00590, -0.024),
        SymbolData("sh513400", "道琼斯ETF", 240, 1, 11, 37, 45, MAType.RSI, 0.000, 0.000, 0.070, 0.00333, -0.045),
        SymbolData("sh510230", "金融ETF", 240, 5, 5, 21, 61, MAType.RSI, 0.000, 0.000, 0.084, 0.00134, -0.049),
        SymbolData("sh516860", "金融科技ETF", 240, 1, 5, -40, 460, MAType.CMF, 0.000, 0.000, 0.199, 0.00646, -0.028),
        SymbolData("sh512010", "医药ETF", 240, 5, 21, 42, 5, MAType.MACD, 0.100, -0.100, 0.129, 0.00088, -0.035),
        SymbolData("sz159766", "旅游ETF", 240, 5, 9, 30, 29, MAType.MACD, 0.050, 0.000, 0.111, 0.00191, -0.038),
        SymbolData("sh588790", "科创AIETF", 240, 1, 7, 36, 5, MAType.MACD, 0.000, 0.000, 0.325, 0.00482, -0.062),
        SymbolData("sh513310", "中韩半导体ETF", 240, 1, 1, 20, 0, MAType.SMA, 0.040, -0.020, 0.252, 0.00205, -0.034),
        SymbolData("sh588220", "科创100ETF基金", 240, 1, 6, 17, 36, MAType.RSI, 0.000, 0.000, 0.075, 0.00657, -0.078),
        SymbolData("sh588000", "科创50ETF", 240, 1, 13, 22, 51, MAType.RSI, 0.000, 0.000, 0.086, 0.00585, -0.034),
        SymbolData("sz159755", "电池ETF", 240, 1, 31, 34, 7, MAType.MACD, 0.050, 0.000, 0.129, 0.00289, -0.101),
        SymbolData("sh513090", "香港证券ETF", 240, 1, 22, 0, 80, MAType.CMF, 0.000, 0.000, 0.139, 0.00579, -0.049),
        SymbolData("sh562500", "机器人ETF", 240, 1, 35, 4, 5, MAType.SKDJ, 0.100, 0.000, 0.168, 0.00308, -0.030),
        SymbolData("sz159915", "易方达创业板ETF", 240, 1, 14, 30, 38, MAType.RSI, 0.000, 0.000, 0.079, 0.00327, -0.078),
        SymbolData("sh515050", "5G通信ETF", 240, 1, 29, 5, 22, MAType.SKDJ, 0.000, -0.100, 0.275, 0.00158, -0.103),
        SymbolData("sz159201", "华夏国证自由现金流ETF", 240, 1, 2, 2, 6, MAType.SKDJ, 0.000, -0.100, 0.132, 0.00135, -0.009),
        SymbolData("sh512890", "红利低波ETF", 240, 1, 47, 47, 63, MAType.RSI, 0.000, 0.000, 0.170, 0.00092, -0.076),
        SymbolData("sh515100", "红利低波100ETF", 240, 1, 7, 20, 76, MAType.RSI, 0.000, 0.000, 0.075, 0.00113, -0.047),
        SymbolData("sh515450", "红利低波50ETF", 240, 5, 28, 48, 54, MAType.RSI, 0.000, 0.000, 0.096, 0.00134, -0.062),
        SymbolData("sh513820", "港股红利ETF", 240, 1, 2, 2, 50, MAType.SKDJ, 0.100, -0.050, 0.109, 0.00134, -0.041),
        SymbolData("sz159545", "恒生红利低波ETF", 240, 1, 2, 16, 6, MAType.SKDJ, 0.000, -0.100, 0.170, 0.00144, -0.022),
        SymbolData("sh513130", "恒生科技ETF", 240, 5, 41, 48, 33, MAType.MACD, 0.000, 0.000, 0.188, 0.00229, -0.034),
        SymbolData("sz159892", "恒生医药ETF", 240, 5, 5, 20, 30, MAType.RSI, 0.000, 0.000, 0.075, 0.00301, -0.091),
        SymbolData("sz159941", "纳指ETF广发", 240, 1, 8, 25, 49, MAType.RSI, 0.000, 0.000, 0.098, 0.00369, -0.090),
        SymbolData("sh518880", "黄金ETF", 240, 1, 5, 16, 72, MAType.RSI, 0.000, 0.000, 0.061, 0.00131, -0.034),
    )

    @Test
    fun main() = runBlocking {
        // val symbol = symbols.first()
        val symbol = symbols.find { it.code == "" || it.desc == "卫星产业ETF" }!!

        calculateBestSingleArgs(symbol) { true }
        // calculateSpecificArg(symbol)
        // calculateBestSingleArgs(symbol) { it.maType != MAType.SKDJ }
        // calculateBestSingleArgs(symbol) { it.maType in listOf(MAType.MACD, MAType.RSI ) }
        // queryTradeSignal(symbols)
        // queryTradeSignal(listOf(symbol))

        // calculateSpecificArg(SymbolData("sz159892", "恒生医药ETF", 240, 1, 1, 40, 0, MAType.OBV, 0.020, 0.000, 0.328, 0.00426, -0.007))
        // queryTradeSignal(listOf(SymbolData(code="sh513130",desc="恒生科技ETF",scale=240,d=1,shortMA=13,longMA=23,extN=3,maType=MAType.SKDJ,upCrossDiffRate=0.060,downCrossDiffRate=0.000,yearlyPercentage=0.139,dailyPercentage=0.00166,mdd=-0.048)))

        // fetchJsonFile()

        printBasicInfo()
    }

    /**
     * 根据提供的参数组合列表，使用协程并行计算最优的交易策略参数
     * (优化版本: 按CalculationArgs顺序执行，分批处理以降低内存峰值)
     *
     * @param symbol 要计算的股票或ETF信息
     * @param argsList 包含多个CalculationArgs对象的列表，每个对象定义了一组要测试的参数范围
     */
    private suspend fun calculateBestArgs(symbol: SymbolData, argsList: List<CalculationArgs>) = coroutineScope {
        val scale = 240

        // 1. 预先获取所有需要的K线数据
        val allDs = argsList.flatMap { it.d }.distinct()
        val kLineDataMap = allDs.associateWith { d ->
            val kLineSymbol = symbol.copy(d = d)
            Utils.getSinaKLineData(kLineSymbol, findBestData = false, useLocalData = true, datalen = 10000)
        }

        // 定义一个可复用的lambda，用于根据MDD和总收益率进行筛选和排序
        val filterAndSortByMdd = {
                results: List<Pair<MACrossResult, String>>,
                sortSelector: (Pair<MACrossResult, String>) -> Double,
                start: Int, end: Int, size: Int,
            ->
            results.groupBy { it.first.getTotalDesc() }.map { it.value.first() } // 完全一样的结果只保留一个
                .filter {
                    val mdd = it.first.maxDrawDownData.maxLossFromBuyRate // 最大本金亏损 (为负数)
                    (-mdd * 100).toInt() in start until end
                }.sortedByDescending(sortSelector).take(size)
        }

        // 存储每个计算组筛选出的最优结果的集合
        val allFinalResultsFromGroups = mutableListOf<Pair<MACrossResult, String>>()

        // 2. 按顺序遍历每个CalculationArgs，分批计算和筛选
        argsList.forEach { args ->
            // 2.1. 生成当前计算组的参数组合
            val currentParamCombinations = mutableListOf<Pair<CalculationArgs, SymbolData>>()
            args.d.forEach { d ->
                args.shortMAList.forEach { shortMA ->
                    args.longMAList.forEach { longMA ->
                        args.extNList.forEach { extN ->
                            args.upCrossDiffRateList.forEach { upCrossDiffRate ->
                                args.downCrossDiffRateList.forEach { downCrossDiffRate ->
                                    val currentSymbol = symbol.copy(
                                        scale = scale,
                                        d = d,
                                        shortMA = shortMA,
                                        longMA = longMA,
                                        maType = args.maType,
                                        extN = extN,
                                        upCrossDiffRate = upCrossDiffRate,
                                        downCrossDiffRate = downCrossDiffRate
                                    )
                                    if (args.argsFilter(currentSymbol)) {
                                        currentParamCombinations.add(args to currentSymbol)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (args.maType.isVolumeBased() && kLineDataMap.values.any { kline -> kline.any { it.volume <= 0 } }) {
                println("跳过 ${args.maType} 计算，因为缺少成交量数据")
                return@forEach
            }
            if (currentParamCombinations.isEmpty()) {
                return@forEach
            }

            println("开始计算 ${args.maType}，总任务数: ${currentParamCombinations.size}")
            val completedTasks = AtomicInteger(0)
            val percent = AtomicInteger(0)
            val totalTasks = currentParamCombinations.size

            // 为每个计算组创建一个独立的缓存
            val cachedAlignedMADataMap = mutableMapOf<String, List<AlignedMAData>>()

            // 2.2. 使用协程并行计算当前组
            val resultsFromThisArg = mutableListOf<Pair<MACrossResult, String>>()
            val sortSelector: (Pair<MACrossResult, String>) -> Double = { it.first.totalCrossData.totalPercentage }
            currentParamCombinations.chunked(10000).map { chunk ->
                async(Dispatchers.Default) {
                    chunk.mapNotNull { (currentArgs, currentSymbol) ->
                        val resultData = kLineDataMap[currentSymbol.d]?.let { kLineData ->
                            val filteredKLineData = currentArgs.kLineDataFilter(kLineData)
                            if (filteredKLineData.isNotEmpty() && (!currentArgs.maType.isVolumeBased() || filteredKLineData.all { it.volume > 0 })) {
                                val result = MACrossUtils.calculateMACross(
                                    symbol = currentSymbol,
                                    kLineData = filteredKLineData,
                                    cachedAlignedMADataMap = cachedAlignedMADataMap,
                                )
                                if (result.totalCrossData.crossDataList.isEmpty()) {
                                    null
                                } else {
                                    val symbolForLogging = currentSymbol.copy(
                                        yearlyPercentage = result.yearlyPercentage,
                                        dailyPercentage = result.totalCrossData.dailyPercentage,
                                        mdd = result.maxDrawDownData.maxLossFromBuyRate
                                    )
                                    result to getArgStr(symbolForLogging, result)
                                }
                            } else {
                                null
                            }
                        }
                        val progress = completedTasks.incrementAndGet()
                        val newPercent = progress * 100 / totalTasks
                        if (newPercent > 0 && percent.compareAndSet(newPercent - 1, newPercent)) {
                            if (newPercent % 20 == 0) {
                                val freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024)
                                println("${args.maType}: $progress / $totalTasks (${percent}%) $freeMemory MB")
                            }
                        }
                        resultData
                    }.let { results ->
                        System.gc()
                        mutableListOf<Pair<MACrossResult, String>>().also { chunkResultList ->
                            chunkResultList.addAll(filterAndSortByMdd(results, sortSelector, 0, 5, 5))
                            chunkResultList.addAll(filterAndSortByMdd(results, sortSelector, 5, 10, 5))
                            chunkResultList.addAll(filterAndSortByMdd(results, sortSelector, 10, 15, 5))
                        }
                    }
                }
            }.awaitAll().forEach(resultsFromThisArg::addAll)

            // 2.3. 对当前组的结果进行初步过滤
            resultsFromThisArg.removeAll { it.first.yearlyPercentage < 0.04 }
            resultsFromThisArg.removeAll { it.first.totalCrossData.totalCount <= 5 }
            resultsFromThisArg.removeAll { it.first.totalCrossData.totalCount / it.first.yearCrossDataMap.size > 6 }

            // 2.4. 为当前组执行筛选并输出 avgMaxPercentage
            println("${args.maType} 初步过滤后，找到 ${resultsFromThisArg.size} 个有效结果。开始为本组筛选最优结果...")
            val avgMaxPercentageList = filterAndSortByMdd(resultsFromThisArg, sortSelector, 0, 15, 5)
            if (avgMaxPercentageList.isNotEmpty()) {
                val avgMaxPercentage =
                    Utils.getPercentageString(avgMaxPercentageList.sumOf(sortSelector) / avgMaxPercentageList.size)
                println("${args.maType} 平均最大涨幅 $avgMaxPercentage")
            }

            val groupFinalResultList = mutableListOf<Pair<MACrossResult, String>>()
            groupFinalResultList.addAll(filterAndSortByMdd(resultsFromThisArg, sortSelector, 0, 5, 5))
            groupFinalResultList.addAll(filterAndSortByMdd(resultsFromThisArg, sortSelector, 5, 10, 5))
            groupFinalResultList.addAll(filterAndSortByMdd(resultsFromThisArg, sortSelector, 10, 15, 5))

            // 2.5. 将当前组的最优结果添加到总列表中
            allFinalResultsFromGroups.addAll(groupFinalResultList)
            println("${args.maType} 计算完成，本组选出 ${groupFinalResultList.size} 个较优结果。")
        }

        // 3. 所有计算完成后，对收集到的所有组的最优结果进行最终的筛选和排序
        println("\n\n所有计算组已完成，总共收集到 ${allFinalResultsFromGroups.size} 个较优结果，开始进行最终全局排序...")
        if (allFinalResultsFromGroups.isEmpty()) {
            println("没有找到符合条件的结果。")
            return@coroutineScope
        }

        // 在所有分组结果的集合上，再次执行筛选，得到全局最优解
        val avgMaxPercentageList = filterAndSortByMdd(allFinalResultsFromGroups, { it.first.totalCrossData.totalPercentage }, 0, 15, 5)
        if (avgMaxPercentageList.isNotEmpty()) {
            val avgMaxPercentage =
                Utils.getPercentageString(avgMaxPercentageList.sumOf { it.first.totalCrossData.totalPercentage } / avgMaxPercentageList.size)
            println("全局平均最大涨幅$avgMaxPercentage")
        }

        val sortSelector: (Pair<MACrossResult, String>) -> Double = {
            val mdd = it.first.maxDrawDownData.maxLossFromBuyRate
            it.first.totalCrossData.dailyPercentage * 240 + 3 * mdd
        }

        val finalResultList = mutableListOf<Pair<MACrossResult, String>>()
        finalResultList.addAll(filterAndSortByMdd(allFinalResultsFromGroups, sortSelector, 0, 5, 5))
        finalResultList.addAll(filterAndSortByMdd(allFinalResultsFromGroups, sortSelector, 5, 10, 5))
        finalResultList.addAll(filterAndSortByMdd(allFinalResultsFromGroups, sortSelector, 10, 15, 5))

        // 最终排序和输出
        if (finalResultList.isEmpty()) {
            println("最终结果列表为空，没有可显示的数据。")
        } else {
            finalResultList.sortedByDescending(sortSelector).forEach {
                // println(it.first.getTotalDesc())
                println("\n${it.second} \n${it.first.getTotalDesc()} \n${it.first.totalCrossData.crossDataList.joinToString("\n")}")
            }
        }
    }

    private fun queryTradeSignal(symbols: List<SymbolData>) {
        symbols
            .forEachIndexed { i, it ->
                // .find { it.desc == "上证50ETF" }
                // ?.also {
                if (i > 0) Thread.sleep(Utils.httpDelay)
                val backtestLog = runCatching { File("src/main/assets", "backtest.txt").readText() }.getOrNull()
                val tradeSignalData = MACrossUtils.getTradeSignal(it, backtestLog)
                println("${it.code} ${it.desc} d=${it.d} ${tradeSignalData.takeLast(3)}")
            }
    }

    private fun getArgStr(symbol: SymbolData, result: MACrossResult): String =
        "--- 参数：code=\"${symbol.code}\",desc=\"${symbol.desc}\",scale=${symbol.scale},d=${symbol.d},shortMA=${symbol.shortMA},longMA=${symbol.longMA},extN=${symbol.extN},maType=MAType.${symbol.maType}" +
            ",upCrossDiffRate=${String.format("%.3f", symbol.upCrossDiffRate)},downCrossDiffRate=${String.format("%.3f", symbol.downCrossDiffRate)}" +
            ",yearlyPercentage=${String.format("%.3f", symbol.yearlyPercentage)}" +
            ",dailyPercentage=${String.format("%.5f", symbol.dailyPercentage)}" +
            ",mdd=${String.format("%.3f", symbol.mdd)})," +
            "\n有效数据时间${result.alignedMAData.firstOrNull()?.kLineData?.date} - ${result.alignedMAData.lastOrNull()?.kLineData?.date}"

    private suspend fun calculateBestSingleArgs(symbol: SymbolData, argsFilter: (CalculationArgs) -> Boolean) {
        val filter = { kLineData: List<KLineData> ->
            kLineData.filterNot { it.date.split("-").first().toInt() < 2016 }
        }
        val argsList = listOf(
            CalculationArgs(
                maType = MAType.SKDJ,
                d = listOf(1, 5),
                shortMAList = Utils.newList(listOf(2), 40, 1), // K 值平滑周期
                longMAList = Utils.newList(listOf(2), 30, 1), // D 值平滑周期
                extNList = Utils.newList(listOf(2), 50, 1), // RSV 计算周期
                upCrossDiffRateList = Utils.newList(listOf(0.0), 0.10, 0.05),
                downCrossDiffRateList = Utils.newList(listOf(0.0), -0.10, -0.05),
                argsFilter = { true },
                kLineDataFilter = filter,
            ),
            CalculationArgs(
                maType = MAType.MACD,
                d = listOf(1, 5),
                shortMAList = Utils.newList(listOf(5), 60, 2), //短周期
                longMAList = Utils.newList(listOf(30), 60, 2), //长周期
                extNList = Utils.newList(listOf(5), 60, 2), // DIF 平滑周期
                upCrossDiffRateList = Utils.newList(listOf(0.0), 0.10, 0.05),
                downCrossDiffRateList = Utils.newList(listOf(0.0), -0.10, -0.05),
                argsFilter = { it.shortMA < it.longMA },
                kLineDataFilter = filter,
            ),
            CalculationArgs(
                maType = MAType.SMA,
                d = listOf(1, 5),
                shortMAList = Utils.newList(listOf(1, 5), 60, 5),
                longMAList = Utils.newList(listOf(5), 120, 5),
                upCrossDiffRateList = Utils.newList(listOf(0.0), 0.20, 0.01),
                downCrossDiffRateList = Utils.newList(listOf(0.0), -0.20, -0.01),
                argsFilter = { it.shortMA < it.longMA },
                kLineDataFilter = filter,
            ),
            CalculationArgs(
                maType = MAType.EMA,
                d = listOf(1, 5),
                shortMAList = Utils.newList(listOf(1, 5), 60, 5),
                longMAList = Utils.newList(listOf(5), 120, 5),
                upCrossDiffRateList = Utils.newList(listOf(0.0), 0.20, 0.01),
                downCrossDiffRateList = Utils.newList(listOf(0.0), -0.20, -0.01),
                argsFilter = { it.shortMA < it.longMA },
                kLineDataFilter = filter,
            ),
            CalculationArgs(
                maType = MAType.RSI,
                d = listOf(1, 5),
                shortMAList = Utils.newList(listOf(5), 60, 1),   // RSI单根线
                longMAList = Utils.newList(listOf(15), 70, 1),    // 超卖阈值
                extNList = Utils.newList(listOf(30), 80, 1), // 超买阈值
                argsFilter = { it.longMA < it.extN - 5 },
                kLineDataFilter = filter,
            ),
            CalculationArgs(
                maType = MAType.OBV,
                d = listOf(1, 5),
                shortMAList = Utils.newList(listOf(1, 5), 60, 5),
                longMAList = Utils.newList(listOf(5), 180, 5),
                upCrossDiffRateList = Utils.newList(listOf(0.0), 0.20, 0.01),
                downCrossDiffRateList = Utils.newList(listOf(0.0), -0.20, -0.01),
                argsFilter = { it.shortMA < it.longMA },
                kLineDataFilter = filter,
            ),
            CalculationArgs(
                maType = MAType.VPT,
                d = listOf(1, 5),
                shortMAList = Utils.newList(listOf(1, 5), 60, 5),
                longMAList = Utils.newList(listOf(5), 180, 5),
                upCrossDiffRateList = Utils.newList(listOf(0.0), 0.20, 0.01),
                downCrossDiffRateList = Utils.newList(listOf(0.0), -0.20, -0.01),
                argsFilter = { it.shortMA < it.longMA },
                kLineDataFilter = filter,
            ),
            CalculationArgs(
                maType = MAType.CMF,
                d = listOf(1, 5),
                shortMAList = Utils.newList(listOf(2), 60, 1),   // 周期
                longMAList = Utils.newList(listOf(0), -1000, -20), // 资金流出阈值
                extNList = Utils.newList(listOf(0), 1000, 20),    // 资金流入阈值
                argsFilter = { true },
                kLineDataFilter = filter,
            ),
            CalculationArgs(
                maType = MAType.VWAP,
                d = listOf(1, 5),
                shortMAList = Utils.newList(listOf(1, 5), 60, 5),
                longMAList = Utils.newList(listOf(5), 180, 5),
                upCrossDiffRateList = Utils.newList(listOf(0.0), 0.20, 0.01),
                downCrossDiffRateList = Utils.newList(listOf(0.0), -0.20, -0.01),
                argsFilter = { it.shortMA < it.longMA },
                kLineDataFilter = filter,
            ),
        )
        calculateBestArgs(symbol, argsList.filter(argsFilter))
    }

    private fun calculateSpecificArg(
        symbol: SymbolData,
    ) {
        var kLineData = Utils.getSinaKLineData(symbol, findBestData = false, useLocalData = true, datalen = 10000)
        kLineData = kLineData.filterNot { it.date.split("-").first().toInt() < 2016 }
        if (symbol.maType == MAType.OBV) {
            kLineData = Utils.findLatestSublist(kLineData) { it.volume > 0 }
        }
        val result = MACrossUtils.calculateMACross(
            symbol = symbol,
            kLineData = kLineData,
        )
        val symbolForLogging = symbol.copy(
            yearlyPercentage = result.yearlyPercentage,
            dailyPercentage = result.totalCrossData.dailyPercentage,
            mdd = result.maxDrawDownData.maxLossFromBuyRate
        )
        // println(getArgStr(symbolForLogging, result))
        println("\n${getArgStr(symbolForLogging, result)} \n${result.getTotalDesc()} \n${result.totalCrossData.crossDataList.joinToString("\n")}")
    }

    private fun printBasicInfo() {
        repeat(3) { println() }
        val backtestLog = runCatching { File("src/main/assets", "backtest.txt").readText() }.getOrNull()
        // backtestLog?.split("---")?.filter { it.trim().split("\n").size <= 5 + 3 }?.forEach {
        //     println(it)
        // }
        // symbols.sortedByDescending { it.dailyPercentage }.forEach {
        //     println(
        //         "${it.code} ${it.desc}" +
        //             " yearlyPercentage:${Utils.getPercentageString(it.yearlyPercentage)}" +
        //             " dailyPercentage:${Utils.getPercentageString(it.dailyPercentage)}"
        //     )
        // }

        println("\n=== Group Size ===")
        symbols.groupBy { it.d }.forEach { (d, symbols) -> println("d=$d ${symbols.size}") }
        symbols.sortedBy { it.maType.ordinal }.groupBy { it.maType }.forEach { (maType, symbolsInGroup) ->
            // 计算并打印参数范围
            val shortMARange = "shortMA[${symbolsInGroup.minOf { it.shortMA }}, ${symbolsInGroup.maxOf { it.shortMA }}]"
            val longMARange = "longMA[${symbolsInGroup.minOf { it.longMA }}, ${symbolsInGroup.maxOf { it.longMA }}]"
            val extNRange = "extN[${symbolsInGroup.minOf { it.extN }}, ${symbolsInGroup.maxOf { it.extN }}]"
            println("$maType (${symbolsInGroup.size}): $shortMARange, $longMARange, $extNRange")
        }

        Utils.printHeapUsage()
    }

    @Test
    fun fetchJsonFile() {
        var count = 0
        val scale = 240
        val d = listOf(1, 5)
        symbols.forEach { symbol ->
            d.forEach { d ->
                if (count++ > 0) Thread.sleep(Utils.httpDelay)

                val api =
                    "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=${symbol.code}&scale=${scale * d}&ma=no&datalen=10000"
                val json = Utils.httpGet(
                    urlString = api,
                    headMap = mapOf(
                        "Referer" to "https://finance.sina.com.cn",
                        "host" to "hq.sinajs.cn",
                    )
                )
                if (json.isNullOrEmpty()) return@forEach

                val file = File("data", "${symbol.code}.$d.json")
                file.parentFile.mkdirs()
                BufferedWriter(FileWriter(file)).use { writer ->
                    writer.write(json)
                    writer.flush()
                }
                println("${symbol.code} ${symbol.desc} ${d}")
            }
        }
    }
}