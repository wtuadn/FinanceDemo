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
        // val symbol = "sz159338" to "中证A500ETF"
        // val symbol = "sh512100" to "中证1000ETF"
        // val symbol = "sh510500" to "中证500ETF"
        // val symbol = "sh510300" to "沪深300ETF"
        // val symbol = "sh510050" to "上证50ETF"
        // val symbol = "sh515880" to "通信ETF"
        val symbol = "sz159941" to "纳指ETF广发"
        // val symbol = "sh588000" to "科创50ETF"
        // val symbol = "sz159915" to "易方达创业板ETF"
        // val symbol = "sh518880" to "黄金ETF"
        // val symbol = "sz159201" to "华夏国证自由现金流"
        // val symbol = "sz980092" to "自由现金流指数"
        // val symbol = "sh515450" to "南方红利低波50"
        val scale = 240
        val d = 5

        var json: String? = runCatching { File("data", "${symbol.first}.$d.json").readText() }.getOrNull()
        if (json.isNullOrEmpty()) {
            val api =
                "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=${symbol.first}&scale=${scale * d}&ma=no&datalen=10000"
            println(api)
            json = Utils.httpGet(
                urlString = api,
                headMap = mapOf(
                    "Referer" to "https://finance.sina.com.cn",
                    "host" to "hq.sinajs.cn",
                )
            )
        }
        if (!json.isNullOrEmpty()) {
            var kLineData = parseKLineData(json)
            kLineData = Utils.findBestKLineDataList(kLineData)
            // val short = EXPMACrossUtils.calculateEMAData(kLineData, 5)
            // val long = EXPMACrossUtils.calculateEMAData(kLineData, 10)
            // println("${short.subList(10,15).joinToString("\n")}\n")
            // println("${long.subList(10,15).joinToString("\n")}\n")
            // println("${short.takeLast(5).joinToString("\n")}\n")
            // println("${long.takeLast(5).joinToString("\n")}\n")
            // calculateBestMAArgs(kLineData, symbol, d)
            // calculateSpecificMAArg(kLineData, symbol, d)
            calculateBestEXPMAArgs(kLineData, symbol, d)
            // calculateSpecificEXPMAArg(kLineData, symbol, d)
        }

        println()
    }

    private fun calculateSpecificEXPMAArg(kLineData: List<KLineData>, symbol: Pair<String, String>, d: Int) {
        val shortMA = 5
        val longMA = 10
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
            "--- 参数：${symbol.first} ${symbol.second} d=$d shortMA=$shortMA longMA=$longMA " +
                "upCrossDiffRate=${"%.3f".format(upCrossDiffRate)} downCrossDiffRate=${"%.3f".format(downCrossDiffRate)} ---"
        println("\n$args \n${result}")
    }

    private fun calculateBestEXPMAArgs(kLineData: List<KLineData>, symbol: Pair<String, String>, d: Int) {
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
                    "--- 参数：${symbol.first} ${symbol.second} d=$d shortMA=$shortMA longMA=$longMA " +
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
// --- 参数：sz159941 纳指ETF广发 d=5 shortMA=1 longMA=5 upCrossMADiffRate=0.000 downCrossMADiffRate=-0.010 ---
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

//--- 参数：sh512100 中证1000ETF d=30 shortMA=1 longMA=5 upCrossMADiffRate=0.045 downCrossMADiffRate=-0.040 ---
// --- 有效数据时间段为：2016-11-30 - 2025-12-12 ---
// 2016: 0.00% 胜率：次数：0 胜率：0.00% 最大涨幅： 最大回撤：
// 2017: 0.00% 胜率：次数：0 胜率：0.00% 最大涨幅： 最大回撤：
// 2018: 0.00% 胜率：次数：0 胜率：0.00% 最大涨幅： 最大回撤：
// 2019: 0.00% 胜率：次数：0 胜率：0.00% 最大涨幅： 最大回撤：
// 2020: 0.00% 胜率：次数：0 胜率：0.00% 最大涨幅： 最大回撤：
// 2021: 40.13% 胜率：次数：1 胜率：100.00% 最大涨幅：2019-02-28 - 2021-03-31 : 40.13% 最大回撤：
// 2022: 3.99% 胜率：次数：1 胜率：100.00% 最大涨幅：2021-05-31 - 2022-01-28 : 3.99% 最大回撤：
// 2023: 156.61% 胜率：次数：1 胜率：100.00% 最大涨幅：2022-06-30 - 2023-07-31 : 156.61% 最大回撤：
// 2024: 0.00% 胜率：次数：0 胜率：0.00% 最大涨幅： 最大回撤：
// 2025: 0.00% 胜率：次数：0 胜率：0.00% 最大涨幅： 最大回撤：
// --- 平均年收益20.07% 次数：3 胜率：100.00% 最大涨幅：2022-06-30 - 2023-07-31 : 156.61% 最大回撤： ---

//--- 参数：sz159941 纳指ETF广发 d=30 shortMA=1 longMA=5 upCrossMADiffRate=0.000 downCrossMADiffRate=-0.050 ---
// --- 有效数据时间段为：2016-01-29 - 2025-12-12 ---
// 2016: 0.00% 胜率：次数：0 胜率：0.00% 最大涨幅： 最大回撤：
// 2017: 0.00% 胜率：次数：0 胜率：0.00% 最大涨幅： 最大回撤：
// 2018: 49.71% 胜率：次数：1 胜率：100.00% 最大涨幅：2016-07-29 - 2018-10-31 : 49.71% 最大回撤：
// 2019: 0.00% 胜率：次数：0 胜率：0.00% 最大涨幅： 最大回撤：
// 2020: 16.54% 胜率：次数：1 胜率：100.00% 最大涨幅：2019-02-28 - 2020-03-31 : 16.54% 最大回撤：
// 2021: 0.00% 胜率：次数：0 胜率：0.00% 最大涨幅： 最大回撤：
// 2022: 28.62% 胜率：次数：1 胜率：100.00% 最大涨幅：2020-04-30 - 2022-01-28 : 28.62% 最大回撤：
// 2023: 0.00% 胜率：次数：0 胜率：0.00% 最大涨幅： 最大回撤：
// 2024: 0.00% 胜率：次数：0 胜率：0.00% 最大涨幅： 最大回撤：
// 2025: 65.37% 胜率：次数：1 胜率：100.00% 最大涨幅：2023-02-28 - 2025-03-31 : 65.37% 最大回撤：
// --- 平均年收益16.02% 次数：4 胜率：100.00% 最大涨幅：2023-02-28 - 2025-03-31 : 65.37% 最大回撤： ---

//--- 参数：sh588000 科创50ETF d=1 shortMA=1 longMA=15 upCrossDiff=0.040 downCrossDiff=-0.040 ---
// --- 有效数据时间段为：2021-01-04 - 2025-12-12 ---
// 2021: 8.91% 胜率：次数：2 胜率：50.00% 最大涨幅：2021-05-25 - 2021-07-16 : 12.50% 最大回撤：2021-04-02 - 2021-04-14 : -3.59%
// 2022: 1.43% 胜率：次数：3 胜率：66.67% 最大涨幅：2022-05-11 - 2022-07-13 : 5.17% 最大回撤：2022-08-05 - 2022-08-23 : -4.69%
// 2023: 1.91% 胜率：次数：1 胜率：100.00% 最大涨幅：2023-03-23 - 2023-04-25 : 1.91% 最大回撤：
// 2024: 40.60% 胜率：次数：4 胜率：50.00% 最大涨幅：2024-09-26 - 2024-11-19 : 44.51% 最大回撤：2024-07-19 - 2024-07-26 : -3.95%
// 2025: 41.49% 胜率：次数：2 胜率：100.00% 最大涨幅：2025-07-25 - 2025-10-16 : 34.08% 最大回撤：
// --- 平均年收益18.87% 次数：12 胜率：66.67% 最大涨幅：2024-09-26 - 2024-11-19 : 44.51% 最大回撤：2022-08-05 - 2022-08-23 : -4.69% ---

//--- 参数：sz159915 易方达创业板ETF d=5 shortMA=1 longMA=5 upCrossDiff=0.030 downCrossDiff=-0.030 ---
// --- 有效数据时间段为：2016-01-08 - 2025-12-12 ---
// 2016: -5.53% 胜率：次数：2 胜率：0.00% 最大涨幅： 最大回撤：2016-06-03 - 2016-07-29 : -3.70%
// 2017: -0.06% 胜率：次数：1 胜率：0.00% 最大涨幅： 最大回撤：2017-08-18 - 2017-11-03 : -0.06%
// 2018: -4.18% 胜率：次数：2 胜率：0.00% 最大涨幅： 最大回撤：2018-03-02 - 2018-06-01 : -2.94%
// 2019: 28.31% 胜率：次数：2 胜率：100.00% 最大涨幅：2019-02-15 - 2019-04-30 : 19.28% 最大回撤：
// 2020: 41.34% 胜率：次数：2 胜率：100.00% 最大涨幅：2020-04-17 - 2020-09-11 : 26.04% 最大回撤：
// 2021: 16.26% 胜率：次数：2 胜率：100.00% 最大涨幅：2020-10-09 - 2021-02-26 : 9.03% 最大回撤：
// 2022: 6.16% 胜率：次数：2 胜率：50.00% 最大涨幅：2022-05-20 - 2022-07-29 : 10.52% 最大回撤：2022-11-04 - 2022-12-30 : -4.36%
// 2023: -1.61% 胜率：次数：1 胜率：0.00% 最大涨幅： 最大回撤：2023-01-13 - 2023-02-17 : -1.61%
// 2024: -3.51% 胜率：次数：2 胜率：50.00% 最大涨幅：2024-02-23 - 2024-04-12 : 0.23% 最大回撤：2024-04-30 - 2024-06-07 : -3.74%
// 2025: 48.39% 胜率：次数：3 胜率：66.67% 最大涨幅：2025-05-09 - 2025-10-17 : 46.93% 最大回撤：2025-02-07 - 2025-04-03 : -4.88%
// --- 平均年收益12.56% 次数：19 胜率：52.63% 最大涨幅：2025-05-09 - 2025-10-17 : 46.93% 最大回撤：2025-02-07 - 2025-04-03 : -4.88% ---

//--- 参数：sz159201 华夏国证自由现金流 d=1 shortMA=1 longMA=5 upCrossMADiffRate=0.000 downCrossMADiffRate=-0.005 ---
// --- 有效数据时间段为：2025-02-27 - 2025-12-12 ---
// 2025: 24.41% 胜率：次数：10 胜率：90.00% 最大涨幅：2025-06-24 - 2025-07-28 : 6.53% 最大回撤：2025-09-01 - 2025-09-04 : -2.40%
// --- 平均年收益24.41% 次数：10 胜率：90.00% 最大涨幅：2025-06-24 - 2025-07-28 : 6.53% 最大回撤：2025-09-01 - 2025-09-04 : -2.40% ---

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