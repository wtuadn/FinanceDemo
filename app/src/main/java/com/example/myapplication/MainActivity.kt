package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.myapplication.data.SymbolData
import com.example.myapplication.data.TradeSignal
import com.example.myapplication.data.TradeSignalData
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.utils.MACrossUtils
import com.example.myapplication.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SymbolWithSignal(
    val symbolData: SymbolData,
    val tradeSignalData: TradeSignalData? = null,
) {
    val shouldShowSignal: Boolean = tradeSignalData != null
    val isTodaySignal: Boolean = tradeSignalData?.date == Utils.timestampToDate(System.currentTimeMillis() / 1000)
    val isBuySignal: Boolean = tradeSignalData?.tradeSignal == TradeSignal.BUY
    val isSellSignal: Boolean = tradeSignalData?.tradeSignal == TradeSignal.SELL
}

class MainActivity : ComponentActivity() {
    val symbols = listOf(
        SymbolData("sz159915", "易方达创业板ETF", 240, 1, 1, 25, MACrossUtils.MAType.SMA, 0.04, 0.0),
        SymbolData("sh515050", "5G通信ETF", 240, 1, 1, 20, MACrossUtils.MAType.SMA, 0.01, 0.0),
        SymbolData("sh513820", "港股红利ETF", 240, 1, 5, 10, MACrossUtils.MAType.SMA, 0.02, -0.01),
        SymbolData("sh512890", "红利低波ETF", 240, 1, 1, 15, MACrossUtils.MAType.SMA, 0.0, -0.01),
        SymbolData("sh515100", "红利低波100ETF", 240, 1, 10, 15, MACrossUtils.MAType.SMA, 0.0, 0.0),
        SymbolData("sz159201", "华夏国证自由现金流", 240, 1, 1, 5, MACrossUtils.MAType.SMA, 0.0, -0.03),
        SymbolData("sz159545", "恒生红利低波ETF", 240, 1, 5, 10, MACrossUtils.MAType.SMA, 0.01, 0.0),
        SymbolData("sh513130", "恒生科技ETF", 240, 1, 5, 30, MACrossUtils.MAType.SMA, 0.01, 0.0),
        SymbolData("sz159892", "恒生医药ETF", 240, 1, 10, 20, MACrossUtils.MAType.SMA, 0.01, -0.01),
        SymbolData("sz159941", "纳指ETF广发", 240, 1, 20, 60, MACrossUtils.MAType.SMA, 0.01, -0.05),
        SymbolData("sh518880", "黄金ETF", 240, 1, 10, 15, MACrossUtils.MAType.SMA, 0.0, 0.0),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val coroutineScope = rememberCoroutineScope()

                var symbolWithSignals by remember {
                    mutableStateOf(symbols.map { SymbolWithSignal(it) })
                }

                var isLoading by remember { mutableStateOf(false) }
                var selectedSymbol by remember { mutableStateOf<SymbolWithSignal?>(null) }

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
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(symbolWithSignals) { item ->
                                SymbolRow(
                                    symbolWithSignal = item,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onClick = { selectedSymbol = item },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    val updatedSymbols = fetchTradeSignals(symbols)
                                    symbolWithSignals = updatedSymbols
                                    isLoading = false
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            if (isLoading) {
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
                            symbolWithSignal = selectedSymbol!!,
                            onDismiss = { selectedSymbol = null }
                        )
                    }
                }
            }
        }
    }

    private suspend fun fetchTradeSignals(symbols: List<SymbolData>): List<SymbolWithSignal> {
        return withContext(Dispatchers.IO) {
            symbols.map { symbol ->
                val tradeSignal = try {
                    MACrossUtils.getTradeSignal(symbol)
                } catch (e: Exception) {
                    Log.e("MainActivity", "获取交易信号失败: ${e.message}", e)
                    null
                }
                SymbolWithSignal(symbol, tradeSignal)
            }
        }
    }
}

@Composable
fun SymbolRow(
    symbolWithSignal: SymbolWithSignal,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val (code, desc) = symbolWithSignal.symbolData
    val tradeSignalData = symbolWithSignal.tradeSignalData

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick ?: {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = code,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = desc,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(2f)
                )
            }

            if (symbolWithSignal.shouldShowSignal) {
                Spacer(modifier = Modifier.height(4.dp))

                val textColor = when {
                    symbolWithSignal.isTodaySignal && symbolWithSignal.isBuySignal -> Color.Red
                    symbolWithSignal.isTodaySignal && symbolWithSignal.isSellSignal -> Color.Green
                    else -> Color.White
                }

                val signalText = when (tradeSignalData?.tradeSignal) {
                    TradeSignal.BUY -> "买入"
                    TradeSignal.SELL -> "卖出"
                    else -> "无操作"
                }

                Text(
                    text = "交易信号: $signalText ${tradeSignalData?.date}",
                    fontSize = 12.sp,
                    color = textColor,
                    fontWeight = if (symbolWithSignal.isTodaySignal) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun SymbolDetailDialog(
    symbolWithSignal: SymbolWithSignal,
    onDismiss: () -> Unit,
) {
    val symbol = symbolWithSignal.symbolData

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = symbol.desc) },
        text = {
            Column {
                Text("$symbol")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
