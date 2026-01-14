package com.example.myapplication

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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

private val orangeColor = Color(0xFFFFA500)

class MainActivity : ComponentActivity() {
    private val symbols = listOf(
        SymbolData("sh512710", "军工龙头ETF", 240, 1, 1, 10, 0, MAType.EMA, 0.140, -0.020, 0.121, 0.0035, -0.036),
        SymbolData("sz159227", "航空航天ETF", 240, 1, 17, 20, 8, MAType.SKDJ, 0.010, -0.020, 0.073, 0.0012, -0.009),
        SymbolData("sh588220", "科创100ETF基金", 240, 1, 1, 40, 0, MAType.EMA, 0.060, 0.000, 0.166, 0.0023, 0.000),
        SymbolData("sh513130", "恒生科技ETF", 240, 5, 30, 40, 0, MAType.SMA, 0.010, 0.000, 0.221, 0.0009, -0.012),
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

                var loadingD by remember { mutableStateOf<Int?>(null) } // null:不在加载, 1:加载d=1, 5:加载d=5
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
                                    onItemUpdate = { updatedItem ->
                                        // 更新列表中的单个项目状态
                                        symbolItemStates = symbolItemStates.toMutableList().apply {
                                            this[index] = updatedItem
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 底部按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 刷新 D=1 按钮
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        loadingD = 1
                                        fetchTradeSignalsSequentially(
                                            initialList = symbolItemStates.map { it.symbolData }.filter { it.d == 1 },
                                            onUpdate = { updatedList ->
                                                val updatedMap = updatedList.associateBy { it.symbolData.code }
                                                symbolItemStates = symbolItemStates.map { existingItem ->
                                                    updatedMap[existingItem.symbolData.code] ?: existingItem
                                                }
                                            },
                                            onComplete = {
                                                loadingD = null
                                                symbolItemStates = symbolItemStates.sortedByDescending { it.getSortPriority() }
                                            }
                                        )
                                    }
                                },
                                enabled = loadingD == null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            ) {
                                if (loadingD == 1) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.Black
                                    )
                                } else {
                                    Text(text = "刷新 D=1")
                                }
                            }

                            // 刷新 D=5 按钮
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        loadingD = 5
                                        fetchTradeSignalsSequentially(
                                            initialList = symbolItemStates.map { it.symbolData }.filter { it.d == 5 },
                                            onUpdate = { updatedList ->
                                                val updatedMap = updatedList.associateBy { it.symbolData.code }
                                                symbolItemStates = symbolItemStates.map { existingItem ->
                                                    updatedMap[existingItem.symbolData.code] ?: existingItem
                                                }
                                            },
                                            onComplete = {
                                                loadingD = null
                                                symbolItemStates = symbolItemStates.sortedByDescending { it.getSortPriority() }
                                            }
                                        )
                                    }
                                },
                                enabled = loadingD == null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            ) {
                                if (loadingD == 5) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.Black
                                    )
                                } else {
                                    Text(text = "刷新 D=5")
                                }
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

                // 2. 更新当前 Item 为加载完成
                val completedItem = mutableList[index].copy(
                    isLoading = false,
                    isCompleted = true,
                    tradeSignalDataList = MACrossUtils.getTradeSignal(symbol, getBacktestLog(symbol))
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
        onItemUpdate: (SymbolItemState) -> Unit,
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "d=${symbol.d} mdd=${Utils.getPercentageString(symbol.mdd)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (symbol.d == 5 && itemState.isTodaySignal) orangeColor else itemState.getTradeTextColor(),
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "yearlyPercentage=${Utils.getPercentageString(symbol.yearlyPercentage)}" +
                            "\ndailyPercentage=${Utils.getPercentageString(symbol.dailyPercentage)}",
                        fontSize = 12.sp,
                    )
                    itemState.tradeSignalDataList.takeLast(3).reversed().forEachIndexed { i, tradeSignalData ->

                        var textColor = itemState.getTradeTextColor()
                        if (i > 0 || tradeSignalData.tradeSignal == TradeSignal.SELL) {
                            val backtestLog = getBacktestLog(symbol)
                            if (!backtestLog.isNullOrBlank()) {
                                // 正则表达式匹配 YYYY-MM-DD——YYYY-MM-DD 格式的日期
                                val regex = "(\\d{4}-\\d{2}-\\d{2})——(\\d{4}-\\d{2}-\\d{2})".toRegex()
                                val findAll = regex.findAll(backtestLog)
                                val matches = findAll.map { it.groupValues[1] }.toList() + findAll.map { it.groupValues[2] }
                                if (!matches.contains(tradeSignalData.date)) {
                                    textColor = orangeColor
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "$tradeSignalData",
                            fontSize = 12.sp,
                            color = textColor,
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
                                        onItemUpdate(updatedList.first())
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
                            val signalText = when (itemState.tradeSignalDataList.lastOrNull()?.tradeSignal) {
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

    private val backtestLogs by lazy {
        readAssetFile(this, "backtest.txt")
            .split("---")
    }

    private fun getBacktestLog(symbolData: SymbolData): String? {
        return backtestLogs.find { it.contains(symbolData.code) }
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
    val tradeSignalDataList: List<TradeSignalData> = emptyList(),
    val isLoading: Boolean = false, // 正在加载中
    val isCompleted: Boolean = false, // 加载已完成
) {
    val shouldShowSignal: Boolean = tradeSignalDataList != null
    val isTodaySignal: Boolean = tradeSignalDataList.lastOrNull()?.date?.startsWith(Utils.timestampToDate(System.currentTimeMillis() / 1000)) == true
    val isBuySignal: Boolean = tradeSignalDataList.lastOrNull()?.tradeSignal == TradeSignal.BUY
    val isSellSignal: Boolean = tradeSignalDataList.lastOrNull()?.tradeSignal == TradeSignal.SELL

    // 用于排序：今天有信号 > 有信号 > 无信号。信号越新越靠前。
    fun getSortPriority(): Long {
        if (isError()) {
            return Long.MAX_VALUE
        }
        return if (isTodaySignal) {
            tradeSignalDataList.last().date.toTimestamp() + (symbolData.yearlyPercentage * 100000).toLong()
        } else if (tradeSignalDataList.isNotEmpty()) {
            tradeSignalDataList.last().date.toTimestamp()
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
        return tradeSignalDataList.firstOrNull()?.date?.contains("kLineData", ignoreCase = true) == true
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