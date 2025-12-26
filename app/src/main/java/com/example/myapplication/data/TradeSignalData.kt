package com.example.myapplication.data

/**
 * 交易信号
 */
data class TradeSignalData(
    val tradeSignal: TradeSignal,
    val date: String,
){
    override fun toString(): String {
        return "$date $tradeSignal"
    }
}