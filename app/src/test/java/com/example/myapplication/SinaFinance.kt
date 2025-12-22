package com.example.myapplication

import com.example.myapplication.data.KLineData
import com.example.myapplication.data.MACrossResult
import com.example.myapplication.data.SymbolData
import com.example.myapplication.utils.MACrossUtils
import com.example.myapplication.utils.MACrossUtils.MA_TYPE
import com.example.myapplication.utils.Utils
import org.json.JSONArray
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
        // val symbol = SymbolData(code = "sz159338", desc = "中证A500ETF")
        // val symbol = SymbolData(code = "sh512100", desc = "中证1000ETF")
        // val symbol = SymbolData(code = "sh510500", desc = "中证500ETF")
        // val symbol = SymbolData(code = "sh510300", desc = "沪深300ETF")
        // val symbol = SymbolData(code = "sh510050", desc = "上证50ETF")
        // val symbol = SymbolData(code = "sh515880", desc = "通信ETF")
        // val symbol = SymbolData(code = "sz159941", desc = "纳指ETF广发")
        // val symbol = SymbolData(code = "sz159632", desc = "纳指ETF华安")
        // val symbol = SymbolData(code = "sh513300", desc = "纳指ETF华夏")
        // val symbol = SymbolData(code = "sh588000", desc = "科创50ETF")
        // val symbol = SymbolData(code = "sz159915", desc = "易方达创业板ETF")
        val symbol = SymbolData(code = "sh518880", desc = "黄金ETF")
        // val symbol = SymbolData(code = "sz159201", desc = "华夏国证自由现金流")
        // val symbol = SymbolData(code = "sz980092", desc = "自由现金流指数")
        // val symbol = SymbolData(code = "sh515450", desc = "南方红利低波50")
        // val symbol = SymbolData(code = "sh515100", desc = "红利低波100ETF")
        val scale = 240
        val d = 1

        var json: String? = null
        if (json.isNullOrEmpty()) {
            json = runCatching { File("data", "${symbol.code}.$d.json").readText() }.getOrNull()
        }
        if (json.isNullOrEmpty()) {
            val api =
                "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=${symbol.code}&scale=${scale * d}&ma=no&datalen=100000"
            println(api)
            json = Utils.httpGet(urlString = api, headMap = mapOf("Referer" to "https://finance.sina.com.cn", "host" to "hq.sinajs.cn"))
        }
        if (!json.isNullOrEmpty()) {
            var kLineData = parseKLineData(json)
            kLineData = Utils.findBestKLineDataList(kLineData)
            calculateBestMAArgs(symbol, scale, d, kLineData, MA_TYPE.SMA)
            calculateSpecificMAArg(
                symbol = symbol,
                scale = scale,
                d = d,
                kLineData = kLineData,
                shortMA = 1,
                longMA = 5,
                maType = MA_TYPE.SMA,
                upCrossDiffRate = 0.000,
                downCrossDiffRate = 0.000,
                volumeDiffRate = 0.000,
            )
        }

        println()
    }

    private fun calculateBestMAArgs(symbol: SymbolData, scale: Int, d: Int, kLineData: List<KLineData>, maType: MA_TYPE) {
        println("--- 有效数据时间段为：${kLineData.firstOrNull()?.date} - ${kLineData.lastOrNull()?.date} ---\n")
        val list = mutableListOf<Pair<MACrossResult, String>>()
        listOf(0.0 /*0.05, 0.10, 0.15, 0.20, 0.25, 0.30*/).forEach { volumeDiffRate ->
            listOf(1, 5, 10, 15, 20, 25, 30).forEach { shortMA ->
                listOf(5, 10, 15, 20, 25, 30, 40, 60, 120).forEach { longMA ->
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
                                kLineData = kLineData,
                                shortMA = shortMA,
                                longMA = longMA,
                                maType = maType,
                                upCrossDiffRate = upCrossDiffRate,
                                downCrossDiffRate = downCrossDiffRate,
                                volumeDiffRate = volumeDiffRate,
                            )
                            val args = "--- 参数：${symbol.code} ${symbol.desc} scale=$scale d=$d shortMA=$shortMA longMA=$longMA maType:${maType}" +
                                "upCrossMADiffRate=${upCrossDiffRate} downCrossMADiffRate=${downCrossDiffRate} volumeDiffRate=$volumeDiffRate ---"
                            list.add(result to args)
                            downCrossDiffRate -= step
                        }
                        upCrossDiffRate += step
                        downCrossDiffRate = start
                    }
                }
            }
        }
        println("\n\n收益优先")
        println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---")
        println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---")
        println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---")
        list.sortedByDescending { it.first.totalCrossData.totalPercentage }
            .subList(0, 10.coerceAtMost(list.size))
            .forEach {
                println("\n${it.second} \n${it.first}")
            }
        println("\n\n回撤优先")
        println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---")
        println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---")
        println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---")
        list.sortedWith(
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
        scale: Int,
        d: Int,
        kLineData: List<KLineData>,
        shortMA: Int,
        longMA: Int,
        maType: MA_TYPE = MA_TYPE.SMA,
        upCrossDiffRate: Double = 0.000,
        downCrossDiffRate: Double = 0.000,
        volumeDiffRate: Double = 0.000,
    ) {
        val result = MACrossUtils.calculateMACross(
            kLineData = kLineData,
            shortMA = shortMA,
            longMA = longMA,
            maType = maType,
            upCrossDiffRate = upCrossDiffRate,
            downCrossDiffRate = downCrossDiffRate,
            volumeDiffRate = volumeDiffRate,
        )
        val args = "--- 参数：${symbol.code} ${symbol.desc} scale=$scale d=$d shortMA=$shortMA longMA=$longMA maType:${maType}" +
            "upCrossMADiffRate=${upCrossDiffRate} downCrossMADiffRate=${downCrossDiffRate} volumeDiffRate=$volumeDiffRate ---"
        println("\n$args \n${result} \n${result.totalCrossData.crossDataList.joinToString("\n")}")
    }

//--- 参数：sh515100 红利低波100ETF scale=240 d=1 shortMA=1 longMA=10 upCrossMADiffRate=0.015 downCrossMADiffRate=0.000 ---
// 2021 总次数: 4 胜率: 75.00% 涨幅: 10.20% 最大涨幅: 8.05% 最大回撤: -1.34%
// 2022 总次数: 4 胜率: 75.00% 涨幅: 14.21% 最大涨幅: 7.13% 最大回撤: -0.65%
// 2023 总次数: 4 胜率: 75.00% 涨幅: 7.10% 最大涨幅: 4.61% 最大回撤: -0.94%
// 2024 总次数: 6 胜率: 50.00% 涨幅: 9.43% 最大涨幅: 10.22% 最大回撤: -1.62%
// 2025 总次数: 2 胜率: 50.00% 涨幅: -0.61% 最大涨幅: 0.26% 最大回撤: -0.87%
// total: 平均年涨幅8.06% 总次数: 20 胜率: 65.00% 涨幅: 40.32% 最大涨幅: 10.22% 最大回撤: -1.62%

// --- 参数：sz159941 纳指ETF广发 scale=240 d=5 shortMA=1 longMA=5 upCrossMADiffRate=0.000 downCrossMADiffRate=-0.010 ---
// 2016 总次数: 2 胜率: 50.00% 涨幅: 8.00% 最大涨幅: 9.91% 最大回撤: -1.91%
// 2017 总次数: 3 胜率: 33.33% 涨幅: 9.17% 最大涨幅: 12.47% 最大回撤: -2.36%
// 2018 总次数: 4 胜率: 25.00% 涨幅: 11.82% 最大涨幅: 15.67% 最大回撤: -1.67%
// 2019 总次数: 1 胜率: 100.00% 涨幅: 11.10% 最大涨幅: 11.10% 最大回撤: 0.00%
// 2020 总次数: 4 胜率: 50.00% 涨幅: 45.09% 最大涨幅: 31.82% 最大回撤: -3.42%
// 2021 总次数: 5 胜率: 80.00% 涨幅: 12.79% 最大涨幅: 9.21% 最大回撤: -1.84%
// 2022 总次数: 3 胜率: 0.00% 涨幅: -4.83% 最大涨幅: 0.00% 最大回撤: -2.48%
// 2023 总次数: 3 胜率: 33.33% 涨幅: 25.91% 最大涨幅: 33.28% 最大回撤: -4.19%
// 2024 总次数: 3 胜率: 66.67% 涨幅: 15.01% 最大涨幅: 14.84% 最大回撤: -6.56%
// 2025 总次数: 2 胜率: 100.00% 涨幅: 40.23% 最大涨幅: 26.88% 最大回撤: 0.00%
// total: 平均年涨幅17.43% 总次数: 30 胜率: 50.00% 涨幅: 174.30% 最大涨幅: 33.28% 最大回撤: -6.56%

    /**
     * 解析原始 JSON 字符串，提取时间戳和收盘价，并进行转换封装。
     * @param jsonString 完整的 JSON 字符串。
     * @return ChartData 对象的列表。
     */
    fun parseKLineData(jsonString: String): List<KLineData> {
        val resultList = mutableListOf<KLineData>()

        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                resultList.add(
                    KLineData(
                        date = jsonObject.optString("day"),
                        openPriceStr = jsonObject.optString("open"),
                        closePriceStr = jsonObject.optString("close"),
                        volume = jsonObject.optLong("volume")
                    )
                )
            }
        } catch (e: Exception) {
            System.err.println("JSON 解析或转换失败: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
        return Utils.findLongestNonNullSublist(resultList)
    }

    @Test
    fun fetchJsonFile() {
        val symbol = listOf(
            "sz159338",
            "sh512100",
            "sh510500",
            "sh510300",
            "sh510050",
            "sh515880",
            "sz159941",
            "sh588000",
            "sz159915",
            "sh518880",
            "sz159201",
            "sz980092",
            "sh515450",
        )
        val scale = 240
        val d = listOf(1, 5, 30)
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
                Thread.sleep(Random.nextLong(1000, 5000))
            }
        }
    }
}