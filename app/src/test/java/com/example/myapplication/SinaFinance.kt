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
            SymbolData("sz159915", "易方达创业板ETF", 240, 1, 1, 25, MAType.SMA, 0.04, 0.0),
            // SymbolData("sh515050", "5G通信ETF", 240, 1, 1, 20, MAType.SMA, 0.01, 0.0),
            // SymbolData("sh513820", "港股红利ETF", 240, 1, 5, 10, MAType.SMA, 0.02, -0.01),
            // SymbolData("sh512890", "红利低波ETF", 240, 1, 1, 15, MAType.SMA, 0.0, -0.01),
            // SymbolData("sh515100", "红利低波100ETF", 240, 1, 10, 15, MAType.SMA, 0.0, 0.0),
            // SymbolData("sz159201", "华夏国证自由现金流", 240, 1, 1, 5, MAType.SMA, 0.0, -0.03),
            // SymbolData("sz159545", "恒生红利低波ETF", 240, 1, 5, 10, MAType.SMA, 0.01, 0.0),
            // SymbolData("sh513130", "恒生科技ETF", 240, 1, 5, 30, MAType.SMA, 0.01, 0.0),
            // SymbolData("sz159892", "恒生医药ETF", 240, 1, 10, 20, MAType.SMA, 0.01, -0.01),
            // SymbolData("sz159941", "纳指ETF广发", 240, 1, 20, 60, MAType.SMA, 0.01, -0.05),
            // SymbolData("sh518880", "黄金ETF", 240, 1, 10, 15, MAType.SMA, 0.0, 0.0),
        )

        // queraTradeSignal(symbols)
        calculateBestMAArgs(symbols.first())
        // calculateSpecificMAArg(symbol = symbols.first())

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
            var kLineData = Utils.getSinaKLineData(symbol, findBestData = true, useLocalData = true, datalen = 10000)
            // kLineData = kLineData.filterNot { it.date.split("-").first().toInt() < 2021 }
            println("--- scale=$scale d=$d 有效数据时间段为：${kLineData.firstOrNull()?.date} - ${kLineData.lastOrNull()?.date} ---\n")
            listOf(MAType.SMA/*, MA_TYPE.EMA*/).forEach { maType ->
                listOf(1, 5, 10, 15, 20, 25, 30).forEach { shortMA ->
                    listOf(5, 10, 15, 20, 25, 30, 40, 60, 120, 180).forEach { longMA ->
                        if (shortMA >= longMA) return@forEach

                        val start = 0.0
                        val end = 0.0501
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
                                val args =
                                    "--- 参数：code=\"${symbol.code}\",desc=\"${symbol.desc}\",scale=$scale,d=$d,shortMA=$shortMA,longMA=$longMA,maType=MAType.${maType}" +
                                        ",upCrossDiffRate=${upCrossDiffRate},downCrossDiffRate=${downCrossDiffRate}),"
                                list.add(result to args)
                                downCrossDiffRate -= step
                            }
                            upCrossDiffRate += step
                            downCrossDiffRate = start
                        }
                    }
                }
            }
        }
        // list.removeAll { it.first.totalCrossData.totalCount.toDouble() / it.first.yearCrossDataMap.size < 1 } //过滤掉操作次数太少的
        println("\n\n年收益优先")
        repeat(10) { println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---") }
        list.sortedByDescending { it.first.yearCrossDataMap.values.minOf { it.totalPercentage } }
            .subList(0, 10.coerceAtMost(list.size))
            .forEach {
                println("\n${it.second} \n${it.first}")
            }
        println("\n\n年年正收益优先")
        repeat(10) { println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---") }
        list.sortedWith(
            compareByDescending<Pair<MACrossResult, String>> {
                it.first.yearCrossDataMap.values.count { it.totalPercentage > 0 }
            }.thenByDescending {
                it.first.totalCrossData.totalPercentage
            }
        ).subList(0, 10.coerceAtMost(list.size))
            .forEach {
                println("\n${it.second} \n${it.first}")
            }
        println("\n\n收益优先")
        repeat(10) { println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---") }
        list.sortedByDescending { it.first.totalCrossData.totalPercentage }
            .subList(0, 10.coerceAtMost(list.size))
            .forEach {
                println("\n${it.second} \n${it.first}")
            }
        println("\n\n回撤优先")
        repeat(10) { println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---") }
        list.filter { it.first.totalCrossData.totalCount > 0 && it.first.totalCrossData.totalPercentage > 0 }
            .sortedWith(
                compareByDescending<Pair<MACrossResult, String>> {
                    it.first.totalCrossData.minPercentage
                }.thenByDescending {
                    it.first.totalCrossData.totalPercentage
                }
            ).subList(0, 10.coerceAtMost(list.size))
            .forEach {
                println("\n${it.second} \n${it.first}")
            }
    }

    private fun calculateSpecificMAArg(
        symbol: SymbolData,
    ) {
        val result = MACrossUtils.calculateMACross(
            symbol = symbol,
            kLineData = Utils.getSinaKLineData(symbol, findBestData = true, useLocalData = true, datalen = 10000),
        )
        val args =
            "--- 参数：code=\"${symbol.code}\",desc=\"${symbol.desc}\",scale=${symbol.scale},d=${symbol.d},shortMA=${symbol.shortMA},longMA=${symbol.longMA},maType=MAType.${symbol.maType}" +
                ",upCrossDiffRate=${symbol.upCrossDiffRate},downCrossDiffRate=${symbol.downCrossDiffRate}),"
        println("\n$args \n${result} \n${result.totalCrossData.crossDataList.joinToString("\n")}")
    }

    // --- 参数：code="sz159915",desc="易方达创业板ETF",scale=240,d=1,shortMA=1,longMA=25,maType=MAType.SMA,upCrossDiffRate=0.04,downCrossDiffRate=0.0)
    // 2021 总次数: 3 胜率: 100.00% 涨幅: 11.52% 最大涨幅: 8.08% 最大回撤: 0.00%
    // 2022 总次数: 2 胜率: 50.00% 涨幅: 7.40% 最大涨幅: 11.38% 最大回撤: -3.99%
    // 2023 总次数: 1 胜率: 100.00% 涨幅: 1.46% 最大涨幅: 1.46% 最大回撤: 0.00%
    // 2024 总次数: 3 胜率: 66.67% 涨幅: 34.37% 最大涨幅: 35.41% 最大回撤: -3.05%
    // 2025 总次数: 4 胜率: 25.00% 涨幅: 32.52% 最大涨幅: 39.12% 最大回撤: -3.44%
    // total: 平均年涨幅17.45% 总次数: 13 胜率: 61.54% 涨幅: 87.26% 最大涨幅: 39.12% 最大回撤: -3.99%

    // --- 参数：code="sh515050",desc="5G通信ETF",scale=240,d=1,shortMA=1,longMA=20,maType=MAType.SMA,upCrossDiffRate=0.01,downCrossDiffRate=0.0)
    // 2020 总次数: 9 胜率: 33.33% 涨幅: 8.78% 最大涨幅: 13.55% 最大回撤: -4.81%
    // 2021 总次数: 9 胜率: 44.44% 涨幅: 9.39% 最大涨幅: 8.11% 最大回撤: -3.45%
    // 2022 总次数: 6 胜率: 33.33% 涨幅: 5.33% 最大涨幅: 7.38% 最大回撤: -2.31%
    // 2023 总次数: 11 胜率: 27.27% 涨幅: 6.82% 最大涨幅: 13.38% 最大回撤: -4.74%
    // 2024 总次数: 8 胜率: 37.50% 涨幅: 31.48% 最大涨幅: 21.18% 最大回撤: -3.32%
    // 2025 总次数: 7 胜率: 42.86% 涨幅: 82.48% 最大涨幅: 89.82% 最大回撤: -3.13%
    // total: 平均年涨幅24.05% 总次数: 50 胜率: 36.00% 涨幅: 144.28% 最大涨幅: 89.82% 最大回撤: -4.81%

    // --- 参数：code="sh513820",desc="港股红利ETF",scale=240,d=1,shortMA=5,longMA=10,maType=MAType.SMA,upCrossDiffRate=0.02,downCrossDiffRate=-0.01)
    // 2025 总次数: 3 胜率: 100.00% 涨幅: 21.61% 单次最大涨幅: 17.52% 单次最大回撤: 0.00%
    // total: 平均年涨幅21.61% 总次数: 3 胜率: 100.00% 涨幅: 21.61% 单次最大涨幅: 17.52% 单次最大回撤: 0.00%
    // 最大回撤:4.74% 持续天数:6 2025-11-12 - 2025-11-18 修复时间:修复中

    // --- 参数：code="sh512890",desc="红利低波ETF",scale=240,d=1,shortMA=1,longMA=15,maType=MAType.SMA,upCrossDiffRate=0.0,downCrossDiffRate=-0.01)
    // 2019 总次数: 5 胜率: 60.00% 涨幅: 7.07% 最大涨幅: 6.71% 最大回撤: -1.48%
    // 2020 总次数: 9 胜率: 55.56% 涨幅: 15.50% 最大涨幅: 13.65% 最大回撤: -3.71%
    // 2021 总次数: 7 胜率: 42.86% 涨幅: 19.29% 最大涨幅: 15.55% 最大回撤: -3.04%
    // 2022 总次数: 11 胜率: 45.45% 涨幅: 10.32% 最大涨幅: 6.38% 最大回撤: -3.36%
    // 2023 总次数: 10 胜率: 30.00% 涨幅: 3.79% 最大涨幅: 5.96% 最大回撤: -2.24%
    // 2024 总次数: 9 胜率: 44.44% 涨幅: 7.46% 最大涨幅: 5.71% 最大回撤: -2.73%
    // 2025 总次数: 6 胜率: 50.00% 涨幅: 10.32% 最大涨幅: 8.41% 最大回撤: -2.30%
    // total: 平均年涨幅10.54% 总次数: 57 胜率: 45.61% 涨幅: 73.75% 最大涨幅: 15.55% 最大回撤: -3.71%

    // --- 参数：code="sh515100",desc="红利低波100ETF",scale=240,d=1,shortMA=10,longMA=15,maType=MAType.SMA,upCrossDiffRate=0.0,downCrossDiffRate=0.0)
    // 2021 总次数: 5 胜率: 60.00% 涨幅: 13.50% 最大涨幅: 8.96% 最大回撤: -0.99%
    // 2022 总次数: 10 胜率: 50.00% 涨幅: 2.40% 最大涨幅: 6.77% 最大回撤: -3.39%
    // 2023 总次数: 8 胜率: 25.00% 涨幅: 12.22% 最大涨幅: 14.02% 最大回撤: -3.74%
    // 2024 总次数: 14 胜率: 42.86% 涨幅: 10.83% 最大涨幅: 9.25% 最大回撤: -2.07%
    // 2025 总次数: 9 胜率: 77.78% 涨幅: 6.30% 最大涨幅: 2.39% 最大回撤: -0.64%
    // total: 平均年涨幅9.05% 总次数: 46 胜率: 50.00% 涨幅: 45.26% 最大涨幅: 14.02% 最大回撤: -3.74%

    // --- 参数：code="sz159201",desc="华夏国证自由现金流",scale=240,d=1,shortMA=1,longMA=5,maType=MAType.SMA,upCrossDiffRate=0.0,downCrossDiffRate=-0.03)
    // 2025 总次数: 2 胜率: 50.00% 涨幅: 25.63% 最大涨幅: 26.04% 最大回撤: -0.40%
    // total: 平均年涨幅25.63% 总次数: 2 胜率: 50.00% 涨幅: 25.63% 最大涨幅: 26.04% 最大回撤: -0.40%

    // --- 参数：code="sz159545",desc="恒生红利低波ETF",scale=240,d=1,shortMA=5,longMA=10,maType=MAType.SMA,upCrossDiffRate=0.01,downCrossDiffRate=0.0)
    // 2025 总次数: 5 胜率: 80.00% 涨幅: 23.94% 最大涨幅: 17.15% 最大回撤: -1.11%
    // total: 平均年涨幅23.94% 总次数: 5 胜率: 80.00% 涨幅: 23.94% 最大涨幅: 17.15% 最大回撤: -1.11%

    // --- 参数：code="sh513130",desc="恒生科技ETF",scale=240,d=1,shortMA=5,longMA=30,maType=MAType.SMA,upCrossDiffRate=0.01,downCrossDiffRate=0.0)
    // 2022 总次数: 1 胜率: 100.00% 涨幅: 6.78% 最大涨幅: 6.78% 最大回撤: 0.00%
    // 2023 总次数: 4 胜率: 50.00% 涨幅: 23.94% 最大涨幅: 29.20% 最大回撤: -7.44%
    // 2024 总次数: 4 胜率: 100.00% 涨幅: 25.86% 最大涨幅: 20.12% 最大回撤: 0.00%
    // 2025 总次数: 6 胜率: 50.00% 涨幅: 17.53% 最大涨幅: 19.08% 最大回撤: -3.43%
    // total: 平均年涨幅18.53% 总次数: 15 胜率: 66.67% 涨幅: 74.11% 最大涨幅: 29.20% 最大回撤: -7.44%

    // --- 参数：code="sz159892",desc="恒生医药ETF",scale=240,d=1,shortMA=10,longMA=20,maType=MAType.SMA,upCrossDiffRate=0.01,downCrossDiffRate=-0.01)
    // 2022 总次数: 2 胜率: 50.00% 涨幅: 18.92% 最大涨幅: 25.46% 最大回撤: -6.55%
    // 2023 总次数: 4 胜率: 50.00% 涨幅: 25.65% 最大涨幅: 30.47% 最大回撤: -5.26%
    // 2024 总次数: 4 胜率: 25.00% 涨幅: 7.33% 最大涨幅: 23.00% 最大回撤: -6.16%
    // 2025 总次数: 2 胜率: 100.00% 涨幅: 62.87% 最大涨幅: 43.59% 最大回撤: 0.00%
    // total: 平均年涨幅28.69% 总次数: 12 胜率: 50.00% 涨幅: 114.76% 最大涨幅: 43.59% 最大回撤: -6.55%

    // --- 参数：code="sz159941",desc="纳指ETF广发",scale=240,d=1,shortMA=20,longMA=60,maType=MAType.SMA,upCrossDiffRate=0.01,downCrossDiffRate=-0.05)
    // 2016 总次数: 0 胜率: 0.00% 涨幅: 0.00% 单次最大涨幅: 0.00% 单次最大回撤: 0.00%
    // 2017 总次数: 0 胜率: 0.00% 涨幅: 0.00% 单次最大涨幅: 0.00% 单次最大回撤: 0.00%
    // 2018 总次数: 1 胜率: 100.00% 涨幅: 64.24% 单次最大涨幅: 64.24% 单次最大回撤: 0.00%
    // 2019 总次数: 0 胜率: 0.00% 涨幅: 0.00% 单次最大涨幅: 0.00% 单次最大回撤: 0.00%
    // 2020 总次数: 1 胜率: 100.00% 涨幅: 14.53% 单次最大涨幅: 14.53% 单次最大回撤: 0.00%
    // 2021 总次数: 0 胜率: 0.00% 涨幅: 0.00% 单次最大涨幅: 0.00% 单次最大回撤: 0.00%
    // 2022 总次数: 1 胜率: 100.00% 涨幅: 36.13% 单次最大涨幅: 36.13% 单次最大回撤: 0.00%
    // 2023 总次数: 0 胜率: 0.00% 涨幅: 0.00% 单次最大涨幅: 0.00% 单次最大回撤: 0.00%
    // 2024 总次数: 0 胜率: 0.00% 涨幅: 0.00% 单次最大涨幅: 0.00% 单次最大回撤: 0.00%
    // 2025 总次数: 1 胜率: 100.00% 涨幅: 70.39% 单次最大涨幅: 70.39% 单次最大回撤: 0.00%
    // total: 平均年涨幅18.53% 总次数: 4 胜率: 100.00% 涨幅: 185.29% 单次最大涨幅: 70.39% 单次最大回撤: 0.00%
    // 最大回撤:31.10% 持续天数:34 2020-02-13 - 2020-03-18 修复时间:168 2020-03-18 - 2020-09-02

    @Test
    fun fetchJsonFile() {
        val symbol = listOf(
            "sz159915",
            "sh518880",
            "sz159201",
            "sh515100",
            "sh512890",
            "sz159941",
            "sh513130",
            "sz159892",
            "sz159545",
            "sh515050",
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
                Thread.sleep(Random.nextLong(1000, 3000))
            }
        }
    }
}