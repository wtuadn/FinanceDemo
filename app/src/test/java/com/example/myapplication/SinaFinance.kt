package com.example.myapplication

import com.example.myapplication.data.MACrossResult
import com.example.myapplication.data.SymbolData
import com.example.myapplication.utils.MACrossUtils
import com.example.myapplication.utils.MACrossUtils.MAType
import com.example.myapplication.utils.Utils
import org.junit.Test
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.random.Random

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 * 651ED65C-9613-4143-8A5F-8DB6E1D235B6
 */
class SinaFinance {

    @Test
    fun main() {
        val symbols = listOf(
            // SymbolData("sh513310", "中韩半导体ETF", 240, 5, 1, 10, MAType.SMA, 0.130, -0.020, 0.199, 0.0),
            // SymbolData("sh588220", "科创100ETF基金", 240, 5, 1, 5, MAType.EMA, 0.060, 0.000, 0.151, 0.0),
            // SymbolData("sh588000", "科创50ETF", 240, 5, 1, 5, MAType.EMA, 0.160, 0.000, 0.073, 0.0),
            // SymbolData("sz159755", "电池ETF", 240, 1, 1, 20, MAType.EMA, 0.160, 0.000, 0.167, 0.0),
            // SymbolData("sh513090", "香港证卷ETF", 240, 5, 1, 15, MAType.SMA, 0.190, -0.010, 0.211, 0.0),
            SymbolData("sh562500", "机器人ETF", 240, 5, 1, 5, MAType.SMA, 0.080, -0.010, 0.071, -0.019),
            // SymbolData("sz159915", "易方达创业板ETF", 240, 1, 1, 5, MAType.EMA, 0.200, -0.030),
            // SymbolData("sh515050", "5G通信ETF", 240, 1, 20, 25, MAType.SMA, 0.190, 0.000),
            // SymbolData("sh513820", "港股红利ETF", 240, 1, 5, 10, MAType.SMA, 0.02, -0.01),
            // SymbolData("sh515100", "红利低波100ETF", 240, 5, 1, 25, MAType.SMA, 0.020, 0.000),
            // SymbolData("sz159201", "华夏国证自由现金流", 240, 1, 1, 10, MAType.EMA, 0.000, -0.020),
            // SymbolData("sz159545", "恒生红利低波ETF", 240, 1, 10, 15, MAType.SMA, 0.010, 0.000),
            // SymbolData("sh513130", "恒生科技ETF", 240, 1, 1, 15, MAType.SMA, 0.080, -0.060),
            // SymbolData("sz159892", "恒生医药ETF", 240, 5, 1, 5, MAType.SMA, 0.100, -0.090),
            // SymbolData("sz159941", "纳指ETF广发", 240, 1, 20, 60, MAType.SMA, 0.010, -0.080),
            // SymbolData("sh518880", "黄金ETF", 240, 5, 5, 15, MAType.SMA, 0.080, 0.000),
        )

        // queraTradeSignal(symbols)
        calculateBestMAArgs(symbols.first())
        // calculateSpecificMAArg(symbols.first())
        // calculateSpecificMAArg(SymbolData(code="sh513310",desc="中韩半导体ETF",scale=240,d=5,shortMA=5,longMA=10,maType=MAType.SMA,upCrossDiffRate=0.110,downCrossDiffRate=0.000))

        println()
    }

    private fun queraTradeSignal(symbols: List<SymbolData>) {
        symbols.forEach {
            val tradeSignalData = MACrossUtils.getTradeSignal(it)
            println(tradeSignalData)
            Thread.sleep(Random.nextLong(100, 500))
        }
    }

    private fun calculateBestMAArgs(symbol: SymbolData) {
        val list = mutableListOf<Pair<MACrossResult, String>>()
        val scale = 240
        listOf(1, 5).forEach { d ->
            symbol.d = d
            var kLineData = Utils.getSinaKLineData(symbol, findBestData = true, useLocalData = true, datalen = 10000)
            kLineData = kLineData.filterNot { it.date.split("-").first().toInt() < 2016 }
            println("--- scale=$scale d=$d 有效数据时间段为：${kLineData.firstOrNull()?.date} - ${kLineData.lastOrNull()?.date} ---\n")
            listOf(MAType.SMA, MAType.EMA).forEach { maType ->
                listOf(1, 5, 10, 15, 20, 25, 30).forEach { shortMA ->
                    listOf(5, 10, 15, 20, 25, 30, 40, 60, 120, 180).forEach { longMA ->
                        if (shortMA >= longMA) return@forEach

                        val start = 0.0
                        val end = 0.2001
                        val step = 0.01
                        var upCrossDiffRate = start
                        var downCrossDiffRate = start
                        // 使用 while 循环进行浮点数步进迭代
                        while (upCrossDiffRate <= end) {
                            while (downCrossDiffRate >= -end) {
                                val result = MACrossUtils.calculateMACross(
                                    symbol = symbol.also {
                                        it.scale = scale
                                        it.d = d
                                        it.shortMA = shortMA
                                        it.longMA = longMA
                                        it.maType = maType
                                        it.upCrossDiffRate = upCrossDiffRate
                                        it.downCrossDiffRate = downCrossDiffRate
                                    },
                                    kLineData = kLineData,
                                )
                                symbol.apply {
                                    countlyPercentage = result.totalCrossData.countlyPercentage
                                    mdd = result.maxDrawDownData.maxLossFromBuyRate
                                }
                                list.add(result to getArgStr(symbol))
                                downCrossDiffRate -= step
                            }
                            upCrossDiffRate += step
                            downCrossDiffRate = start
                        }
                    }
                }
            }
        }
        list.removeAll { it.first.totalCrossData.totalCount < 2 } //过滤掉操作次数太少的
        list.removeAll { it.first.totalCrossData.totalCount == 0 || it.first.totalCrossData.totalPercentage <= 0.0 }
        println("\n\n最小本金损失优先")
        repeat(2) { println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---") }
        list.sortedWith(
            compareByDescending<Pair<MACrossResult, String>> {
                it.first.maxDrawDownData.maxLossFromBuyRate
            }.thenByDescending {
                it.first.totalCrossData.totalPercentage
            }
        ).subList(0, 20.coerceAtMost(list.size))
            .forEach {
                println("\n${it.second} \n${it.first.getTotalDesc()} \n${it.first.totalCrossData.crossDataList.joinToString("\n")}")
            }
        println("\n\n收益优先")
        repeat(10) { println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---") }
        list.sortedByDescending { it.first.totalCrossData.totalPercentage }
            .subList(0, 10.coerceAtMost(list.size))
            .forEach {
                println("\n${it.second} \n${it.first}")
            }
    }

    private fun getArgStr(symbol: SymbolData): String =
        "--- 参数：code=\"${symbol.code}\",desc=\"${symbol.desc}\",scale=${symbol.scale},d=${symbol.d},shortMA=${symbol.shortMA},longMA=${symbol.longMA},maType=MAType.${symbol.maType}" +
            ",upCrossDiffRate=${String.format("%.3f", symbol.upCrossDiffRate)},downCrossDiffRate=${
                String.format(
                    "%.3f",
                    symbol.downCrossDiffRate
                )
            },countlyPercentage=${String.format("%.3f", symbol.countlyPercentage)},mdd=${String.format("%.3f", symbol.mdd)}),"

    private fun calculateSpecificMAArg(
        symbol: SymbolData,
    ) {
        val result = MACrossUtils.calculateMACross(
            symbol = symbol,
            kLineData = Utils.getSinaKLineData(symbol, findBestData = true, useLocalData = true, datalen = 10000),
        )
        println("\n${getArgStr(symbol)} \n${result.getTotalDesc()} \n${result.totalCrossData.crossDataList.joinToString("\n")}")
    }
    // --- 参数：code="sh513310",desc="中韩半导体ETF",scale=240,d=5,shortMA=1,longMA=10,maType=MAType.SMA,upCrossDiffRate=0.130,downCrossDiffRate=-0.020,countlyPercentage=0.199,mdd=0.0),
    // total: 平均次涨幅19.92% 平均年涨幅13.28% 总次数: 2 胜率: 100.00% 涨幅: 39.84% 单次最大涨幅: 30.14% 单次最大回撤: 0.00%
    // 最大回撤:-15.11% 持续天数:21 2025-10-31 - 2025-11-21 修复时间:修复中
    // 最大本金损失率:0.00% 修复时间:修复中
    // 2024-10-11——2025-03-28 涨幅: 9.70%
    // 2025-08-15——2025-11-21 涨幅: 30.14%

    // --- 参数：code="sh588220",desc="科创100ETF基金",scale=240,d=5,shortMA=1,longMA=5,maType=MAType.EMA,upCrossDiffRate=0.060,downCrossDiffRate=0.000,countlyPercentage=0.151,mdd=0.0),
    // total: 平均次涨幅15.13% 平均年涨幅22.69% 总次数: 3 胜率: 100.00% 涨幅: 45.38% 单次最大涨幅: 21.90% 单次最大回撤: 0.00%
    // 最大回撤:-9.86% 持续天数:17 2025-09-30 - 2025-10-17 修复时间:修复中
    // 最大本金损失率:0.00% 修复时间:修复中
    // 2024-09-27——2024-11-22 涨幅: 20.26%
    // 2025-02-07——2025-03-21 涨幅: 3.21%
    // 2025-07-18——2025-10-17 涨幅: 21.90%

    // --- 参数：code="sh588000",desc="科创50ETF",scale=240,d=5,shortMA=1,longMA=5,maType=MAType.EMA,upCrossDiffRate=0.160,downCrossDiffRate=0.000,countlyPercentage=0.073,mdd=0.0),
    // total: 平均次涨幅7.31% 平均年涨幅4.39% 总次数: 3 胜率: 100.00% 涨幅: 21.93% 单次最大涨幅: 9.68% 单次最大回撤: 0.00%
    // 最大回撤:-8.73% 持续天数:17 2025-09-30 - 2025-10-17 修复时间:修复中
    // 最大本金损失率:0.00% 修复时间:修复中
    // 2021-06-18——2021-08-13 涨幅: 3.94%
    // 2024-09-30——2024-12-13 涨幅: 9.68%
    // 2025-08-22——2025-10-17 涨幅: 8.31%

    // --- 参数：code="sz159755",desc="电池ETF",scale=240,d=1,shortMA=1,longMA=20,maType=MAType.EMA,upCrossDiffRate=0.160,downCrossDiffRate=0.000,countlyPercentage=0.167,mdd=0.0),
    // total: 平均次涨幅16.74% 平均年涨幅12.55% 总次数: 3 胜率: 100.00% 涨幅: 50.21% 单次最大涨幅: 18.11% 单次最大回撤: 0.00%
    // 最大回撤:-16.53% 持续天数:9 2024-10-08 - 2024-10-17 修复时间:25 2024-10-17 - 2024-11-11
    // 最大本金损失率:0.00% 修复时间:修复中
    // 2022-06-02——2022-07-11 涨幅: 16.27%
    // 2024-09-27——2024-11-22 涨幅: 15.83%
    // 2025-08-29——2025-10-17 涨幅: 18.11%

    // --- 参数：code="sh513090",desc="香港证卷ETF",scale=240,d=5,shortMA=1,longMA=15,maType=MAType.SMA,upCrossDiffRate=0.190,downCrossDiffRate=-0.010,countlyPercentage=0.211,mdd=0.0),
    // total: 平均次涨幅21.06% 平均年涨幅7.02% 总次数: 2 胜率: 100.00% 涨幅: 42.12% 单次最大涨幅: 21.31% 单次最大回撤: 0.00%
    // 最大回撤:-16.62% 持续天数:56 2024-11-08 - 2025-01-03 修复时间:203 2025-01-03 - 2025-07-25
    // 最大本金损失率:0.00% 修复时间:修复中
    // 2024-09-27——2025-01-03 涨幅: 20.81%
    // 2025-06-27——2025-10-17 涨幅: 21.31%

    // --- 参数：code="sh562500",desc="机器人ETF",scale=240,d=5,shortMA=1,longMA=5,maType=MAType.SMA,upCrossDiffRate=0.080,downCrossDiffRate=-0.010,countlyPercentage=0.071,mdd=-0.019),
    // total: 平均次涨幅7.06% 平均年涨幅10.60% 总次数: 6 胜率: 66.67% 涨幅: 42.39% 单次最大涨幅: 25.16% 单次最大回撤: -1.88%
    // 最大回撤:-10.61% 持续天数:17 2025-09-30 - 2025-10-17 修复时间:修复中
    // 最大本金损失率:-1.88% 修复时间:364 2023-03-10 - 2024-03-08
    // 2022-06-02——2022-08-26 涨幅: 6.73%
    // 2023-02-03——2023-03-10 涨幅: -1.88%
    // 2024-03-01——2024-03-29 涨幅: -0.96%
    // 2024-09-27——2024-12-27 涨幅: 25.16%
    // 2025-02-07——2025-03-21 涨幅: 4.67%
    // 2025-07-25——2025-10-17 涨幅: 8.68%
    @Test
    fun fetchJsonFile() {
        val symbol = listOf(
            "sh513310",
            "sh588220",
            "sh588000",
            "sz159755",
            "sh513090",
            "sh562500",
            "sz159915",
            "sh515050",
            "sh513820",
            "sh515100",
            "sz159201",
            "sz159545",
            "sh513130",
            "sz159892",
            "sz159941",
            "sh518880",
        )
        val scale = 240
        val d = listOf(1, 5)
        symbol.forEach { symbol ->
            d.forEach { d ->
                val api =
                    "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=$symbol&scale=${scale * d}&ma=no&datalen=10000"
                val json = Utils.httpGet(
                    urlString = api,
                    headMap = mapOf(
                        "Referer" to "https://finance.sina.com.cn",
                        "host" to "hq.sinajs.cn",
                    )
                )
                if (json.isNullOrEmpty()) return@forEach

                val file = File("data", "$symbol.$d.json")
                file.parentFile.mkdirs()
                BufferedWriter(FileWriter(file)).use { writer ->
                    writer.write(json)
                    writer.flush()
                }
                Thread.sleep(Random.nextLong(300, 800))
            }
        }
    }
}