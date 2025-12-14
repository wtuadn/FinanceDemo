package com.example.myapplication

    /**
     * 封装日期和收盘价的数据模型
     * @param date +8区 (Asia/Shanghai) 的日期字符串，格式为 yyyy-MM-dd
     * @param closePrice 四舍五入保留小数点后3位的收盘价
     */
    data class KLineData(
        val date: String,
        val closePrice: Double,
        val volume: Long,
    )