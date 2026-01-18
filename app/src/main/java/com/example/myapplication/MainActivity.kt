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
        SymbolData("sh512880", "证券ETF", 240, 1, 17, 27, 47, MAType.RSI, 0.000, 0.000, 0.046, 0.00217, -0.053),
        SymbolData("sz159326", "电网设备ETF", 240, 1, 5, 36, 46, MAType.MACD, 0.100, -0.080, 0.166, 0.00230, -0.031),
        SymbolData("sh563360", "A500ETF", 240, 1, 60, 70, 0, MAType.SMA, 0.000, 0.000, 0.175, 0.00160, -0.012),
        SymbolData("sh513500", "标普500ETF", 240, 1, 5, 55, 0, MAType.OBV, 0.140, -0.030, 0.126, 0.00080, -0.047),
        SymbolData("sh510500", "中证500ETF", 240, 5, 28, 2, 3, MAType.SKDJ, 0.050, 0.000, 0.068, 0.00080, -0.064),
        SymbolData("sh510050", "上证50ETF", 240, 1, 17, 27, 67, MAType.RSI, 0.000, 0.000, 0.072, 0.00096, -0.076),
        SymbolData("sh510300", "沪深300ETF", 240, 1, 28, 33, 24, MAType.SKDJ, 0.080, -0.050, 0.074, 0.00057, -0.071),
        SymbolData("sh512040", "价值100ETF", 240, 1, 15, 20, 0, MAType.OBV, 0.090, -0.030, 0.105, 0.00061, -0.077),
        SymbolData("sz159883", "医疗器械ETF", 240, 1, 7, 17, 42, MAType.RSI, 0.000, 0.000, 0.088, 0.00482, -0.041),
        SymbolData("sz159928", "消费ETF", 240, 5, 51, 56, 11, MAType.MACD, 0.000, 0.000, 0.146, 0.00136, -0.067),
        SymbolData("sh512980", "传媒ETF", 240, 1, 33, 2, 3, MAType.SKDJ, 0.100, -0.010, 0.120, 0.00242, -0.048),
        SymbolData("sz159869", "游戏ETF", 240, 5, 2, 18, 3, MAType.SKDJ, 0.000, 0.000, 0.246, 0.00229, -0.059),
        SymbolData("sz159852", "软件ETF", 240, 1, 1, 5, 0, MAType.EMA, 0.070, -0.040, 0.205, 0.00276, -0.058),
        SymbolData("sh516510", "云计算ETF", 240, 1, 53, 2, 4, MAType.SKDJ, 0.080, -0.030, 0.261, 0.00283, -0.066),
        SymbolData("sz159998", "计算机ETF", 240, 1, 43, 2, 14, MAType.SKDJ, 0.100, -0.060, 0.168, 0.00176, -0.067),
        SymbolData("sh515400", "大数据ETF", 240, 1, 1, 5, 0, MAType.EMA, 0.070, -0.040, 0.181, 0.00291, -0.038),
        SymbolData("sh601398", "工商银行", 240, 1, 30, 40, 0, MAType.OBV, 0.190, -0.020, 0.076, 0.00063, -0.037),
        SymbolData("sh600036", "招商银行", 240, 1, 10, 25, 0, MAType.SMA, 0.090, -0.060, 0.179, 0.00094, -0.062),
        SymbolData("sh513120", "港股创新药ETF", 240, 1, 48, 3, 19, MAType.SKDJ, 0.010, -0.060, 0.317, 0.00218, -0.077),
        SymbolData("sh515790", "光伏ETF", 240, 1, 30, 35, 0, MAType.SMA, 0.040, 0.000, 0.180, 0.00249, -0.028),
        SymbolData("sh513550", "港股通50ETF", 240, 5, 21, 41, 16, MAType.MACD, 0.100, -0.030, 0.140, 0.00124, -0.049),
        SymbolData("sh512710", "军工龙头ETF", 240, 1, 7, 17, 37, MAType.RSI, 0.000, 0.000, 0.067, 0.00579, -0.039),
        SymbolData("sz159227", "航空航天ETF", 240, 1, 2, 3, 19, MAType.SKDJ, 0.100, -0.050, 0.211, 0.00397, -0.033),
        SymbolData("sz159218", "卫星产业ETF", 240, 1, 3, 2, 3, MAType.SKDJ, 0.000, -0.070, 0.565, 0.00719, -0.016),
        SymbolData("sz159813", "半导体ETF", 240, 5, 2, 3, 39, MAType.SKDJ, 0.000, -0.010, 0.147, 0.00116, -0.082),
        SymbolData("sz159713", "稀土ETF", 240, 1, 1, 5, 0, MAType.SMA, 0.080, -0.010, 0.139, 0.00416, -0.048),
        SymbolData("sz159985", "豆粕ETF", 240, 5, 53, 2, 4, MAType.SKDJ, 0.000, -0.040, 0.142, 0.00070, -0.037),
        SymbolData("sh561330", "矿业ETF", 240, 1, 30, 200, 0, MAType.OBV, 0.000, -0.190, 0.371, 0.00404, -0.016),
        SymbolData("sh513400", "道琼斯ETF", 240, 1, 13, 8, 39, MAType.SKDJ, 0.050, -0.060, 0.144, 0.00117, -0.013),
        SymbolData("sh510230", "金融ETF", 240, 1, 35, 150, 0, MAType.OBV, 0.100, -0.020, 0.075, 0.00116, -0.098),
        SymbolData("sh516860", "金融科技ETF", 240, 1, 53, 2, 4, MAType.SKDJ, 0.080, 0.000, 0.161, 0.00435, -0.042),
        SymbolData("sh512010", "医药ETF", 240, 5, 26, 51, 5, MAType.MACD, 0.030, -0.060, 0.126, 0.00090, -0.047),
        SymbolData("sz159766", "旅游ETF", 240, 5, 16, 36, 11, MAType.MACD, 0.040, -0.100, 0.116, 0.00152, -0.057),
        SymbolData("sh588790", "科创AIETF", 240, 1, 2, 2, 54, MAType.SKDJ, 0.000, -0.040, 0.351, 0.00339, -0.061),
        SymbolData("sh513310", "中韩半导体ETF", 240, 1, 1, 20, 0, MAType.SMA, 0.040, -0.020, 0.252, 0.00205, -0.034),
        SymbolData("sh588220", "科创100ETF基金", 240, 1, 13, 3, 49, MAType.SKDJ, 0.090, -0.080, 0.210, 0.00242, -0.052),
        SymbolData("sh588000", "科创50ETF", 240, 1, 60, 80, 0, MAType.OBV, 0.170, 0.000, 0.162, 0.00149, -0.038),
        SymbolData("sz159755", "电池ETF", 240, 1, 46, 56, 5, MAType.MACD, 0.090, 0.000, 0.156, 0.00267, -0.097),
        SymbolData("sh513090", "香港证券ETF", 240, 1, 48, 2, 3, MAType.SKDJ, 0.090, -0.040, 0.201, 0.00256, -0.062),
        SymbolData("sh562500", "机器人ETF", 240, 1, 48, 3, 9, MAType.SKDJ, 0.100, 0.000, 0.187, 0.00307, -0.041),
        SymbolData("sz159915", "易方达创业板ETF", 240, 5, 3, 2, 44, MAType.SKDJ, 0.010, 0.000, 0.121, 0.00084, -0.082),
        SymbolData("sh515050", "5G通信ETF", 240, 1, 18, 8, 19, MAType.SKDJ, 0.010, -0.100, 0.249, 0.00149, -0.112),
        SymbolData("sz159201", "华夏国证自由现金流ETF", 240, 1, 42, 57, 62, MAType.RSI, 0.000, 0.000, 0.144, 0.00183, 0.000),
        SymbolData("sh512890", "红利低波ETF", 240, 5, 12, 47, 72, MAType.RSI, 0.000, 0.000, 0.152, 0.00089, -0.069),
        SymbolData("sh515100", "红利低波100ETF", 240, 1, 3, 8, 3, MAType.SKDJ, 0.070, -0.100, 0.109, 0.00067, -0.093),
        SymbolData("sh515450", "红利低波50ETF", 240, 1, 8, 2, 59, MAType.SKDJ, 0.090, -0.090, 0.102, 0.00063, -0.061),
        SymbolData("sh513820", "港股红利ETF", 240, 1, 2, 2, 54, MAType.SKDJ, 0.100, -0.070, 0.129, 0.00139, -0.027),
        SymbolData("sz159545", "恒生红利低波ETF", 240, 1, 43, 2, 4, MAType.SKDJ, 0.030, -0.040, 0.150, 0.00164, -0.025),
        SymbolData("sh513130", "恒生科技ETF", 240, 1, 13, 23, 3, MAType.SKDJ, 0.060, 0.000, 0.139, 0.00166, -0.048),
        SymbolData("sz159892", "恒生医药ETF", 240, 5, 43, 2, 3, MAType.SKDJ, 0.060, 0.000, 0.146, 0.00205, -0.038),
        SymbolData("sz159941", "纳指ETF广发", 240, 1, 58, 8, 9, MAType.SKDJ, 0.000, -0.030, 0.189, 0.00088, -0.059),
        SymbolData("sh518880", "黄金ETF", 240, 1, 22, 62, 77, MAType.RSI, 0.000, 0.000, 0.143, 0.00046, -0.127),
    )
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

                var loadingD by remember { mutableStateOf<Int?>(null) } // null:不在加载, 1:加载d=1, 5:加载d=5, 0:加载全部
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp) // 减小间距以容纳三个按钮
                        ) {
                            // 刷新 D=1 按钮
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        loadingD = 1
                                        fetchTradeSignalsSequentially(
                                            initialList = symbols.filter { it.d == 1 },
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
                                    Text(text = "刷新 d=1", fontSize = 12.sp) // 减小字体
                                }
                            }

                            // 刷新 D=5 按钮
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        loadingD = 5
                                        fetchTradeSignalsSequentially(
                                            initialList = symbols.filter { it.d == 5 },
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
                                    Text(text = "刷新 d=5", fontSize = 12.sp) // 减小字体
                                }
                            }

                            // 刷新全部按钮
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        loadingD = 0 // 使用 0 代表全部
                                        fetchTradeSignalsSequentially(
                                            initialList = symbols, // 传递整个列表
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
                                if (loadingD == 0) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.Black
                                    )
                                } else {
                                    Text(text = "刷新全部", fontSize = 12.sp) // 减小字体
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
                                if (!matches.contains(tradeSignalData.getFirstDate())) {
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