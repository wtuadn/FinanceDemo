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
import kotlin.random.Random

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 * 651ED65C-9613-4143-8A5F-8DB6E1D235B6
 */
class SinaFinance {
    private val symbols = listOf(
        SymbolData("sh563360", "A500ETF", 240, 5, 27, 72, 67, MAType.RSI, 0.000, 0.000, 0.179, 0.00161, -0.023),
        SymbolData("sh513500", "标普500ETF", 240, 5, 18, 13, 44, MAType.SKDJ, 0.020, -0.010, 0.096, 0.00060, -0.009),
        SymbolData("sh510500", "中证500ETF", 240, 5, 67, 42, 42, MAType.RSI, 0.000, 0.000, 0.048, 0.00574, -0.009),
        SymbolData("sh510050", "上证50ETF", 240, 5, 31, 36, 21, MAType.MACD, 0.100, 0.000, 0.056, 0.00057, -0.049),
        SymbolData("sh510300", "沪深300ETF", 240, 1, 28, 33, 24, MAType.SKDJ, 0.080, -0.050, 0.074, 0.00057, -0.071),
        SymbolData("sh512040", "价值100ETF", 240, 1, 5, 25, 0, MAType.OBV, 0.200, -0.030, 0.119, 0.00092, -0.090),
        SymbolData("sz159883", "医疗器械ETF", 240, 5, 12, 27, 27, MAType.RSI, 0.000, 0.000, 0.067, 0.00733, 0.000),
        SymbolData("sz159928", "消费ETF", 240, 5, 41, 56, 6, MAType.MACD, 0.060, -0.050, 0.169, 0.00144, -0.007),
        SymbolData("sh512980", "传媒ETF", 240, 5, 22, 32, 57, MAType.RSI, 0.000, 0.000, 0.105, 0.00245, 0.000),
        SymbolData("sz159869", "游戏ETF", 240, 5, 2, 18, 3, MAType.SKDJ, 0.050, 0.000, 0.203, 0.00243, 0.000),
        SymbolData("sz159852", "软件ETF", 240, 1, 1, 5, 0, MAType.EMA, 0.070, -0.030, 0.180, 0.00265, -0.046),
        SymbolData("sh516510", "云计算ETF", 240, 5, 1, 5, 0, MAType.SMA, 0.040, 0.000, 0.208, 0.00225, -0.038),
        SymbolData("sz159998", "计算机ETF", 240, 1, 7, 12, 82, MAType.RSI, 0.000, 0.000, 0.106, 0.00252, -0.038),
        SymbolData("sh515400", "大数据ETF", 240, 1, 1, 5, 0, MAType.EMA, 0.070, -0.030, 0.194, 0.00331, -0.030),
        SymbolData("sh601398", "工商银行", 240, 5, 58, 3, 9, MAType.SKDJ, 0.100, -0.030, 0.062, 0.00075, -0.005),
        SymbolData("sh600036", "招商银行", 240, 1, 15, 25, 0, MAType.SMA, 0.060, -0.060, 0.175, 0.00087, -0.034),
        SymbolData("sh513120", "港股创新药ETF", 240, 5, 58, 2, 4, MAType.SKDJ, 0.000, 0.000, 0.272, 0.00234, -0.046),
        SymbolData("sh515790", "光伏ETF", 240, 1, 30, 35, 0, MAType.SMA, 0.040, 0.000, 0.180, 0.00249, -0.028),
        SymbolData("sh513550", "港股通50ETF", 240, 5, 21, 36, 26, MAType.MACD, 0.090, -0.050, 0.130, 0.00143, 0.000),
        SymbolData("sh512710", "军工龙头ETF", 240, 5, 7, 17, 72, MAType.RSI, 0.000, 0.000, 0.111, 0.00159, -0.042),
        SymbolData("sz159227", "航空航天ETF", 240, 5, 7, 87, 87, MAType.RSI, 0.000, 0.000, 0.290, 0.00360, -0.011),
        SymbolData("sz159218", "卫星产业ETF", 240, 1, 12, 87, 82, MAType.RSI, 0.000, 0.000, 0.419, 0.00413, -0.015),
        SymbolData("sz159813", "半导体ETF", 240, 5, 2, 3, 39, MAType.SKDJ, 0.000, -0.010, 0.147, 0.00116, -0.082),
        SymbolData("sz159713", "稀土ETF", 240, 5, 43, 2, 4, MAType.SKDJ, 0.000, 0.000, 0.178, 0.00189, -0.058),
        SymbolData("sz159985", "豆粕ETF", 240, 5, 53, 2, 4, MAType.SKDJ, 0.000, -0.040, 0.142, 0.00070, -0.037),
        SymbolData("sh561330", "矿业ETF", 240, 1, 30, 200, 0, MAType.OBV, 0.000, -0.190, 0.371, 0.00404, -0.016),
        SymbolData("sh513400", "道琼斯ETF", 240, 1, 13, 8, 44, MAType.SKDJ, 0.080, -0.060, 0.156, 0.00118, -0.009),
        SymbolData("sh510230", "金融ETF", 240, 5, 7, 27, 57, MAType.RSI, 0.000, 0.000, 0.059, 0.00094, -0.049),
        SymbolData("sz159851", "金融科技ETF", 240, 1, 58, 2, 4, MAType.SKDJ, 0.070, 0.000, 0.152, 0.00482, -0.044),
        SymbolData("sh516860", "金融科技ETF", 240, 1, 1, 35, 0, MAType.EMA, 0.100, -0.010, 0.165, 0.00298, -0.035),
        SymbolData("sh512010", "医药ETF", 240, 5, 26, 51, 5, MAType.MACD, 0.030, -0.060, 0.126, 0.00090, -0.047),
        SymbolData("sz159766", "旅游ETF", 240, 5, 16, 36, 11, MAType.MACD, 0.040, -0.100, 0.116, 0.00152, -0.057),
        SymbolData("sh588790", "科创AIETF", 240, 1, 5, 16, 26, MAType.MACD, 0.040, 0.000, 0.297, 0.00640, 0.000),
        SymbolData("sh513310", "中韩半导体ETF", 240, 1, 12, 42, 82, MAType.RSI, 0.000, 0.000, 0.441, 0.00180, -0.058),
        SymbolData("sh588220", "科创100ETF基金", 240, 5, 47, 62, 57, MAType.RSI, 0.000, 0.000, 0.343, 0.00248, -0.069),
        SymbolData("sh588000", "科创50ETF", 240, 1, 28, 8, 4, MAType.SKDJ, 0.090, -0.010, 0.104, 0.00126, -0.044),
        SymbolData("sz159755", "电池ETF", 240, 1, 5, 30, 0, MAType.EMA, 0.020, 0.000, 0.135, 0.00155, -0.047),
        SymbolData("sh513090", "香港证券ETF", 240, 1, 1, 5, 0, MAType.EMA, 0.130, -0.020, 0.112, 0.00537, -0.029),
        SymbolData("sh562500", "机器人ETF", 240, 1, 48, 3, 9, MAType.SKDJ, 0.100, 0.000, 0.187, 0.00307, -0.041),
        SymbolData("sz159915", "易方达创业板ETF", 240, 5, 40, 50, 0, MAType.SMA, 0.110, 0.000, 0.090, 0.00108, 0.000),
        SymbolData("sh515050", "5G通信ETF", 240, 1, 8, 43, 3, MAType.SKDJ, 0.030, 0.000, 0.127, 0.00128, -0.041),
        SymbolData("sz159201", "华夏国证自由现金流ETF", 240, 1, 42, 57, 62, MAType.RSI, 0.000, 0.000, 0.144, 0.00183, 0.000),
        SymbolData("sh512890", "红利低波ETF", 240, 1, 47, 47, 62, MAType.RSI, 0.000, 0.000, 0.154, 0.00089, -0.076),
        SymbolData("sh515100", "红利低波100ETF", 240, 5, 26, 46, 11, MAType.MACD, 0.000, -0.090, 0.092, 0.00058, -0.025),
        SymbolData("sh515450", "红利低波50ETF", 240, 1, 62, 42, 57, MAType.RSI, 0.000, 0.000, 0.071, 0.00188, -0.013),
        SymbolData("sh513820", "港股红利ETF", 240, 1, 77, 52, 52, MAType.RSI, 0.000, 0.000, 0.172, 0.00312, -0.051),
        SymbolData("sz159545", "恒生红利低波ETF", 240, 1, 48, 2, 4, MAType.SKDJ, 0.020, -0.020, 0.145, 0.00172, -0.014),
        SymbolData("sh513130", "恒生科技ETF", 240, 5, 26, 31, 56, MAType.MACD, 0.020, -0.080, 0.180, 0.00217, -0.024),
        SymbolData("sz159892", "恒生医药ETF", 240, 1, 1, 40, 0, MAType.OBV, 0.020, 0.000, 0.328, 0.00426, -0.007),
        SymbolData("sz159941", "纳指ETF广发", 240, 1, 77, 42, 62, MAType.RSI, 0.000, 0.000, 0.173, 0.00149, -0.015),
        SymbolData("sh518880", "黄金ETF", 240, 1, 72, 57, 72, MAType.RSI, 0.000, 0.000, 0.185, 0.00061, 0.000),
    )

    @Test
    fun main() = runBlocking {
        val symbol = symbols.first()

        // calculateBestSingleArgs(symbol)
        symbols.forEach { calculateSpecificArg(it) }

        // calculateSpecificArg(SymbolData(code="sh510050",desc="上证50ETF",scale=240,d=5,shortMA=15,longMA=105,extN=0,maType=MAType.OBV,upCrossDiffRate=0.000,downCrossDiffRate=-0.050,yearlyPercentage=0.219,dailyPercentage=0.00051,mdd=-0.006))

        Utils.printHeapUsage()
    }

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
                filter = filter,
            ),
            CalculationArgs(
                maType = MAType.EMA,
                d = listOf(1, 5),
                shortMAList = Utils.newList(listOf(1, 5), 60, 5),
                longMAList = Utils.newList(listOf(5), 240, 10),
                upCrossDiffRateList = Utils.newList(listOf(0.0), 0.2, 0.01),
                downCrossDiffRateList = Utils.newList(listOf(0.0), -0.2, -0.01),
                filter = filter,
            ),
            // OBV 的参数组合
            CalculationArgs(
                maType = MAType.OBV,
                d = listOf(1), // obv只能用日线，周线成交量有问题看不出来
                shortMAList = Utils.newList(listOf(1, 5), 60, 5),
                longMAList = Utils.newList(listOf(5), 240, 10),
                upCrossDiffRateList = Utils.newList(listOf(0.0), 0.2, 0.01),
                downCrossDiffRateList = Utils.newList(listOf(0.0), -0.2, -0.01),
                filter = { Utils.findLatestSublist(filter(it)) { it.volume > 0 } },
            ),
            // RSI 的参数组合
            CalculationArgs(
                maType = MAType.RSI,
                d = listOf(1, 5),
                shortMAList = Utils.newList(listOf(1), 100, 1),   // RSI单根线
                longMAList = Utils.newList(listOf(1), 100, 1),    // 超卖阈值
                extNList = Utils.newList(listOf(1), 100, 1), // 超买阈值
                ignoreShortOverLong = true,
                filter = filter,
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
                filter = filter,
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
                ignoreShortOverLong = true,
                filter = filter,
            )
        )
        // argsList = argsList.filter { it.maType == MAType.SMA }
        // argsList = argsList.filterNot { it.maType == MAType.OBV }
        calculateBestArgs(symbol, argsList)
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
                        if (!args.ignoreShortOverLong && shortMA >= longMA) return@forEach
                        args.extNList.forEach { extN ->
                            args.upCrossDiffRateList.forEach { upCrossDiffRate ->
                                args.downCrossDiffRateList.forEach { downCrossDiffRate ->
                                    allParamCombinations.add(
                                        args to symbol.copy(
                                            scale = scale,
                                            d = d,
                                            shortMA = shortMA,
                                            longMA = longMA,
                                            maType = args.maType,
                                            extN = extN,
                                            upCrossDiffRate = upCrossDiffRate,
                                            downCrossDiffRate = downCrossDiffRate
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
                            val filteredKLineData = args.filter(kLineData)
                            val result = MACrossUtils.calculateMACross(
                                symbol = currentSymbol,
                                kLineData = filteredKLineData,
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
        filteredList.removeAll { it.first.yearlyPercentage < 0.03 } // 过滤掉平均年收益过低的结果
        filteredList.removeAll { it.first.totalCrossData.totalCount <= 2 } // 过滤掉交易次数过少的结果
        filteredList.removeAll { it.first.totalCrossData.totalCount / it.first.yearCrossDataMap.size > 5 } // 过滤掉交易次数过多的结果
        // filteredList.removeAll { it.first.totalCrossData.totalCount == 0 || it.first.totalCrossData.totalPercentage <= 0.0 } // 过滤掉没有交易或总收益为负的结果

        println("\n\n计算完成，加权排序结果 (综合考虑收益率和本金损失)")

        println("\n\n总涨幅优先")
        repeat(5) { println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---") }
        filteredList.sortedByDescending {
            it.first.totalCrossData.totalPercentage
        }.take(3).forEach {
            println(
                "\n${it.second} \n${it.first.getTotalDesc()}"
                    + " \n${it.first.totalCrossData.crossDataList.joinToString("\n")}"
            )
        }
        println("\n\n最小本金损失优先 0-2%")
        repeat(5) { println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---") }
        filteredList.filter {
            val mdd = it.first.maxDrawDownData.maxLossFromBuyRate // 最大本金亏损 (为负数)
            (-mdd * 100).toInt() in 0 until 2
        }.sortedByDescending {
            it.first.totalCrossData.totalPercentage
        }.take(5).forEach {
            println(
                "\n${it.second} \n${it.first.getTotalDesc()}"
                    + " \n${it.first.totalCrossData.crossDataList.joinToString("\n")}"
            )
        }
        println("\n\n最小本金损失优先 < 2-5%")
        repeat(5) { println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---") }
        filteredList.filter {
            val mdd = it.first.maxDrawDownData.maxLossFromBuyRate // 最大本金亏损 (为负数)
            (-mdd * 100).toInt() in 2 until 5
        }.sortedByDescending {
            it.first.totalCrossData.totalPercentage
        }.take(5).forEach {
            println(
                "\n${it.second} \n${it.first.getTotalDesc()}"
                    + " \n${it.first.totalCrossData.crossDataList.joinToString("\n")}"
            )
        }
        println("\n\n最小本金损失优先 < 5-10%")
        repeat(5) { println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---") }
        filteredList.filter {
            val mdd = it.first.maxDrawDownData.maxLossFromBuyRate // 最大本金亏损 (为负数)
            (-mdd * 100).toInt() in 5 until 10
        }.sortedByDescending {
            it.first.totalCrossData.totalPercentage
        }.take(5).forEach {
            println(
                "\n${it.second} \n${it.first.getTotalDesc()}"
                    + " \n${it.first.totalCrossData.crossDataList.joinToString("\n")}"
            )
        }
    }

    private fun queryTradeSignal(symbols: List<SymbolData>) {
        symbols
            .find { it.code == "sz159915" }
            ?.also {
                val tradeSignalData = MACrossUtils.getTradeSignal(it)
                println("${it.code} ${it.desc} ${tradeSignalData}")
                Thread.sleep(Random.nextLong(100, 500))
            }
    }

    private fun getArgStr(symbol: SymbolData, result: MACrossResult): String =
        "--- 参数：code=\"${symbol.code}\",desc=\"${symbol.desc}\",scale=${symbol.scale},d=${symbol.d},shortMA=${symbol.shortMA},longMA=${symbol.longMA},extN=${symbol.extN},maType=MAType.${symbol.maType}" +
            ",upCrossDiffRate=${String.format("%.3f", symbol.upCrossDiffRate)},downCrossDiffRate=${String.format("%.3f", symbol.downCrossDiffRate)}" +
            ",yearlyPercentage=${String.format("%.3f", symbol.yearlyPercentage)}" +
            ",dailyPercentage=${String.format("%.5f", symbol.dailyPercentage)}" +
            ",mdd=${String.format("%.3f", symbol.mdd)}),"
    // + "\n有效数据时间${result.alignedMAData.firstOrNull()?.kLineData?.date} - ${result.alignedMAData.lastOrNull()?.kLineData?.date}"

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

    @Test
    fun fetchJsonFile() {
        val scale = 240
        val d = listOf(1, 5)
        symbols.subList(25, symbols.size).forEach { symbol ->
            // (listOf(SymbolData("sh512980", "传媒ETF", 240, 5, 1, 15, 0, MAType.EMA, 0.120, 0.000, 0.083, 0.0009, -0.018))).forEach { symbol ->
            d.forEach { d ->
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
                Thread.sleep(Random.nextLong(300, 800))
            }
        }
    }
}