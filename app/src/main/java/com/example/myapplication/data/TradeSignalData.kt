package com.example.myapplication.data

/**
 * 交易信号
 */
data class TradeSignalData(
    val tradeSignal: TradeSignal,
    val date: String,
){
    fun getFirstDate(): String{
        return date.split(" ")[0]
    }

    override fun toString(): String {
        return "$date $tradeSignal"
    }
}