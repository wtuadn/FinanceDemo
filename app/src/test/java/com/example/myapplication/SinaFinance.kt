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
            SymbolData("sz159869", "游戏ETF", 240, 1, 10, 20, MAType.SMA, 0.170, -0.150, 0.251, 0.0015, -0.036),
            SymbolData("sz159852", "软件ETF", 240, 1, 20, 25, MAType.SMA, 0.190, 0.000, 0.082, 0.0029, -0.003),
            SymbolData("sh516510", "云计算ETF", 240, 5, 1, 25, MAType.SMA, 0.170, 0.000, 0.213, 0.0012, 0.000),
            SymbolData("sz159998", "计算机ETF", 240, 5, 1, 25, MAType.EMA, 0.090, -0.020, 0.088, 0.0006, 0.000),
            SymbolData("sh515400", "大数据ETF", 240, 1, 1, 5, MAType.EMA, 0.180, -0.020, 0.146, 0.0044, 0.000),
            SymbolData("sh601398", "工商银行", 240, 5, 15, 20, MAType.SMA, 0.140, 0.000, 0.130, 0.0006, -0.004),
            SymbolData("sh600036", "招商银行", 240, 1, 20, 30, MAType.SMA, 0.070, -0.050, 0.378, 0.0009, -0.015),
            SymbolData("sh513120", "港服创新药ETF", 240, 1, 1, 20, MAType.SMA, 0.160, -0.030, 0.140, 0.0018, -0.014),
            SymbolData("sh515790", "光伏ETF", 240, 1, 15, 40, MAType.SMA, 0.130, 0.000, 0.179, 0.0022, 0.000),
            SymbolData("sh513550", "港股通50ETF", 240, 5, 20, 25, MAType.SMA, 0.080, 0.000, 0.204, 0.0018, 0.000),
            SymbolData("sh512710", "军工龙头ETF", 240, 1, 1, 10, MAType.EMA, 0.140, -0.020, 0.121, 0.0035, -0.036),
            SymbolData("sz159227", "航空航天ETF", 240, 1, 5, 10, MAType.SMA, 0.010, -0.010, 0.028, 0.0007, -0.018),
            SymbolData("sz159218", "卫星产业ETF", 240, 1, 5, 10, MAType.EMA, 0.030, 0.000, 0.033, 0.0012, -0.007),
            SymbolData("sz159813", "半导体ETF", 240, 1, 30, 40, MAType.SMA, 0.200, 0.000, 0.064, 0.0010, -0.005),
            SymbolData("sz159713", "稀土ETF", 240, 1, 20, 25, MAType.EMA, 0.120, 0.000, 0.184, 0.0021, 0.000),
            SymbolData("sz159985", "豆粕ETF", 240, 5, 1, 15, MAType.EMA, 0.000, -0.130, 0.335, 0.0004, 0.000),
            SymbolData("sh561330", "矿业ETF", 240, 1, 30, 40, MAType.SMA, 0.170, 0.000, 0.215, 0.0029, 0.000),
            SymbolData("sh513400", "道琼斯ETF", 240, 1, 25, 40, MAType.SMA, 0.050, 0.000, 0.056, 0.0005, -0.006),
            SymbolData("sh510230", "金融ETF", 240, 1, 1, 10, MAType.SMA, 0.120, 0.000, 0.052, 0.0034, 0.000),
            SymbolData("sz159851", "金融科技ETF", 240, 5, 10, 20, MAType.SMA, 0.060, 0.000, 0.243, 0.0017, 0.000),
            SymbolData("sh516860", "金融科技ETF", 240, 5, 5, 20, MAType.EMA, 0.060, -0.040, 0.265, 0.0008, 0.000),
            SymbolData("sh512010", "医药ETF", 240, 5, 20, 25, MAType.SMA, 0.170, 0.000, 0.273, 0.0007, 0.000),
            SymbolData("sz159766", "旅游ETF", 240, 1, 20, 25, MAType.SMA, 0.130, 0.000, 0.026, 0.0027, -0.003),
            SymbolData("sh588790", "科创AIETF", 240, 5, 5, 10, MAType.SMA, 0.000, 0.000, 0.177, 0.0030, 0.000),
            SymbolData("sh513310", "中韩半导体ETF", 240, 1, 10, 30, MAType.SMA, 0.160, -0.030, 0.209, 0.0028, 0.000),
            SymbolData("sh588220", "科创100ETF基金", 240, 1, 1, 40, MAType.EMA, 0.060, 0.000, 0.166, 0.0023, 0.000),
            SymbolData("sh588000", "科创50ETF", 240, 5, 1, 5, MAType.EMA, 0.160, 0.000, 0.073, 0.0012, 0.000),
            SymbolData("sz159755", "电池ETF", 240, 1, 1, 20, MAType.EMA, 0.160, 0.000, 0.167, 0.0035, 0.000),
            SymbolData("sh513090", "香港证券ETF", 240, 1, 1, 5, MAType.EMA, 0.140, -0.020, 0.153, 0.0060, -0.009),
            SymbolData("sh562500", "机器人ETF", 240, 5, 1, 5, MAType.SMA, 0.080, -0.010, 0.071, 0.0012, -0.019),
            SymbolData("sz159915", "易方达创业板ETF", 240, 5, 1, 15, MAType.EMA, 0.120, 0.000, 0.083, 0.0009, -0.018),
            SymbolData("sh515050", "5G通信ETF", 240, 1, 20, 25, MAType.SMA, 0.200, 0.000, 0.225, 0.0058, -0.041),
            SymbolData("sh513820", "港股红利ETF", 240, 5, 1, 5, MAType.SMA, 0.010, 0.000, 0.096, 0.0014, 0.000),
            SymbolData("sz159201", "华夏国证自由现金流ETF", 240, 1, 1, 5, MAType.SMA, 0.000, -0.030, 0.128, 0.0010, -0.004),
            SymbolData("sz159545", "恒生红利低波ETF", 240, 1, 10, 15, MAType.SMA, 0.010, 0.000, 0.083, 0.0011, -0.006),
            SymbolData("sh513130", "恒生科技ETF", 240, 5, 30, 40, MAType.SMA, 0.010, 0.000, 0.221, 0.0009, -0.012),
            SymbolData("sz159892", "恒生医药ETF", 240, 5, 1, 5, MAType.SMA, 0.120, 0.000, 0.097, 0.0015, -0.014),
            SymbolData("sz159941", "纳指ETF广发", 240, 5, 5, 10, MAType.EMA, 0.000, -0.080, 0.648, 0.0007, -0.020),
            SymbolData("sh518880", "黄金ETF", 240, 1, 1, 5, MAType.SMA, 0.050, -0.070, 0.564, 0.0006, -0.066),
        )

        // calculateBestMAArgs(symbols.first())
        // symbols.forEach { calculateSpecificMAArg(it) }
        // calculateBestMAArgs(SymbolData("sz002594", "比亚迪", 240, 5, 30, 40, MAType.SMA, 0.010, 0.000, 0.221, 0.0009, -0.012))
        queryTradeSignal(symbols)
        // calculateSpecificMAArg(SymbolData("sh513130", "恒生科技ETF", 240, 5, 30, 40, MAType.SMA, 0.010, 0.000, 0.221, 0.0009, -0.012))

        println()
    }

    private fun queryTradeSignal(symbols: List<SymbolData>) {
        symbols.forEach {
            val tradeSignalData = MACrossUtils.getTradeSignal(it)
            println("${it.code} ${it.desc} ${tradeSignalData}}")
            Thread.sleep(Random.nextLong(100, 500))
        }
    }

    private fun calculateBestMAArgs(symbol: SymbolData) {
        val list = mutableListOf<Pair<MACrossResult, String>>()
        val scale = 240
        listOf(1, 5).forEach { d ->
            symbol.d = d
            var kLineData = Utils.getSinaKLineData(symbol, findBestData = false, useLocalData = true, datalen = 10000)
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
        list.removeAll { it.first.totalCrossData.countlyPercentage < 0.01 } //过滤掉单次收益太低的
        list.removeAll { it.first.totalCrossData.totalCount < 2 } //过滤掉操作次数太少的
        // list.removeAll { it.first.totalCrossData.totalCount > 20 } //过滤掉操作次数太多的
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
                println("\n${it.second} \n${it.first.getTotalDesc()} \n${it.first.totalCrossData.crossDataList.joinToString("\n")}")
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
        var kLineData = Utils.getSinaKLineData(symbol, findBestData = false, useLocalData = true, datalen = 10000)
        kLineData = kLineData.filterNot { it.date.split("-").first().toInt() < 2016 }
        val result = MACrossUtils.calculateMACross(
            symbol = symbol,
            kLineData = kLineData,
        )
        println("\n${getArgStr(symbol)} \n${result.getTotalDesc()} \n${result.totalCrossData.crossDataList.joinToString("\n")}")
    }

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