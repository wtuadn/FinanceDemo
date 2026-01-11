package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
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
import androidx.core.view.WindowCompat
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

class MainActivity : ComponentActivity() {
    private val symbols = listOf(
        SymbolData("sh563360", "A500ETF", 240, 5, 27, 72, 67, MAType.RSI, 0.000, 0.000, 0.179, 0.00161, -0.023),
        SymbolData("sh513500", "标普500ETF", 240, 5, 18, 13, 44, MAType.SKDJ, 0.020, -0.010, 0.096, 0.00060, -0.009),
        SymbolData("sh510500", "中证500ETF", 240, 5, 67, 42, 42, MAType.RSI, 0.000, 0.000, 0.048, 0.00574, -0.009),
        SymbolData("sh510050", "上证50ETF", 240, 5, 31, 36, 21, MAType.MACD, 0.100, 0.000, 0.056, 0.00057, -0.049),
        SymbolData("sh510300", "沪深300ETF", 240, 1, 28, 33, 24, MAType.SKDJ, 0.080, -0.050, 0.074, 0.00057, -0.071),
        SymbolData("sh512040", "价值100ETF", 240, 1, 5, 25, 0, MAType.OBV, 0.200, -0.030, 0.119, 0.00092, -0.090),
        SymbolData("sz159883", "医疗器械ETF", 240, 5, 12, 27, 27, MAType.RSI, 0.000, 0.000, 0.067, 0.00733, 0.000),
        SymbolData("sz159928", "消费ETF", 240, 5, 41, 56, 6, MAType.MACD, 0.060, -0.050, 0.169, 0.00144, -0.007),
        SymbolData("sh512980", "传媒ETF", 240, 5, 22, 32, 57, MAType.RSI, 0.000, 0.000, 0.105, 0.00245, 0.000),
        SymbolData("sz159869", "游戏ETF", 240, 5, 2, 18, 3, MAType.SKDJ, 0.050, 0.000, 0.203, 0.00243, 0.000),
        SymbolData("sz159852", "软件ETF", 240, 1, 1, 5, 0, MAType.EMA, 0.070, -0.030, 0.180, 0.00265, -0.046),
        SymbolData("sh516510", "云计算ETF", 240, 5, 1, 5, 0, MAType.SMA, 0.040, 0.000, 0.208, 0.00225, -0.038),
        SymbolData("sz159998", "计算机ETF", 240, 1, 7, 12, 82, MAType.RSI, 0.000, 0.000, 0.106, 0.00252, -0.038),
        SymbolData("sh515400", "大数据ETF", 240, 1, 1, 5, 0, MAType.EMA, 0.070, -0.030, 0.194, 0.00331, -0.030),
        SymbolData("sh601398", "工商银行", 240, 5, 58, 3, 9, MAType.SKDJ, 0.100, -0.030, 0.062, 0.00075, -0.005),
        SymbolData("sh600036", "招商银行", 240, 1, 15, 25, 0, MAType.SMA, 0.060, -0.060, 0.175, 0.00087, -0.034),
        SymbolData("sh513120", "港股创新药ETF", 240, 5, 58, 2, 4, MAType.SKDJ, 0.000, 0.000, 0.272, 0.00234, -0.046),
        SymbolData("sh515790", "光伏ETF", 240, 1, 30, 35, 0, MAType.SMA, 0.040, 0.000, 0.180, 0.00249, -0.028),
        SymbolData("sh513550", "港股通50ETF", 240, 5, 21, 36, 26, MAType.MACD, 0.090, -0.050, 0.130, 0.00143, 0.000),
        SymbolData("sh512710", "军工龙头ETF", 240, 5, 7, 17, 72, MAType.RSI, 0.000, 0.000, 0.111, 0.00159, -0.042),
        SymbolData("sz159227", "航空航天ETF", 240, 5, 7, 87, 87, MAType.RSI, 0.000, 0.000, 0.290, 0.00360, -0.011),
        SymbolData("sz159218", "卫星产业ETF", 240, 1, 12, 87, 82, MAType.RSI, 0.000, 0.000, 0.419, 0.00413, -0.015),
        SymbolData("sz159813", "半导体ETF", 240, 5, 2, 3, 39, MAType.SKDJ, 0.000, -0.010, 0.147, 0.00116, -0.082),
        SymbolData("sz159713", "稀土ETF", 240, 5, 43, 2, 4, MAType.SKDJ, 0.000, 0.000, 0.178, 0.00189, -0.058),
        SymbolData("sz159985", "豆粕ETF", 240, 5, 53, 2, 4, MAType.SKDJ, 0.000, -0.040, 0.142, 0.00070, -0.037),
        SymbolData("sh561330", "矿业ETF", 240, 1, 30, 200, 0, MAType.OBV, 0.000, -0.190, 0.371, 0.00404, -0.016),
        SymbolData("sh513400", "道琼斯ETF", 240, 1, 13, 8, 44, MAType.SKDJ, 0.080, -0.060, 0.156, 0.00118, -0.009),
        SymbolData("sh510230", "金融ETF", 240, 5, 7, 27, 57, MAType.RSI, 0.000, 0.000, 0.059, 0.00094, -0.049),
        SymbolData("sz159851", "金融科技ETF", 240, 1, 58, 2, 4, MAType.SKDJ, 0.070, 0.000, 0.152, 0.00482, -0.044),
        SymbolData("sh516860", "金融科技ETF", 240, 1, 1, 35, 0, MAType.EMA, 0.100, -0.010, 0.165, 0.00298, -0.035),
        SymbolData("sh512010", "医药ETF", 240, 5, 26, 51, 5, MAType.MACD, 0.030, -0.060, 0.126, 0.00090, -0.047),
        SymbolData("sz159766", "旅游ETF", 240, 5, 16, 36, 11, MAType.MACD, 0.040, -0.100, 0.116, 0.00152, -0.057),
        SymbolData("sh588790", "科创AIETF", 240, 1, 5, 16, 26, MAType.MACD, 0.040, 0.000, 0.297, 0.00640, 0.000),
        SymbolData("sh513310", "中韩半导体ETF", 240, 1, 12, 42, 82, MAType.RSI, 0.000, 0.000, 0.441, 0.00180, -0.058),
        SymbolData("sh588220", "科创100ETF基金", 240, 5, 47, 62, 57, MAType.RSI, 0.000, 0.000, 0.343, 0.00248, -0.069),
        SymbolData("sh588000", "科创50ETF", 240, 1, 28, 8, 4, MAType.SKDJ, 0.090, -0.010, 0.104, 0.00126, -0.044),
        SymbolData("sz159755", "电池ETF", 240, 1, 5, 30, 0, MAType.EMA, 0.020, 0.000, 0.135, 0.00155, -0.047),
        SymbolData("sh513090", "香港证券ETF", 240, 1, 1, 5, 0, MAType.EMA, 0.130, -0.020, 0.112, 0.00537, -0.029),
        SymbolData("sh562500", "机器人ETF", 240, 1, 48, 3, 9, MAType.SKDJ, 0.100, 0.000, 0.187, 0.00307, -0.041),
        SymbolData("sz159915", "易方达创业板ETF", 240, 5, 40, 50, 0, MAType.SMA, 0.110, 0.000, 0.090, 0.00108, 0.000),
        SymbolData("sh515050", "5G通信ETF", 240, 1, 8, 43, 3, MAType.SKDJ, 0.030, 0.000, 0.127, 0.00128, -0.041),
        SymbolData("sz159201", "华夏国证自由现金流ETF", 240, 1, 42, 57, 62, MAType.RSI, 0.000, 0.000, 0.144, 0.00183, 0.000),
        SymbolData("sh512890", "红利低波ETF", 240, 1, 47, 47, 62, MAType.RSI, 0.000, 0.000, 0.154, 0.00089, -0.076),
        SymbolData("sh515100", "红利低波100ETF", 240, 5, 26, 46, 11, MAType.MACD, 0.000, -0.090, 0.092, 0.00058, -0.025),
        SymbolData("sh515450", "红利低波50ETF", 240, 1, 62, 42, 57, MAType.RSI, 0.000, 0.000, 0.071, 0.00188, -0.013),
        SymbolData("sh513820", "港股红利ETF", 240, 1, 77, 52, 52, MAType.RSI, 0.000, 0.000, 0.172, 0.00312, -0.051),
        SymbolData("sz159545", "恒生红利低波ETF", 240, 1, 48, 2, 4, MAType.SKDJ, 0.020, -0.020, 0.145, 0.00172, -0.014),
        SymbolData("sh513130", "恒生科技ETF", 240, 5, 26, 31, 56, MAType.MACD, 0.020, -0.080, 0.180, 0.00217, -0.024),
        SymbolData("sz159892", "恒生医药ETF", 240, 1, 1, 40, 0, MAType.OBV, 0.020, 0.000, 0.328, 0.00426, -0.007),
        SymbolData("sz159941", "纳指ETF广发", 240, 1, 77, 42, 62, MAType.RSI, 0.000, 0.000, 0.173, 0.00149, -0.015),
        SymbolData("sh518880", "黄金ETF", 240, 1, 72, 57, 72, MAType.RSI, 0.000, 0.000, 0.185, 0.00061, 0.000),
    ).sortedByDescending { it.yearlyPercentage }
    // .subList(0, 10)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
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
                                    initItemState = item,
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
        onComplete: () -> Unit,
    ) {
        val mutableList = initialList.map { SymbolItemState(it) }.toMutableList()
        // 初始更新，清除之前所有状态
        onUpdate(mutableList.toList())

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
        initItemState: SymbolItemState,
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
    ) {
        var itemState by remember(initItemState) { mutableStateOf(initItemState) }
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "d=${symbol.d} mdd=${Utils.getPercentageString(symbol.mdd)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (symbol.d == 5 && itemState.isTodaySignal) Color(0xFFFFA500) else itemState.getTradeTextColor(),
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "yearlyPercentage=${Utils.getPercentageString(symbol.yearlyPercentage)}" +
                            "\ndailyPercentage=${Utils.getPercentageString(symbol.dailyPercentage)}",
                        fontSize = 12.sp,
                    )
                    if (itemState.tradeSignalData != null) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = itemState.tradeSignalData!!.date,
                            fontSize = 12.sp,
                            color = itemState.getTradeTextColor(),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // 右侧状态和信号显示区域
                val scope = rememberCoroutineScope()
                Box(
                    modifier = Modifier
                        .size(56.dp) // 固定大小
                        .clickable {
                            scope.launch {
                                fetchTradeSignalsSequentially(
                                    initialList = listOf(symbol),
                                    onUpdate = { updatedList ->
                                        itemState = updatedList.first()
                                    },
                                    onComplete = {
                                    }
                                )

                            }
                        },
                    contentAlignment = Alignment.Center,
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

// 1. 新的数据状态类：包含 Symbol 数据、信号结果，以及当前的加载状态
data class SymbolItemState(
    val symbolData: SymbolData,
    val tradeSignalData: TradeSignalData? = null,
    val isLoading: Boolean = false, // 正在加载中
    val isCompleted: Boolean = false, // 加载已完成
) {
    val shouldShowSignal: Boolean = tradeSignalData != null
    val isTodaySignal: Boolean = tradeSignalData?.date?.startsWith(Utils.timestampToDate(System.currentTimeMillis() / 1000)) == true
    val isBuySignal: Boolean = tradeSignalData?.tradeSignal == TradeSignal.BUY
    val isSellSignal: Boolean = tradeSignalData?.tradeSignal == TradeSignal.SELL

    // 用于排序：今天有信号 > 有信号 > 无信号。信号越新越靠前。
    fun getSortPriority(): Long {
        if (isError()) {
            return Long.MAX_VALUE
        }
        return if (isTodaySignal) {
            tradeSignalData!!.date.toTimestamp() + (symbolData.yearlyPercentage * 100000).toLong()
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
        return tradeSignalData?.date?.contains("kLineData", ignoreCase = true) == true
    }

    // 辅助扩展函数：将日期字符串转换为时间戳（假设日期格式是 YYYY-MM-DD）
    private fun String.toTimestamp(): Long {
        // 实际应用中应该使用 SimpleDateFormat 或 DateTimeFormatter
        // 此处简化处理：假设格式正确
        val parts = this.replace(" ", "-").split("-").map { it.toLongOrNull() ?: 0L }
        // 转换为一个可以排序的数字，如 YYYYMMDD
        return parts[0] * 10000 + parts[1] * 100 + parts[2]
    }
}