package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.data.SymbolData
import com.example.myapplication.data.TradeSignal
import com.example.myapplication.data.TradeSignalData
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.utils.MACrossUtils
import com.example.myapplication.utils.MACrossUtils.MAType
import com.example.myapplication.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

// 辅助扩展函数：将日期字符串转换为时间戳（假设日期格式是 YYYY-MM-DD）
fun String.toTimestamp(): Long {
    // 实际应用中应该使用 SimpleDateFormat 或 DateTimeFormatter
    // 此处简化处理：假设格式正确
    val parts = this.split("-").map { it.toLong() }
    // 转换为一个可以排序的数字，如 YYYYMMDD
    return parts[0] * 10000 + parts[1] * 100 + parts[2]
}

// 1. 新的数据状态类：包含 Symbol 数据、信号结果，以及当前的加载状态
data class SymbolItemState(
    val symbolData: SymbolData,
    val tradeSignalData: TradeSignalData? = null,
    val isLoading: Boolean = false, // 正在加载中
    val isCompleted: Boolean = false, // 加载已完成
) {
    val shouldShowSignal: Boolean = tradeSignalData != null
    val isTodaySignal: Boolean = tradeSignalData?.date == Utils.timestampToDate(System.currentTimeMillis() / 1000)
    val isBuySignal: Boolean = tradeSignalData?.tradeSignal == TradeSignal.BUY
    val isSellSignal: Boolean = tradeSignalData?.tradeSignal == TradeSignal.SELL

    // 用于排序：今天有信号 > 有信号 > 无信号。信号越新越靠前。
    fun getSortPriority(): Long {
        if (isError()) {
            return Long.MAX_VALUE
        }
        return if (isTodaySignal) {
            tradeSignalData!!.date.toTimestamp() + (symbolData.countlyPercentage * 100000).toLong()
        } else if (tradeSignalData != null) {
            tradeSignalData.date.toTimestamp()
        } else {
            // 0 表示没有信号，排在时间戳后面
            0L
        }
    }

    fun getTradeTextColor(): Color = if (isTodaySignal) {
        when {
            isBuySignal -> Color.Red // 红色
            isSellSignal -> Color.Green // 绿色
            else -> Color.Unspecified
        }
    } else if (isError()) {
        Color.Red
    } else {
        Color.Unspecified
    }

    private fun isError(): Boolean {
        return tradeSignalData?.date?.contains("empty", ignoreCase = true) == true
    }
}

class MainActivity : ComponentActivity() {
    // 原始数据列表（假设来自某个地方）
    private val symbols = listOf(
        SymbolData("sz159869", "游戏ETF", 240, 1, 10, 20, MAType.SMA, 0.170, -0.150, 0.251, 0.0015, -0.036),
        SymbolData("sz159852", "软件ETF", 240, 1, 20, 25, MAType.SMA, 0.190, 0.000, 0.082, 0.0029, -0.003),
        SymbolData("sh516510", "云计算ETF", 240, 5, 1, 25, MAType.SMA, 0.170, 0.000, 0.213, 0.0012, 0.000),
        SymbolData("sz159998", "计算机ETF", 240, 5, 1, 25, MAType.EMA, 0.090, -0.020, 0.088, 0.0006, 0.000),
        SymbolData("sh515400", "大数据ETF", 240, 1, 1, 5, MAType.EMA, 0.180, -0.020, 0.146, 0.0044, 0.000),
        SymbolData("sh601398", "工商银行", 240, 5, 15, 20, MAType.SMA, 0.140, 0.000, 0.130, 0.0006, -0.004),
        SymbolData("sh600036", "招商银行", 240, 1, 20, 30, MAType.SMA, 0.070, -0.050, 0.378, 0.0009, -0.015),
        SymbolData("sh513120", "港服创新药ETF", 240, 1, 1, 20, MAType.SMA, 0.160, -0.030, 0.140, 0.0018, -0.014),
        SymbolData("sh515790", "光伏ETF", 240, 1, 15, 40, MAType.SMA, 0.130, 0.000, 0.179, 0.0022, 0.000),
        SymbolData("sh513550", "港股通50ETF", 240, 5, 20, 25, MAType.SMA, 0.080, 0.000, 0.204, 0.0018, 0.000),
        SymbolData("sh512710", "军工龙头ETF", 240, 1, 1, 10, MAType.EMA, 0.140, -0.020, 0.121, 0.0035, -0.036),
        SymbolData("sz159227", "航空航天ETF", 240, 1, 5, 10, MAType.SMA, 0.010, -0.010, 0.028, 0.0007, -0.018),
        SymbolData("sz159218", "卫星产业ETF", 240, 1, 5, 10, MAType.EMA, 0.030, 0.000, 0.033, 0.0012, -0.007),
        SymbolData("sz159813", "半导体ETF", 240, 1, 30, 40, MAType.SMA, 0.200, 0.000, 0.064, 0.0010, -0.005),
        SymbolData("sz159713", "稀土ETF", 240, 1, 20, 25, MAType.EMA, 0.120, 0.000, 0.184, 0.0021, 0.000),
        SymbolData("sz159985", "豆粕ETF", 240, 5, 1, 15, MAType.EMA, 0.000, -0.130, 0.335, 0.0004, 0.000),
        SymbolData("sh561330", "矿业ETF", 240, 1, 30, 40, MAType.SMA, 0.170, 0.000, 0.215, 0.0029, 0.000),
        SymbolData("sh513400", "道琼斯ETF", 240, 1, 25, 40, MAType.SMA, 0.050, 0.000, 0.056, 0.0005, -0.006),
        SymbolData("sh510230", "金融ETF", 240, 1, 1, 10, MAType.SMA, 0.120, 0.000, 0.052, 0.0034, 0.000),
        SymbolData("sz159851", "金融科技ETF", 240, 5, 10, 20, MAType.SMA, 0.060, 0.000, 0.243, 0.0017, 0.000),
        SymbolData("sh516860", "金融科技ETF", 240, 5, 5, 20, MAType.EMA, 0.060, -0.040, 0.265, 0.0008, 0.000),
        SymbolData("sh512010", "医药ETF", 240, 5, 20, 25, MAType.SMA, 0.170, 0.000, 0.273, 0.0007, 0.000),
        SymbolData("sz159766", "旅游ETF", 240, 1, 20, 25, MAType.SMA, 0.130, 0.000, 0.026, 0.0027, -0.003),
        SymbolData("sh588790", "科创AIETF", 240, 5, 5, 10, MAType.SMA, 0.000, 0.000, 0.177, 0.0030, 0.000),
        SymbolData("sh513310", "中韩半导体ETF", 240, 1, 10, 30, MAType.SMA, 0.160, -0.030, 0.209, 0.0028, 0.000),
        SymbolData("sh588220", "科创100ETF基金", 240, 1, 1, 40, MAType.EMA, 0.060, 0.000, 0.166, 0.0023, 0.000),
        SymbolData("sh588000", "科创50ETF", 240, 5, 1, 5, MAType.EMA, 0.160, 0.000, 0.073, 0.0012, 0.000),
        SymbolData("sz159755", "电池ETF", 240, 1, 1, 20, MAType.EMA, 0.160, 0.000, 0.167, 0.0035, 0.000),
        SymbolData("sh513090", "香港证券ETF", 240, 1, 1, 5, MAType.EMA, 0.140, -0.020, 0.153, 0.0060, -0.009),
        SymbolData("sh562500", "机器人ETF", 240, 5, 1, 5, MAType.SMA, 0.080, -0.010, 0.071, 0.0012, -0.019),
        SymbolData("sz159915", "易方达创业板ETF", 240, 5, 1, 15, MAType.EMA, 0.120, 0.000, 0.083, 0.0009, -0.018),
        SymbolData("sh515050", "5G通信ETF", 240, 1, 20, 25, MAType.SMA, 0.200, 0.000, 0.225, 0.0058, -0.041),
        SymbolData("sh513820", "港股红利ETF", 240, 5, 1, 5, MAType.SMA, 0.010, 0.000, 0.096, 0.0014, 0.000),
        SymbolData("sz159201", "华夏国证自由现金流ETF", 240, 1, 1, 5, MAType.SMA, 0.000, -0.030, 0.128, 0.0010, -0.004),
        SymbolData("sz159545", "恒生红利低波ETF", 240, 1, 10, 15, MAType.SMA, 0.010, 0.000, 0.083, 0.0011, -0.006),
        SymbolData("sh513130", "恒生科技ETF", 240, 5, 30, 40, MAType.SMA, 0.010, 0.000, 0.221, 0.0009, -0.012),
        SymbolData("sz159892", "恒生医药ETF", 240, 5, 1, 5, MAType.SMA, 0.120, 0.000, 0.097, 0.0015, -0.014),
        SymbolData("sz159941", "纳指ETF广发", 240, 5, 5, 10, MAType.EMA, 0.000, -0.080, 0.648, 0.0007, -0.020),
        SymbolData("sh518880", "黄金ETF", 240, 1, 1, 5, MAType.SMA, 0.050, -0.070, 0.564, 0.0006, -0.066),
    ).sortedByDescending { it.countlyPercentage }
    // .subList(0,1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val coroutineScope = rememberCoroutineScope()

                // 使用 SymbolItemState 存储状态
                var symbolItemStates by remember {
                    mutableStateOf(symbols.map { SymbolItemState(it) })
                }

                var isGlobalLoading by remember { mutableStateOf(false) }
                var selectedSymbol by remember { mutableStateOf<SymbolItemState?>(null) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding() // 添加系统栏内边距
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // 列表区域
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            itemsIndexed(symbolItemStates) { index, item ->
                                SymbolRow(
                                    index = index,
                                    itemState = item,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onClick = { selectedSymbol = item },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 底部按钮
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isGlobalLoading = true
                                    fetchTradeSignalsSequentially(
                                        initialList = symbolItemStates.map { it.symbolData },
                                        onUpdate = { updatedList ->
                                            symbolItemStates = updatedList // 实时更新列表
                                        },
                                        onComplete = {
                                            isGlobalLoading = false
                                            symbolItemStates = symbolItemStates.sortedByDescending { it.getSortPriority() }
                                        }
                                    )
                                }
                            },
                            enabled = !isGlobalLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            if (isGlobalLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.Black
                                )
                            } else {
                                Text(text = "刷新交易信号")
                            }
                        }
                    }
                    // 详细信息对话框
                    if (selectedSymbol != null) {
                        SymbolDetailDialog(
                            symbolItemState = selectedSymbol!!,
                            onDismiss = { selectedSymbol = null }
                        )
                    }
                }
            }
        }
    }

    // 2. 顺序获取和实时更新状态的挂起函数
    private suspend fun fetchTradeSignalsSequentially(
        initialList: List<SymbolData>,
        onUpdate: (List<SymbolItemState>) -> Unit,
        onComplete: () -> Unit
    ) {
        val mutableList = initialList.map { SymbolItemState(it) }.toMutableList()
        // 初始更新，清除之前所有状态
        onUpdate(mutableList)

        withContext(Dispatchers.IO) {
            for (index in initialList.indices) {
                // 1. 设置当前 Item 为加载中
                mutableList[index] = mutableList[index].copy(isLoading = true, isCompleted = false)
                withContext(Dispatchers.Main) {
                    // 必须切回主线程更新 UI 状态和排序
                    onUpdate(mutableList.toList())
                }

                val symbol = initialList[index]

                val tradeSignal = try {
                    MACrossUtils.getTradeSignal(symbol)
                } catch (e: Exception) {
                    Log.e("MainActivity", "获取交易信号失败: ${e.message}", e)
                    null
                }

                // 2. 更新当前 Item 为加载完成
                val completedItem = mutableList[index].copy(
                    isLoading = false,
                    isCompleted = true,
                    tradeSignalData = tradeSignal
                )
                mutableList[index] = completedItem

                withContext(Dispatchers.Main) {
                    onUpdate(mutableList.toList())
                }
                delay(Utils.httpDelay)
            }
            onComplete()
        }
    }

    // 3. 列表行 Item UI 组件
    @Composable
    fun SymbolRow(
        index: Int,
        itemState: SymbolItemState,
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
    ) {
        val symbol = itemState.symbolData

        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            onClick = onClick ?: {},
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${index + 1}.${symbol.code} ${symbol.desc}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "d=${symbol.d} mdd=${Utils.getPercentageString(symbol.mdd)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = itemState.getTradeTextColor(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "countlyPercentage=${Utils.getPercentageString(symbol.countlyPercentage)}" +
                            "\ndailyPercentage=${Utils.getPercentageString(symbol.dailyPercentage)}",
                        fontSize = 12.sp,
                    )
                    if (itemState.tradeSignalData != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = itemState.tradeSignalData.date,
                            fontSize = 12.sp,
                            color = itemState.getTradeTextColor(),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // 右侧状态和信号显示区域
                Box(
                    modifier = Modifier.size(56.dp), // 固定大小
                    contentAlignment = Alignment.Center
                ) {
                    if (itemState.isLoading) {
                        // 状态 1: 正在加载
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else if (itemState.isCompleted) {
                        // 状态 2: 加载完成，显示信号或无信号
                        if (itemState.shouldShowSignal) {
                            val signalText = when (itemState.tradeSignalData?.tradeSignal) {
                                TradeSignal.BUY -> "📈 买入"
                                TradeSignal.SELL -> "📉 卖出"
                                else -> "无"
                            }

                            // 显示信号
                            Text(
                                text = signalText,
                                fontSize = 14.sp,
                                color = itemState.getTradeTextColor(),
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            // 状态 3: 加载完成，无信号
                            Text(
                                text = "✔️",
                                fontSize = 18.sp,
                            )
                        }
                    } else {
                        // 状态 4: 等待加载
                        Text(
                            text = "...",
                            fontSize = 18.sp,
                        )
                    }
                }
            }
        }
    }

    // 4. 详细信息对话框
    @Composable
    fun SymbolDetailDialog(
        symbolItemState: SymbolItemState,
        onDismiss: () -> Unit,
    ) {
        val symbol = symbolItemState.symbolData
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .wrapContentHeight()
                    .padding(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                onClick = onDismiss,
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "$symbol"
                    )
                    getBacktestLog(symbol)?.also {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = it.drop(it.indexOf("\n"))
                        )
                    }
                }
            }
        }
    }

    private fun getBacktestLog(symbolData: SymbolData): String? {
        return readAssetFile(this, "backtest.txt")
            .split("---")
            .find { it.contains(symbolData.code) }
    }

    private fun readAssetFile(context: Context, fileName: String): String =
        // 使用 try-use 确保 InputStream 在操作完成后自动关闭
        context.assets.open(fileName).use { inputStream ->
            InputStreamReader(inputStream).use { inputStreamReader ->
                BufferedReader(inputStreamReader).use { bufferedReader ->
                    // 读取所有行并合并成一个字符串
                    bufferedReader.readText()
                }
            }
        }
}