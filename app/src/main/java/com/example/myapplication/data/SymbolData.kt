package com.example.myapplication.data

import com.example.myapplication.utils.MACrossUtils.MAType
import com.example.myapplication.utils.Utils

/**
 * Created by wtuadn on 2025/12/22.
 */
data class SymbolData(
    val code: String,
    val desc: String,
    var scale: Int = 240,
    var d: Int = 1,
    var shortMA: Int = 1,
    var longMA: Int = 20,
    var maType: MAType = MAType.SMA,
    var upCrossDiffRate: Double = 0.000,
    var downCrossDiffRate: Double = 0.000,
    var countlyPercentage: Double = 0.0, // 平均次涨幅
    var dailyPercentage: Double = 0.0, // 平均天涨幅
    var mdd: Double = 0.0, // 最大本金损失率
){
    override fun toString(): String {
        return "code=$code, " +
            "desc=$desc, " +
            "scale=$scale, " +
            "d=$d, " +
            "shortMA=$shortMA, " +
            "longMA=$longMA, " +
            "maType=$maType, " +
            "upCrossDiffRate=${Utils.getPercentageString(upCrossDiffRate)}, " +
            "downCrossDiffRate=${Utils.getPercentageString(downCrossDiffRate)}, " +
            "countlyPercentage=${Utils.getPercentageString(countlyPercentage)}, " +
            "dailyPercentage=${Utils.getPercentageString(dailyPercentage)}, " +
            "mdd=${Utils.getPercentageString(mdd)}"
    }
}
