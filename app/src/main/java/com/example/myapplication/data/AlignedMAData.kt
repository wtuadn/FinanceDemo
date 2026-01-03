package com.example.myapplication.data

data class AlignedMAData(
    val kLineData: KLineData,
    val shortMAValue: Double = 0.0, //快线
    val longMAValue: Double = 0.0, //慢线
){
    var extValue: Double = 0.0 //KDJ J值、MACD Bar
}