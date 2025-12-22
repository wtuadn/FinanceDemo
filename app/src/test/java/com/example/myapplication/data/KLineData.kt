package com.example.myapplication.data

/**
 * 封装日期和收盘价的数据模型
 * @param date +8区 (Asia/Shanghai) 的日期字符串，格式为 yyyy-MM-dd
 * @param closePrice 四舍五入保留小数点后3位的收盘价
 * @param volume 成交量
 */
data class KLineData(
    val date: String,
    val openPriceStr: String = "",
    val closePriceStr: String = "",
    val volume: Long = -1,
) {
    val openPrice: Double = openPriceStr.toDoubleOrNull() ?: -1.0
    val closePrice: Double = closePriceStr.toDoubleOrNull() ?: -1.0

    /**
     * 根据 K 线颜色计算红柱量和绿柱量
     */
    val redVolume: Long
        get() = if (closePrice > openPrice) volume else 0L

    val greenVolume: Long
        get() = if (closePrice < openPrice) volume else 0L

    /**
     * K线颜色：true 为红 (收 > 开)，false 为绿 (收 < 开)，null 为平盘
     */
    val isRedCandle: Boolean?
        get() = if (closePrice > openPrice) true else if (closePrice < openPrice) false else null
}

// 每次买卖都是all in
// 收盘价>开盘价时成交量是红柱，收盘价<开盘价时成交量是绿柱，
// EXPMA1>EXPMA5且红柱涨一定百分比买入，
// 买入后，EXPMA1>EXPMA5、EXPMA10、EXPMA20时,EXPMA1<EXPMA5且绿柱涨一定百分比卖出，或者EXPMA1跌一定百分比卖出
// 卖出后，EXPMA1没有比EXPMA5、EXPMA10、EXPMA20都大时，红柱跌一定百分比卖出，或者绿柱涨一定百分比卖出
// 写一个VolumeUtils.kt方法：以最高效率计算出总收益率最高的参数组合，并输出总收益率、平均年收益率、总最大涨跌幅、每年收益率、每年最大涨跌幅