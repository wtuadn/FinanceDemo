package com.example.myapplication.data


// 均线数据
data class MAData(
    val kLineData: KLineData,
    val value: Double?, // MA值，由于前N-1天无法计算，可能为null
)