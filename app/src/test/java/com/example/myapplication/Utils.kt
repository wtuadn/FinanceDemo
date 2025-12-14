package com.example.myapplication

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object Utils {
    fun calculateMACross(
        shortMADataList: List<MAData>,
        longMADataList: List<MAData>,
        upCrossMADiffRate: Double = 0.0,
        downCrossMADiffRate: Double = 0.0
    ): Triple<Double, Double, String> {
        val sb = StringBuilder()

        data class AlignedMAData(
            val date: String,
            val closePrice: Double,
            val volume: Long,
            val shortMAValue: Double,
            val longMAValue: Double,
        )
        // 过滤掉 MA 值为 null 的数据，并确保两个列表日期对齐
        val alignedMAData = shortMADataList
            .mapIndexedNotNull { index, ma1 ->
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

        sb.append("--- 有效数据时间段为：${longMADataList.firstOrNull()?.date} - ${longMADataList.lastOrNull()?.date} ---\n")
        // 存储配对的交易信号
        val tradePairs = mutableListOf<Pair<AlignedMAData, AlignedMAData>>()
        var entryData: AlignedMAData? = null // 记录金叉时的 upData

        for (i in 2 until alignedMAData.size) {
            val today = alignedMAData[i]
            val yesterday = alignedMAData[i - 1] // 昨天的 MA 交叉状态
            val yesterday2 = alignedMAData[i - 2] // 前天的 MA 交叉状态

            val todayMADiffRate = (today.shortMAValue - today.longMAValue) / today.longMAValue
            // 1. 检查金叉 (Golden Cross)
            if (
                (yesterday.shortMAValue <= yesterday.longMAValue && todayMADiffRate > upCrossMADiffRate) //上穿时超过阈值
                || (yesterday.shortMAValue > yesterday.longMAValue && todayMADiffRate > upCrossMADiffRate) //上穿后某天超过阈值
                // || (today.shortMAValue > today.longMAValue && yesterday.shortMAValue > yesterday.longMAValue && yesterday2.shortMAValue > yesterday2.longMAValue) //上穿后连续3天稳住
            ) {
                // 发生金叉
                if (entryData == null) {
                    // 记录 upData (买入信号)
                    entryData = today
                }
            }

            // 2. 检查死叉 (Death Cross)
            else if (
                (yesterday.shortMAValue >= yesterday.longMAValue && todayMADiffRate < downCrossMADiffRate) //下穿时超过阈值
                || (yesterday.shortMAValue < yesterday.longMAValue && todayMADiffRate < downCrossMADiffRate) //下穿后某天超过阈值
                || (today.shortMAValue < today.longMAValue && yesterday.shortMAValue < yesterday.longMAValue && yesterday2.shortMAValue < yesterday2.longMAValue) //下穿后连续3天稳住
            ) {
                // 发生死叉
                if (entryData != null) {
                    // 记录 downData (卖出信号)，并完成配对
                    val exitData = today
                    tradePairs.add(Pair(entryData, exitData))
                    entryData = null // 清空入场数据
                }
            }
        }

        // ------------------ 计算和打印结果 ------------------

        // 用于存储所有计算出的百分比，便于后续按年累计
        val yearlyPercentageMap = mutableMapOf<Int, MutableList<Pair<Double, String>>>()
        longMADataList.groupBy { it.date.substring(0, 4).toInt() }.forEach {
            yearlyPercentageMap.getOrPut(it.key) { mutableListOf() }
        }

        var minPercentage = 0.0
        var minPercentageDisplay = ""
        var maxPercentage = 0.0
        var maxPercentageDisplay = ""
        for (pair in tradePairs) {
            val upData = pair.first
            val downData = pair.second

            val upValue = upData.closePrice
            val downValue = downData.closePrice

            // 计算百分比: (downData.value - upData.value) / upData.value * 100%
            val percentage = (downValue - upValue) / upValue
            val percentageDisplay = "%.2f%%".format(percentage * 100)

            // 打印结果 (upData.date - downData.date: downData.value - upData.value 的百分比)
            val display = "${upData.date} - ${downData.date} : $percentageDisplay"
            // 更新最大最小回撤
            if (percentage > maxPercentage) {
                maxPercentage = percentage
                maxPercentageDisplay = display
            }
            if (percentage < minPercentage) {
                minPercentage = percentage
                minPercentageDisplay = display
            }
            // sb.append("$display\n")

            // 累计到年份映射中
            // 以金叉 (upData) 发生的年份为准进行累计
            val year = downData.date.substring(0, 4).toInt()
            yearlyPercentageMap.getOrPut(year) { mutableListOf() }.add(percentage to display)
        }

        // ------------------ 按年打印累计百分比 ------------------

        for (year in yearlyPercentageMap.keys.sorted()) {
            var minPercentage = 0.0
            var minPercentageDisplay = ""
            var maxPercentage = 0.0
            var maxPercentageDisplay = ""

            val percentages = yearlyPercentageMap[year]!!
            val cumulativePercentage = percentages?.sumOf { it.first } ?: 0.0

            percentages.forEach {
                val percentage = it.first
                val display = it.second
                // 更新最大最小回撤
                if (percentage > maxPercentage) {
                    maxPercentage = percentage
                    maxPercentageDisplay = display
                }
                if (percentage < minPercentage) {
                    minPercentage = percentage
                    minPercentageDisplay = display
                }
            }
            val cumulativeDisplay = "%.2f%%".format(cumulativePercentage * 100)
            val victoryRate = getVictoryRateString(percentages.count { it.first >= 0 }, percentages.count { it.first < 0 })
            sb.append("$year: $cumulativeDisplay 胜率：$victoryRate 最大涨幅：$maxPercentageDisplay 最大回撤：$minPercentageDisplay\n")
        }

        val allPercentageDatas = yearlyPercentageMap.flatMap { it.value }
        val victoryRate = getVictoryRateString(allPercentageDatas.count { it.first >= 0 }, allPercentageDatas.count { it.first < 0 })
        val yearlyPercentage =
            if (yearlyPercentageMap.isEmpty()) 0.0 else yearlyPercentageMap.values.flatMap { it }.sumOf { it.first } / yearlyPercentageMap.size
        sb.append(
            "--- 平均年收益${getPercentageString(yearlyPercentage)} $victoryRate 最大涨幅：$maxPercentageDisplay 最大回撤：$minPercentageDisplay ---"
        )
        return Triple(yearlyPercentage, minPercentage, sb.toString())
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

    /**
     * 根据 ChartData 列表计算 N 日简单移动平均线 (SMA)。
     *
     * @param dataList 原始 ChartData 列表（假设已按日期升序排列）。
     * @param period 均线的计算周期 N (例如：5, 10, 60)。
     * @return 包含日期和对应 MA 值的 MovingAverageData 列表。
     */
    fun calculateMAData(dataList: List<KLineData>, period: Int): List<MAData> {
        if (period <= 0) {
            throw IllegalArgumentException("均线周期 N 必须大于 0。")
        }

        // 提取所有收盘价，方便后续窗口计算
        val prices = dataList.map { it.closePrice }
        val maList = mutableListOf<MAData>()

        // 遍历所有数据点
        for (i in dataList.indices) {
            val currentDate = dataList[i].date
            val currentPrice = dataList[i].closePrice
            val currentVolume = dataList[i].volume

            // MA 计算的起始索引：必须向前追溯 (period - 1) 天
            // 例如，计算 MA5，需要从 i 向前到 i - 4，一共 5 个点。
            val startIndex = i - period + 1

            // 1. 处理前 N-1 天数据：MA值设置为 null
            if (startIndex < 0) {
                maList.add(MAData(currentDate, currentPrice, 0, null))
                continue
            }

            // 2. 提取计算窗口内的 N 个收盘价
            // Kotlin 的 subList 索引是 [fromIndex, toIndex)，所以 toIndex = i + 1
            val windowPrices = prices.subList(startIndex, i + 1)

            // 3. 计算总和
            // 使用 fold 函数求和，初始值为 0.0
            val sum = windowPrices.fold(0.0) { acc, price -> acc + price }

            // 4. 计算平均值
            val rawMa = sum / period

            // 5. 四舍五入到小数点后 3 位 (沿用之前的数据精度要求)
            val roundedMa = kotlin.math.round(rawMa * 1000.0) / 1000.0

            // 6. 添加到结果列表
            maList.add(MAData(currentDate, currentPrice, currentVolume, roundedMa))
        }

        return maList
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

    /**
     * 将收盘价四舍五入保留小数点后3位。
     * @param value 原始收盘价 (Double)。
     * @return 四舍五入后的 Double 值。
     */
    fun roundToThreeDecimalPlaces(value: Double): Double {
        // 放大 1000 倍，四舍五入，然后除以 1000
        // 注意：这里的乘法和除法会导致精度问题，但对于浮点数操作来说是常见且最简单的原生解决方案。
        // 更严谨的做法是使用 BigDecimal，但由于要求不依赖第三方库，我们使用 Double 的原生数学操作。
        return kotlin.math.round(value * 1000.0) / 1000.0
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
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
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