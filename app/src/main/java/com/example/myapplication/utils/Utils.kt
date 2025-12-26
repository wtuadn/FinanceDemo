package com.example.myapplication.utils

import com.example.myapplication.data.AlignedMAData
import com.example.myapplication.data.KLineData
import com.example.myapplication.data.MAData
import com.example.myapplication.data.SymbolData
import org.json.JSONArray
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object Utils {
    fun calculateAlignedMAData(
        shortMADataList: List<MAData>,
        longMADataList: List<MAData>,
    ): List<AlignedMAData> {
        return shortMADataList.mapIndexedNotNull { index, ma1 ->
            val ma2 = longMADataList.getOrNull(index)
            if (ma1.value != null && ma2?.value != null && ma1.date == ma2.date) {
                AlignedMAData(
                    ma1.date,
                    ma1.closePrice,
                    ma1.volume,
                    ma1.value,
                    ma2.value,
                )
            } else {
                null
            }
        }
    }

    fun getPercentageString(percentage: Double): String {
        return "%.2f%%".format(percentage * 100)
    }

    fun getVictoryRateString(upCount: Int, downCount: Int): String {
        val rate = when {
            upCount == 0 -> getPercentageString(0.0)
            downCount == 0 -> getPercentageString(1.0)
            else -> getPercentageString(upCount.toDouble() / (upCount + downCount))
        }
        return "次数：${upCount + downCount} 胜率：$rate"
    }

    fun findLongestNonNullSublist(dataList: List<KLineData>): List<KLineData> {
        // if (true) return dataList
        // 用于记录当前连续非 null 子列表的起始索引
        var currentStartIndex = 0

        // 用于记录当前连续非 null 子列表的长度
        var currentLength = 0

        // 用于记录找到的最长连续非 null 子列表的起始索引
        var maxLengthStartIndex = 0

        // 用于记录找到的最长连续非 null 子列表的长度
        var maxLength = 0

        // 遍历整个列表
        for (i in dataList.indices) {
            val data = dataList[i]
            if (data.closePrice > 0) {
                // 如果当前元素不为 -1
                if (currentLength == 0) {
                    // 如果是新一轮连续的开始
                    currentStartIndex = i
                }
                currentLength++
            } else {
                // 如果当前元素为 null，连续中断

                // 检查当前连续子列表是否比已记录的最长子列表更长
                if (currentLength > maxLength) {
                    maxLength = currentLength
                    maxLengthStartIndex = currentStartIndex
                }
                // 重置当前连续长度
                currentLength = 0
            }
        }

        // 循环结束后，需要再次检查最后一个连续子列表
        if (currentLength > maxLength) {
            maxLength = currentLength
            maxLengthStartIndex = currentStartIndex
        }

        // 根据最长长度和起始索引提取子列表
        if (maxLength > 0) {
            // 使用 subList 提取结果，并强制转换为 List<ChartData>
            // subList(fromIndex, toIndex) 的 toIndex 是不包含的，所以是 maxLengthStartIndex + maxLength
            return dataList.subList(maxLengthStartIndex, maxLengthStartIndex + maxLength)
        }

        // 如果没有找到非 null 元素，则返回空列表
        return emptyList()
    }

    /**
     * 将时间戳 (秒) 转换为 +8 区的日期字符串 (yyyy-MM-dd)。
     * @param timestampSecs Unix 时间戳（秒）。
     * @return 格式化的日期字符串。
     */
    fun timestampToDate(timestampSecs: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("GMT+08:00")
        val timestampMillis = timestampSecs * 1000L
        return formatter.format(timestampMillis)
    }

    /**
     * 将符合特定格式和时区的日期字符串反转为秒级 Unix 时间戳。
     *
     * @param dateString 符合 "yyyy-MM-dd" 格式的日期字符串（例如："2025-11-10"）。
     * @return 对应的秒级 Unix 时间戳 (Long)。
     * @throws java.text.ParseException 如果日期字符串格式不正确，则抛出异常。
     */
    fun dateToTimestamp(dateString: String): Long {
        // 1. 定义与原方法完全相同的格式化器
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("GMT+08:00")
        val date = formatter.parse(dateString)
        val timestampMillis = date!!.time
        return timestampMillis / 1000L
    }

    fun findBestKLineDataList(kLineData: List<KLineData>): List<KLineData> {
        var kLineData = kLineData
        if ((kLineData.firstOrNull()?.date?.split("-")?.getOrNull(1)?.toIntOrNull() ?: 0) > 3) {
            kLineData = kLineData.subList(kLineData.indexOfLast { it.date.startsWith(kLineData.first().date.split("-")[0]) } + 1, kLineData.size)
        }
        return kLineData
    }

    fun getSinaKLineData(symbol: SymbolData, findBestData: Boolean = false, useLocalData: Boolean = false, datalen: Int = 1): List<KLineData> {
        var json: String? = null
        if (useLocalData) {
            json = runCatching { File("data", "${symbol.code}.${symbol.d}.json").readText() }.getOrNull()
        }
        if (json.isNullOrEmpty()) {
            val api =
                "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=${symbol.code}&scale=${symbol.scale * symbol.d}&ma=no&datalen=$datalen"
            json = httpGet(
                urlString = api,
                // headMap = mapOf("Referer" to "https://finance.sina.com.cn", "host" to "hq.sinajs.cn")
            )
        }
        if (!json.isNullOrEmpty()) {
            val kLineData = parseSinaKLineData(json)
            return if (findBestData) {
                findBestKLineDataList(kLineData)
            } else {
                kLineData
            }
        }
        return emptyList()
    }

    /**
     * 解析原始 JSON 字符串，提取时间戳和收盘价，并进行转换封装。
     * @param json 完整的 JSON 字符串。
     * @return ChartData 对象的列表。
     */
    fun parseSinaKLineData(json: String): List<KLineData> {
        val resultList = mutableListOf<KLineData>()

        try {
            val jsonArray = JSONArray(json)
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
        return findLongestNonNullSublist(resultList)
    }

    /**
     * 执行完全同步阻塞的 GET 请求。
     * 该函数在调用它的线程中执行所有网络 I/O，直到响应返回。
     *
     * @param urlString 目标 URL 字符串。
     * @return 成功时返回 JSON 字符串，失败时返回 null。
     */
    fun httpGet(urlString: String, headMap: Map<String, String>? = null): String? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000 // 连接超时 (毫秒)
            connection.readTimeout = 5000    // 读取超时 (毫秒)
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36"
            )
            headMap?.forEach {
                connection.setRequestProperty(it.key, it.value)
            }
            // 发起连接并获取响应码，这一步是阻塞的
            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 响应码为 200，读取数据流，这一步也是阻塞的
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?

                // 循环读取所有响应内容
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                return response.toString()
            } else {
                System.err.println("HTTP GET 请求失败，响应码: $responseCode")
                return null
            }
        } catch (e: Exception) {
            System.err.println("网络请求发生异常: ${e.message}")
            e.printStackTrace()
            return null
        } finally {
            // 确保连接被关闭，释放资源
            connection?.disconnect()
        }
    }
}