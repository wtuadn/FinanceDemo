package com.example.myapplication


// 均线数据
data class MACrossData(
    val date: String,
    val closePrice: Double, //收盘价
    val volume: Long, //成交量
    val value: Double?, // MA值，由于前N-1天无法计算，可能为null
)