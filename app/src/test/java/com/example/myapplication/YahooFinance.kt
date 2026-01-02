package com.example.myapplication

import com.example.myapplication.data.KLineData
import com.example.myapplication.utils.Utils
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import kotlin.math.round

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 * 651ED65C-9613-4143-8A5F-8DB6E1D235B6
 */
class YahooFinance {
    @Test
    fun main() {
        // val symbol = "159201.SZ" //华夏国证自由现金流
        // val symbol = "515450.SS" //南方红利低波50
        val symbol = "159915.SZ" //易方达创业板ETF
        val startDate = "2012-01-01"
        val endDate = "2025-12-31"
        val st = Utils.dateToTimestamp(startDate)
        val et = Utils.dateToTimestamp(endDate)
        val api = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?period1=$st&period2=$et&interval=1d"
        println("\n$api")

        val json = Utils.httpGet(api)
        if (!json.isNullOrEmpty()) {
            val kLineData = parseKLineData(json)
        }
    }

    /**
     * 解析原始 JSON 字符串，提取时间戳和收盘价，并进行转换封装。
     * @param jsonString 完整的 JSON 字符串。
     * @return KLineData 对象的列表。
     */
    fun parseKLineData(jsonString: String): List<KLineData> {
        val resultList = mutableListOf<KLineData>()

        try {
            val rootObject = JSONObject(jsonString)
            val chart = rootObject.getJSONObject("chart")
            val result = chart.getJSONArray("result")

            // 假设 result 数组只有一个元素
            if (result.length() == 0) return emptyList()

            val firstResult = result.getJSONObject(0)

            // 提取时间戳数组
            val timestampArray: JSONArray = firstResult.getJSONArray("timestamp")

            // 提取 close 价格数组 (位于 indicators -> quote -> [0] -> close)
            val indicators = firstResult.getJSONObject("indicators")
            val quote = indicators.getJSONArray("quote").getJSONObject(0)
            val closeArray = quote.getJSONArray("close")

            // 遍历并配对数据
            for (i in 0 until timestampArray.length()) {
                val timestampSecs = timestampArray.getLong(i)

                // 1. 转换日期
                val dateStr = Utils.timestampToDate(timestampSecs)

                // 3. 封装并添加到结果列表
                resultList.add(
                    KLineData(
                        date = dateStr,
                        closePrice = closeArray.optDouble(i, -1.0)
                    )
                )
            }
        } catch (e: Exception) {
            System.err.println("JSON 解析或转换失败: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }

        return Utils.findLongestSublist(resultList) { it.closePrice > 0 }
    }

    /**
     * 将收盘价四舍五入保留小数点后3位。
     * @param value 原始收盘价 (Double)。
     * @return 四舍五入后的 Double 值。
     */
    fun roundToThreeDecimalPlaces(value: Double): Double {
        // 放大 1000 倍，四舍五入，然后除以 1000
        // 注意：这里的乘法和除法会导致精度问题，但对于浮点数操作来说是常见且最简单的原生解决方案。
        // 更严谨的做法是使用 BigDecimal，但由于要求不依赖第三方库，我们使用 Double 的原生数学操作。
        return round(value * 1000.0) / 1000.0
    }
}