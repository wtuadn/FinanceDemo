package com.example.myapplication

import com.example.myapplication.data.KLineData
import com.example.myapplication.data.MACrossResult
import com.example.myapplication.utils.EXPMACrossUtils
import com.example.myapplication.utils.MACrossUtils
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
        val symbol = "sz159338" to "中证A500ETF"
        // val symbol = "sh512100" to "中证1000ETF"
        // val symbol = "sh510500" to "中证500ETF"
        // val symbol = "sh510300" to "沪深300ETF"
        // val symbol = "sh510050" to "上证50ETF"
        // val symbol = "sh515880" to "通信ETF"
        // val symbol = "sz159941" to "纳指ETF广发"
        // val symbol = "sh513300" to "纳指ETF华夏"
        // val symbol = "sh588000" to "科创50ETF"
        // val symbol = "sz159915" to "易方达创业板ETF"
        // val symbol = "sh518880" to "黄金ETF"
        // val symbol = "sz159201" to "华夏国证自由现金流"
        // val symbol = "sz980092" to "自由现金流指数"
        // val symbol = "sh515450" to "南方红利低波50"
        val scale = 240
        val d = 1

        var json: String? = null
        if (json.isNullOrEmpty()) {
            json = runCatching { File("data", "${symbol.first}.$d.json").readText() }.getOrNull()
        }
        if (json.isNullOrEmpty()) {
            val api =
                "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=${symbol.first}&scale=${scale * d}&ma=no&datalen=100000"
            println(api)
            json = Utils.httpGet(urlString = api, headMap = mapOf("Referer" to "https://finance.sina.com.cn", "host" to "hq.sinajs.cn"))
        }
        if (!json.isNullOrEmpty()) {
            var kLineData = parseKLineData(json)
            // kLineData = Utils.findBestKLineDataList(kLineData)
            // kLineData = kLineData.subList(kLineData.indexOfLast { it.date.startsWith("2012") } + 1, kLineData.size)
            // val short = EXPMACrossUtils.calculateEMAData(kLineData, 5)
            // val long = EXPMACrossUtils.calculateEMAData(kLineData, 10)
            // println("${short.subList(10,15).joinToString("\n")}\n")
            // println("${long.subList(10,15).joinToString("\n")}\n")
            // println("${short.takeLast(5).joinToString("\n")}\n")
            // println("${long.takeLast(5).joinToString("\n")}\n")
            // calculateBestMAArgs(kLineData, symbol, d)
            // calculateSpecificMAArg(kLineData, symbol, d)
            calculateBestEXPMAArgs(kLineData, symbol, scale, d)
            // calculateSpecificEXPMAArg(kLineData, symbol, scale, d)
        }

        println()
    }

    private fun calculateBestEXPMAArgs(kLineData: List<KLineData>, symbol: Pair<String, String>, scale: Int, d: Int) {
        println("--- 有效数据时间段为：${kLineData.firstOrNull()?.date} - ${kLineData.lastOrNull()?.date} ---\n")
        val shortMA = 1
        val longMA = 5

        val list = mutableListOf<Pair<MACrossResult, String>>()

        val start = 0.0
        val end = 0.0501
        val step = 0.005
        var upCrossDiffRate = start
        var downCrossDiffRate = start
        // 使用 while 循环进行浮点数步进迭代
        while (upCrossDiffRate <= end) {
            while (downCrossDiffRate >= -end) {
                val result = EXPMACrossUtils.calculateEXPMACross(
                    kLineData = kLineData,
                    shortMA = shortMA,
                    longMA = longMA,
                    upCrossDiffRate = upCrossDiffRate,
                    downCrossDiffRate = downCrossDiffRate,
                )
                val args =
                    "--- 参数：${symbol.first} ${symbol.second} scale=$scale d=$d shortMA=$shortMA longMA=$longMA " +
                        "upCrossMADiffRate=${"%.3f".format(upCrossDiffRate)} downCrossMADiffRate=${"%.3f".format(downCrossDiffRate)} ---"
                list.add(result to args)
                downCrossDiffRate -= step
            }
            upCrossDiffRate += step
            downCrossDiffRate = start
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

    private fun calculateSpecificEXPMAArg(kLineData: List<KLineData>, symbol: Pair<String, String>, scale: Int, d: Int) {
        val shortMA = 1
        val longMA = 5
        val upCrossDiffRate = 0.000
        val downCrossDiffRate = -0.000
        val result = EXPMACrossUtils.calculateEXPMACross(
            kLineData = kLineData,
            shortMA = shortMA,
            longMA = longMA,
            upCrossDiffRate = upCrossDiffRate,
            downCrossDiffRate = downCrossDiffRate,
        )
        val args =
            "--- 参数：${symbol.first} ${symbol.second} scale=$scale d=$d shortMA=$shortMA longMA=$longMA " +
                "upCrossDiffRate=${"%.3f".format(upCrossDiffRate)} downCrossDiffRate=${"%.3f".format(downCrossDiffRate)} ---"
        println("\n$args \n${result} \n${result.totalCrossData.crossDataList.joinToString("\n")}")
    }

    private fun calculateSpecificMAArg(kLineData: List<KLineData>, symbol: Pair<String, String>, d: Int) {
        val shortMA = 1
        val longMA = 15
        val upCrossMADiffRate = 0.000
        val downCrossMADiffRate = -0.000
        val result = MACrossUtils.calculateMACross(
            kLineData = kLineData,
            shortMA = shortMA,
            longMA = longMA,
            upCrossMADiffRate = upCrossMADiffRate,
            downCrossMADiffRate = downCrossMADiffRate,
            logPerCross = true,
        )
        val args =
            "--- 参数：${symbol.first} ${symbol.second} d=$d shortMA=$shortMA longMA=$longMA " +
                "upCrossMADiffRate=${"%.3f".format(upCrossMADiffRate)} downCrossMADiffRate=${"%.3f".format(downCrossMADiffRate)} ---"
        println("\n$args \n${result.third} ")
    }

    private fun calculateBestMAArgs(kLineData: List<KLineData>, symbol: Pair<String, String>, d: Int) {
        val shortMA = 1
        val longMA = 5

        val list = mutableListOf<Triple<Double, Double, String>>()

        val start = 0.0
        val end = 0.1001
        val step = 0.005
        var upCrossMADiffRate = start
        var downCrossMADiffRate = start
        // 使用 while 循环进行浮点数步进迭代
        while (upCrossMADiffRate <= end) {
            while (downCrossMADiffRate >= -end) {
                val result = MACrossUtils.calculateMACross(
                    kLineData = kLineData,
                    shortMA = shortMA,
                    longMA = longMA,
                    upCrossMADiffRate = upCrossMADiffRate,
                    downCrossMADiffRate = downCrossMADiffRate,
                    logPerCross = false,
                )
                val args =
                    "--- 参数：${symbol.first} ${symbol.second} d=$d shortMA=$shortMA longMA=$longMA " +
                        "upCrossMADiffRate=${"%.3f".format(upCrossMADiffRate)} downCrossMADiffRate=${"%.3f".format(downCrossMADiffRate)} ---"
                list.add(Triple(result.first, result.second, "\n$args \n${result.third} "))
                downCrossMADiffRate -= step
            }
            upCrossMADiffRate += step
            downCrossMADiffRate = start
        }
        println("\n\n收益优先")
        println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---")
        println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---")
        println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---")
        list.sortedByDescending { it.first }
            .subList(0, 10.coerceAtMost(list.size))
            .forEach {
                println(it.third)
            }
        println("\n\n回撤优先")
        println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---")
        println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---")
        println("--- --- --- --- --- --- --- --- --- --- --- --- --- --- ---")
        list.sortedWith(
            compareByDescending<Triple<Double, Double, String>> {
                it.second
            }.thenByDescending {
                it.first
            }
        ).subList(0, 10.coerceAtMost(list.size))
            .forEach {
                println(it.third)
            }
    }
// --- 参数：sz159338 中证A500ETF scale=240 d=1 shortMA=1 longMA=5 upCrossMADiffRate=0.000 downCrossMADiffRate=-0.005 ---
// 2024 总次数: 4 胜率: 50.00% 涨幅: 1.55% 最大涨幅: 2.38% 最大回撤: -1.05%
// 2025 总次数: 10 胜率: 60.00% 涨幅: 22.88% 最大涨幅: 7.75% 最大回撤: -2.65%
// total: 平均年涨幅12.21% 总次数: 14 胜率: 57.14% 涨幅: 24.43% 最大涨幅: 7.75% 最大回撤: -2.65%

// --- 参数：sz159941 纳指ETF广发 scale=60 d=1 shortMA=1 longMA=10 upCrossMADiffRate=0.000 downCrossMADiffRate=0.000 ---
// 2023 总次数: 53 胜率: 43.40% 涨幅: 38.40% 最大涨幅: 5.86% 最大回撤: -1.99%
// 2024 总次数: 61 胜率: 36.07% 涨幅: 28.20% 最大涨幅: 7.19% 最大回撤: -3.28%
// 2025 总次数: 59 胜率: 38.98% 涨幅: 26.07% 最大涨幅: 7.90% 最大回撤: -2.81%
// total: 平均年涨幅30.89% 总次数: 173 胜率: 39.31% 涨幅: 92.67% 最大涨幅: 7.90% 最大回撤: -3.28%

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
                resultList.add(KLineData(jsonObject.optString("day"), jsonObject.optString("close"), jsonObject.optLong("volume")))
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