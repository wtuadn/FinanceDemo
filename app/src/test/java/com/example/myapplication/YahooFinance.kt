package com.example.myapplication

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

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
        val symbol = "515450.SS" //南方红利低波50
        // val symbol = "159915.SZ" //易方达创业板ETF
        val startDate = "2012-01-01"
        val endDate = "2025-12-31"
        val st = Utils.dateToTimestamp(startDate)
        val et = Utils.dateToTimestamp(endDate)
        val api = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?period1=$st&period2=$et&interval=1d"
        println("\n$api")

        val json = Utils.httpGet(api)
        if (!json.isNullOrEmpty()) {
            val chartData = parseChartData(json)
            val maDataList1 = Utils.calculateMAData(chartData, 1)
            val maDataList2 = Utils.calculateMAData(chartData, 5)
            Utils.calculateMACross(maDataList1, maDataList2)
        }
    }

    /**
     * 解析原始 JSON 字符串，提取时间戳和收盘价，并进行转换封装。
     * @param jsonString 完整的 JSON 字符串。
     * @return ChartData 对象的列表。
     */
    fun parseChartData(jsonString: String): List<KLineData> {
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

                // 检查收盘价是否为 null (JSON 中为 null)
                val closeValue: Double = if (closeArray.isNull(i)) {
                    -1.0
                } else {
                    closeArray.getDouble(i)
                }

                // 1. 转换日期
                val dateStr = Utils.timestampToDate(timestampSecs)

                // 2. 四舍五入收盘价
                val roundedClose = Utils.roundToThreeDecimalPlaces(closeValue)

                // 3. 封装并添加到结果列表
                resultList.add(KLineData(dateStr, roundedClose, 0))
            }
        } catch (e: Exception) {
            System.err.println("JSON 解析或转换失败: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }

        return Utils.findLongestNonNullSublist(resultList)
    }
}