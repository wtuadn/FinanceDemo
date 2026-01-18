package com.example.myapplication

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
        SymbolData("sh512880", "证券ETF", 240, 1, 17, 27, 47, MAType.RSI, 0.000, 0.000, 0.046, 0.00217, -0.053),
        SymbolData("sz159326", "电网设备ETF", 240, 1, 5, 36, 46, MAType.MACD, 0.100, -0.080, 0.166, 0.00230, -0.031),
        SymbolData("sh563360", "A500ETF", 240, 1, 60, 70, 0, MAType.SMA, 0.000, 0.000, 0.175, 0.00160, -0.012),
        SymbolData("sh513500", "标普500ETF", 240, 1, 5, 55, 0, MAType.OBV, 0.140, -0.030, 0.126, 0.00080, -0.047),
        SymbolData("sh510500", "中证500ETF", 240, 5, 28, 2, 3, MAType.SKDJ, 0.050, 0.000, 0.068, 0.00080, -0.064),
        SymbolData("sh510050", "上证50ETF", 240, 1, 17, 27, 67, MAType.RSI, 0.000, 0.000, 0.072, 0.00096, -0.076),
        SymbolData("sh510300", "沪深300ETF", 240, 1, 28, 33, 24, MAType.SKDJ, 0.080, -0.050, 0.074, 0.00057, -0.071),
        SymbolData("sh512040", "价值100ETF", 240, 1, 15, 20, 0, MAType.OBV, 0.090, -0.030, 0.105, 0.00061, -0.077),
        SymbolData("sz159883", "医疗器械ETF", 240, 1, 7, 17, 42, MAType.RSI, 0.000, 0.000, 0.088, 0.00482, -0.041),
        SymbolData("sz159928", "消费ETF", 240, 5, 51, 56, 11, MAType.MACD, 0.000, 0.000, 0.146, 0.00136, -0.067),
        SymbolData("sh512980", "传媒ETF", 240, 1, 33, 2, 3, MAType.SKDJ, 0.100, -0.010, 0.120, 0.00242, -0.048),
        SymbolData("sz159869", "游戏ETF", 240, 5, 2, 18, 3, MAType.SKDJ, 0.000, 0.000, 0.246, 0.00229, -0.059),
        SymbolData("sz159852", "软件ETF", 240, 1, 1, 5, 0, MAType.EMA, 0.070, -0.040, 0.205, 0.00276, -0.058),
        SymbolData("sh516510", "云计算ETF", 240, 1, 53, 2, 4, MAType.SKDJ, 0.080, -0.030, 0.261, 0.00283, -0.066),
        SymbolData("sz159998", "计算机ETF", 240, 1, 43, 2, 14, MAType.SKDJ, 0.100, -0.060, 0.168, 0.00176, -0.067),
        SymbolData("sh515400", "大数据ETF", 240, 1, 1, 5, 0, MAType.EMA, 0.070, -0.040, 0.181, 0.00291, -0.038),
        SymbolData("sh601398", "工商银行", 240, 1, 30, 40, 0, MAType.OBV, 0.190, -0.020, 0.076, 0.00063, -0.037),
        SymbolData("sh600036", "招商银行", 240, 1, 10, 25, 0, MAType.SMA, 0.090, -0.060, 0.179, 0.00094, -0.062),
        SymbolData("sh513120", "港股创新药ETF", 240, 1, 48, 3, 19, MAType.SKDJ, 0.010, -0.060, 0.317, 0.00218, -0.077),
        SymbolData("sh515790", "光伏ETF", 240, 1, 30, 35, 0, MAType.SMA, 0.040, 0.000, 0.180, 0.00249, -0.028),
        SymbolData("sh513550", "港股通50ETF", 240, 5, 21, 41, 16, MAType.MACD, 0.100, -0.030, 0.140, 0.00124, -0.049),
        SymbolData("sh512710", "军工龙头ETF", 240, 1, 7, 17, 37, MAType.RSI, 0.000, 0.000, 0.067, 0.00579, -0.039),
        SymbolData("sz159227", "航空航天ETF", 240, 1, 2, 3, 19, MAType.SKDJ, 0.100, -0.050, 0.211, 0.00397, -0.033),
        SymbolData("sz159218", "卫星产业ETF", 240, 1, 3, 2, 3, MAType.SKDJ, 0.000, -0.070, 0.565, 0.00719, -0.016),
        SymbolData("sz159813", "半导体ETF", 240, 5, 2, 3, 39, MAType.SKDJ, 0.000, -0.010, 0.147, 0.00116, -0.082),
        SymbolData("sz159713", "稀土ETF", 240, 1, 1, 5, 0, MAType.SMA, 0.080, -0.010, 0.139, 0.00416, -0.048),
        SymbolData("sz159985", "豆粕ETF", 240, 5, 53, 2, 4, MAType.SKDJ, 0.000, -0.040, 0.142, 0.00070, -0.037),
        SymbolData("sh561330", "矿业ETF", 240, 1, 30, 200, 0, MAType.OBV, 0.000, -0.190, 0.371, 0.00404, -0.016),
        SymbolData("sh513400", "道琼斯ETF", 240, 1, 13, 8, 39, MAType.SKDJ, 0.050, -0.060, 0.144, 0.00117, -0.013),
        SymbolData("sh510230", "金融ETF", 240, 1, 35, 150, 0, MAType.OBV, 0.100, -0.020, 0.075, 0.00116, -0.098),
        SymbolData("sh516860", "金融科技ETF", 240, 1, 53, 2, 4, MAType.SKDJ, 0.080, 0.000, 0.161, 0.00435, -0.042),
        SymbolData("sh512010", "医药ETF", 240, 5, 26, 51, 5, MAType.MACD, 0.030, -0.060, 0.126, 0.00090, -0.047),
        SymbolData("sz159766", "旅游ETF", 240, 5, 16, 36, 11, MAType.MACD, 0.040, -0.100, 0.116, 0.00152, -0.057),
        SymbolData("sh588790", "科创AIETF", 240, 1, 2, 2, 54, MAType.SKDJ, 0.000, -0.040, 0.351, 0.00339, -0.061),
        SymbolData("sh513310", "中韩半导体ETF", 240, 1, 1, 20, 0, MAType.SMA, 0.040, -0.020, 0.252, 0.00205, -0.034),
        SymbolData("sh588220", "科创100ETF基金", 240, 1, 13, 3, 49, MAType.SKDJ, 0.090, -0.080, 0.210, 0.00242, -0.052),
        SymbolData("sh588000", "科创50ETF", 240, 1, 60, 80, 0, MAType.OBV, 0.170, 0.000, 0.162, 0.00149, -0.038),
        SymbolData("sz159755", "电池ETF", 240, 1, 46, 56, 5, MAType.MACD, 0.090, 0.000, 0.156, 0.00267, -0.097),
        SymbolData("sh513090", "香港证券ETF", 240, 1, 48, 2, 3, MAType.SKDJ, 0.090, -0.040, 0.201, 0.00256, -0.062),
        SymbolData("sh562500", "机器人ETF", 240, 1, 48, 3, 9, MAType.SKDJ, 0.100, 0.000, 0.187, 0.00307, -0.041),
        SymbolData("sz159915", "易方达创业板ETF", 240, 5, 3, 2, 44, MAType.SKDJ, 0.010, 0.000, 0.121, 0.00084, -0.082),
        SymbolData("sh515050", "5G通信ETF", 240, 1, 18, 8, 19, MAType.SKDJ, 0.010, -0.100, 0.249, 0.00149, -0.112),
        SymbolData("sz159201", "华夏国证自由现金流ETF", 240, 1, 42, 57, 62, MAType.RSI, 0.000, 0.000, 0.144, 0.00183, 0.000),
        SymbolData("sh512890", "红利低波ETF", 240, 5, 12, 47, 72, MAType.RSI, 0.000, 0.000, 0.152, 0.00089, -0.069),
        SymbolData("sh515100", "红利低波100ETF", 240, 1, 3, 8, 3, MAType.SKDJ, 0.070, -0.100, 0.109, 0.00067, -0.093),
        SymbolData("sh515450", "红利低波50ETF", 240, 1, 8, 2, 59, MAType.SKDJ, 0.090, -0.090, 0.102, 0.00063, -0.061),
        SymbolData("sh513820", "港股红利ETF", 240, 1, 2, 2, 54, MAType.SKDJ, 0.100, -0.070, 0.129, 0.00139, -0.027),
        SymbolData("sz159545", "恒生红利低波ETF", 240, 1, 43, 2, 4, MAType.SKDJ, 0.030, -0.040, 0.150, 0.00164, -0.025),
        SymbolData("sh513130", "恒生科技ETF", 240, 1, 13, 23, 3, MAType.SKDJ, 0.060, 0.000, 0.139, 0.00166, -0.048),
        SymbolData("sz159892", "恒生医药ETF", 240, 5, 43, 2, 3, MAType.SKDJ, 0.060, 0.000, 0.146, 0.00205, -0.038),
        SymbolData("sz159941", "纳指ETF广发", 240, 1, 58, 8, 9, MAType.SKDJ, 0.000, -0.030, 0.189, 0.00088, -0.059),
        SymbolData("sh518880", "黄金ETF", 240, 1, 22, 62, 77, MAType.RSI, 0.000, 0.000, 0.143, 0.00046, -0.127),
    )

    @Test
    fun main() = runBlocking {
        // val symbol = symbols.first()
        val symbol = symbols.find { it.code == "" || it.desc == "沪深300ETF" }!!

        // calculateBestSingleArgs(symbol)
        // calculateSpecificArg(symbol)
        // queryTradeSignal(symbols.filter { it.maType== MAType.OBV })

        // calculateSpecificArg(SymbolData("sz159892", "恒生医药ETF", 240, 1, 1, 40, 0, MAType.OBV, 0.020, 0.000, 0.328, 0.00426, -0.007))
        // queryTradeSignal(listOf(SymbolData(code="sh513130",desc="恒生科技ETF",scale=240,d=1,shortMA=13,longMA=23,extN=3,maType=MAType.SKDJ,upCrossDiffRate=0.060,downCrossDiffRate=0.000,yearlyPercentage=0.139,dailyPercentage=0.00166,mdd=-0.048)))

        // fetchJsonFile()

        printBasicInfo()
    }

    /**
     * 根据提供的参数组合列表，使用协程并行计算最优的交易策略参数
     *
     * @param symbol 要计算的股票或ETF信息
     * @param argsList 包含多个CalculationArgs对象的列表，每个对象定义了一组要测试的参数范围
     */
    private suspend fun calculateBestArgs(symbol: SymbolData, argsList: List<CalculationArgs>) = coroutineScope {
        val scale = 240 // K线数量

        // 1. 预先获取所有需要的K线数据，避免在协程中重复请求
        val allDs = argsList.flatMap { it.d }.distinct()
        val kLineDataMap = allDs.associateWith { d ->
            val kLineSymbol = symbol.copy(d = d)
            Utils.getSinaKLineData(kLineSymbol, findBestData = false, useLocalData = true, datalen = 10000)
        }

        // 2. 生成所有待计算的参数组合
        val allParamCombinations = mutableListOf<Pair<CalculationArgs, SymbolData>>()
        argsList.forEach { args ->
            args.d.forEach { d ->
                args.shortMAList.forEach { shortMA ->
                    args.longMAList.forEach { longMA ->
                        args.extNList.forEach { extN ->
                            args.upCrossDiffRateList.forEach { upCrossDiffRate ->
                                args.downCrossDiffRateList.forEach { downCrossDiffRate ->
                                    val symbol = symbol.copy(
                                        scale = scale,
                                        d = d,
                                        shortMA = shortMA,
                                        longMA = longMA,
                                        maType = args.maType,
                                        extN = extN,
                                        upCrossDiffRate = upCrossDiffRate,
                                        downCrossDiffRate = downCrossDiffRate
                                    )
                                    if (args.argsFilter(symbol)) {
                                        allParamCombinations.add(args to symbol)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (kLineDataMap[1]?.find { it.volume <= 0 } != null) {
            allParamCombinations.removeAll { it.second.maType == MAType.OBV } // 缺失成交量数据的OBV参数组合不参与计算
        }

        println("开始并行计算，总任务数: ${allParamCombinations.size}")
        val completedTasks = AtomicInteger(0)
        val percent = AtomicInteger(0)
        val totalTasks = allParamCombinations.size

        // 3. 分块并使用协程并行计算
        val results = allParamCombinations.chunked(1000).map { chunk ->
            async(Dispatchers.Default) { // 在默认线程池中执行计算密集型任务
                chunk.map { (args, currentSymbol) ->
                    val resultData = kLineDataMap[currentSymbol.d]?.let { kLineData ->
                        if (kLineData.isNotEmpty()) {
                            val filteredKLineData = args.kLineDataFilter(kLineData)
                            val result = MACrossUtils.calculateMACross(
                                symbol = currentSymbol,
                                kLineData = filteredKLineData,
                                useCache = true,
                            )
                            val symbolForLogging = currentSymbol.copy(
                                yearlyPercentage = result.yearlyPercentage,
                                dailyPercentage = result.totalCrossData.dailyPercentage,
                                mdd = result.maxDrawDownData.maxLossFromBuyRate
                            )
                            result to getArgStr(symbolForLogging, result)
                        } else {
                            null
                        }
                    }
                    val progress = completedTasks.incrementAndGet()
                    val newPercent = progress * 100 / totalTasks
                    if (newPercent > 0 && percent.compareAndSet(newPercent - 1, newPercent)) {
                        if (newPercent % 10 == 0) {
                            println("$progress / $totalTasks (${percent}%)")
                        }
                    }
                    resultData
                }
            }
        }.awaitAll().flatten().filterNotNull()

        // 4. 过滤掉不符合要求的结果
        val filteredList = results.toMutableList()
        filteredList.removeAll { it.first.yearlyPercentage < 0.04 } // 过滤掉平均年收益过低的结果
        filteredList.removeAll { it.first.totalCrossData.totalCount <= 3 } // 过滤掉交易次数过少的结果
        filteredList.removeAll { it.first.totalCrossData.totalCount / it.first.yearCrossDataMap.size > 5 } // 过滤掉交易次数过多的结果
        val filterByMdd = { start: Int, end: Int, size: Int ->
            filteredList.filter {
                val mdd = it.first.maxDrawDownData.maxLossFromBuyRate // 最大本金亏损 (为负数)
                (-mdd * 100).toInt() in start until end
            }.sortedByDescending {
                it.first.totalCrossData.totalPercentage // 总涨幅
            }.take(size)
        }

        println("\n\n计算完成，加权排序结果 (综合考虑收益率和本金损失)")

        val avgMaxPercentageList = filterByMdd(0, 15, 5)
        val avgMaxPercentage =
            Utils.getPercentageString(avgMaxPercentageList.sumOf { it.first.totalCrossData.totalPercentage } / avgMaxPercentageList.size)
        println("平均最大涨幅$avgMaxPercentage")

        val finalResultList = mutableListOf<Pair<MACrossResult, String>>()
        finalResultList.addAll(filterByMdd(0, 5, 5))
        finalResultList.addAll(filterByMdd(5, 10, 5))
        finalResultList.addAll(filterByMdd(10, 15, 5))
        finalResultList.sortedByDescending {
            val mdd = it.first.maxDrawDownData.maxLossFromBuyRate // 最大本金亏损 (为负数)
            it.first.totalCrossData.dailyPercentage * 240 + 3 * mdd
        }.forEach {
            println(
                "\n${it.second} \n${it.first.getTotalDesc()}"
                    + " \n${it.first.totalCrossData.crossDataList.joinToString("\n")}"
            )
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
                println("${it.code} ${it.desc} ${tradeSignalData.takeLast(3)}")
            }
    }

    private fun getArgStr(symbol: SymbolData, result: MACrossResult): String =
        "--- 参数：code=\"${symbol.code}\",desc=\"${symbol.desc}\",scale=${symbol.scale},d=${symbol.d},shortMA=${symbol.shortMA},longMA=${symbol.longMA},extN=${symbol.extN},maType=MAType.${symbol.maType}" +
            ",upCrossDiffRate=${String.format("%.3f", symbol.upCrossDiffRate)},downCrossDiffRate=${String.format("%.3f", symbol.downCrossDiffRate)}" +
            ",yearlyPercentage=${String.format("%.3f", symbol.yearlyPercentage)}" +
            ",dailyPercentage=${String.format("%.5f", symbol.dailyPercentage)}" +
            ",mdd=${String.format("%.3f", symbol.mdd)})," +
            "\n有效数据时间${result.alignedMAData.firstOrNull()?.kLineData?.date} - ${result.alignedMAData.lastOrNull()?.kLineData?.date}"

    private suspend fun calculateBestSingleArgs(symbol: SymbolData) {
        val filter = { kLineData: List<KLineData> ->
            kLineData.filterNot { it.date.split("-").first().toInt() < 2016 }
        }
        var argsList = listOf(
            CalculationArgs(
                maType = MAType.SMA,
                d = listOf(1, 5), // 日K和周K
                shortMAList = Utils.newList(listOf(1, 5), 60, 5),
                longMAList = Utils.newList(listOf(5), 240, 10),
                upCrossDiffRateList = Utils.newList(listOf(0.0), 0.2, 0.01),
                downCrossDiffRateList = Utils.newList(listOf(0.0), -0.2, -0.01),
                argsFilter = { it.shortMA < it.longMA },
                kLineDataFilter = filter,
            ),
            CalculationArgs(
                maType = MAType.EMA,
                d = listOf(1, 5),
                shortMAList = Utils.newList(listOf(1, 5), 60, 5),
                longMAList = Utils.newList(listOf(5), 240, 10),
                upCrossDiffRateList = Utils.newList(listOf(0.0), 0.2, 0.01),
                downCrossDiffRateList = Utils.newList(listOf(0.0), -0.2, -0.01),
                argsFilter = { it.shortMA < it.longMA },
                kLineDataFilter = filter,
            ),
            // OBV 的参数组合
            CalculationArgs(
                maType = MAType.OBV,
                d = listOf(1), // obv只能用日线，周线成交量有问题看不出来
                shortMAList = Utils.newList(listOf(1, 5), 60, 5),
                longMAList = Utils.newList(listOf(5), 240, 10),
                upCrossDiffRateList = Utils.newList(listOf(0.0), 0.2, 0.01),
                downCrossDiffRateList = Utils.newList(listOf(0.0), -0.2, -0.01),
                argsFilter = { it.shortMA < it.longMA },
                kLineDataFilter = { Utils.findLatestSublist(filter(it)) { it.volume > 0 } },
            ),
            // RSI 的参数组合
            CalculationArgs(
                maType = MAType.RSI,
                d = listOf(1, 5),
                shortMAList = Utils.newList(listOf(1), 100, 1),   // RSI单根线
                longMAList = Utils.newList(listOf(1), 100, 1),    // 超卖阈值
                extNList = Utils.newList(listOf(1), 100, 1), // 超买阈值
                argsFilter = { it.longMA < 80 && it.extN < 80 && it.longMA < it.extN },
                kLineDataFilter = filter,
            ),
            // MACD 的参数组合
            CalculationArgs(
                maType = MAType.MACD,
                d = listOf(1, 5),
                shortMAList = Utils.newList(listOf(5), 60, 1), //短周期
                longMAList = Utils.newList(listOf(5), 60, 1), //长周期
                extNList = Utils.newList(listOf(5), 60, 1), // DIF 平滑周期
                upCrossDiffRateList = Utils.newList(listOf(0.0), 0.1, 0.01),
                downCrossDiffRateList = Utils.newList(listOf(0.0), -0.1, -0.01),
                argsFilter = { it.shortMA < it.longMA },
                kLineDataFilter = filter,
            ),
            // SKDJ 的参数组合
            CalculationArgs(
                maType = MAType.SKDJ,
                d = listOf(1, 5),
                extNList = Utils.newList(listOf(3), 60, 1), // RSV 计算周期
                shortMAList = Utils.newList(listOf(2), 60, 1), // K 值平滑周期
                longMAList = Utils.newList(listOf(2), 60, 1), // D 值平滑周期
                upCrossDiffRateList = Utils.newList(listOf(0.0), 0.1, 0.01),
                downCrossDiffRateList = Utils.newList(listOf(0.0), -0.1, -0.01),
                argsFilter = { true },
                kLineDataFilter = filter,
            )
        )
        // argsList = argsList.filter { it.maType == MAType.SMA }
        // argsList = argsList.filterNot { it.maType == MAType.OBV }
        calculateBestArgs(symbol, argsList)
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
            useCache = true,
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
        symbols.sortedByDescending { it.dailyPercentage }.forEach {
            println(
                "${it.code} ${it.desc}" +
                    " yearlyPercentage:${Utils.getPercentageString(it.yearlyPercentage)}" +
                    " dailyPercentage:${Utils.getPercentageString(it.dailyPercentage)}"
            )
        }

        println("\n=== Group Size ===")
        symbols.groupBy { it.d }.forEach { (d, symbols) -> println("d=$d ${symbols.size}") }
        symbols.groupBy { it.maType }.forEach { (maType, symbols) -> println("$maType ${symbols.size}") }

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