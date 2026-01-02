package com.example.myapplication.data

/**
 * 封装日期和收盘价的数据模型
 * @param date +8区 (Asia/Shanghai) 的日期字符串，格式为 yyyy-MM-dd
 * @param openPrice 四舍五入保留小数点后3位的收盘价
 * @param highPrice 四舍五入保留小数点后3位的收盘价
 * @param lowPrice 四舍五入保留小数点后3位的收盘价
 * @param closePrice 四舍五入保留小数点后3位的收盘价
 * @param volume 成交量
 */
data class KLineData(
    val date: String,
    val openPrice: Double = -1.0,
    val highPrice: Double = -1.0,
    val lowPrice: Double = -1.0,
    val closePrice: Double = -1.0,
    val volume: Long = -1,
)