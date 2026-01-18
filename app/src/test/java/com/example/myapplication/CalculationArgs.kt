package com.example.myapplication

import com.example.myapplication.data.KLineData
import com.example.myapplication.data.SymbolData
import com.example.myapplication.utils.MACrossUtils

/**
 * 定义一个数据类来封装各种计算参数组合
 */
data class CalculationArgs(
    val maType: MACrossUtils.MAType,
    val d: List<Int>,
    val shortMAList: List<Int>,
    val longMAList: List<Int>,
    val extNList: List<Int> = listOf(0),
    val upCrossDiffRateList: List<Double> = listOf(0.0),
    val downCrossDiffRateList: List<Double> = listOf(0.0),
    val argsFilter: (symbol: SymbolData) -> Boolean = { true },
    val kLineDataFilter: (kLineData: List<KLineData>) -> List<KLineData> = { it },
)