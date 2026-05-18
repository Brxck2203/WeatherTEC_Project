package com.project.weathertec.ui.comparison

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.project.weathertec.data.model.WeatherRecord
import com.project.weathertec.data.repository.WeatherRepository
import com.project.weathertec.ui.shared.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private fun List<Double>.safeAverage(): Double = if (isEmpty()) 0.0 else average()

private val COLOR_1 = Color(0xFF5C6BC0)  // Azul índigo
private val COLOR_2 = Color(0xFF26A69A)  // Verde teal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen() {
    val repository = remember { WeatherRepository() }
    val scope      = rememberCoroutineScope()

    var date1       by remember { mutableStateOf("") }
    var date2       by remember { mutableStateOf("") }
    var records1    by remember { mutableStateOf<List<WeatherRecord>>(emptyList()) }
    var records2    by remember { mutableStateOf<List<WeatherRecord>>(emptyList()) }
    var loading     by remember { mutableStateOf(false) }
    var error       by remember { mutableStateOf<String?>(null) }
    var compared    by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val formatter      = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val todayUtcMillis = remember {
        LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    }
    val currentYear = remember { LocalDate.now().year }

    val noFutureDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtcMillis
        override fun isSelectableYear(year: Int) = year <= currentYear
    }

    val picker1State = rememberDatePickerState(selectableDates = noFutureDates)
    var showPicker1  by remember { mutableStateOf(false) }
    val picker2State = rememberDatePickerState(selectableDates = noFutureDates)
    var showPicker2  by remember { mutableStateOf(false) }

    if (showPicker1) {
        DatePickerDialog(
            onDismissRequest = { showPicker1 = false },
            confirmButton = {
                TextButton(onClick = {
                    picker1State.selectedDateMillis?.let {
                        date1 = Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).format(formatter)
                    }
                    showPicker1 = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showPicker1 = false }) { Text("Cancelar") } }
        ) { DatePicker(state = picker1State) }
    }

    if (showPicker2) {
        DatePickerDialog(
            onDismissRequest = { showPicker2 = false },
            confirmButton = {
                TextButton(onClick = {
                    picker2State.selectedDateMillis?.let {
                        date2 = Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).format(formatter)
                    }
                    showPicker2 = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showPicker2 = false }) { Text("Cancelar") } }
        ) { DatePicker(state = picker2State) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Comparación") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle("Compara dos días")

            OutlinedTextField(
                value = date1,
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha 1") },
                placeholder = { Text("Selecciona la primera fecha") },
                leadingIcon = {
                    IconButton(onClick = { showPicker1 = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = date2,
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha 2") },
                placeholder = { Text("Selecciona la segunda fecha") },
                leadingIcon = {
                    IconButton(onClick = { showPicker2 = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    compared    = false
                    error       = null
                    loading     = true
                    selectedTab = 0
                    scope.launch {
                        try {
                            records1 = repository.fetchFromFirebase(date1)
                            records2 = repository.fetchFromFirebase(date2)
                        } catch (e: Exception) {
                            error = "Error al obtener datos: ${e.message}"
                        } finally {
                            loading  = false
                            compared = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled  = date1.length == 10 && date2.length == 10 && !loading
            ) { Text("Comparar") }

            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Cargando datos...")
                        }
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                compared -> {
                    if (records1.isEmpty() && records2.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Sin datos para las fechas seleccionadas",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        HorizontalDivider()
                        ComparisonContent(
                            date1       = date1,
                            date2       = date2,
                            records1    = records1,
                            records2    = records2,
                            selectedTab = selectedTab,
                            onTabChange = { selectedTab = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonContent(
    date1: String,
    date2: String,
    records1: List<WeatherRecord>,
    records2: List<WeatherRecord>,
    selectedTab: Int,
    onTabChange: (Int) -> Unit
) {
    val temps1  = records1.mapNotNull { it.temperature }
    val temps2  = records2.mapNotNull { it.temperature }
    val humids1 = records1.mapNotNull { it.humidity }
    val humids2 = records2.mapNotNull { it.humidity }
    val winds1  = records1.mapNotNull { it.windSpeed }
    val winds2  = records2.mapNotNull { it.windSpeed }

    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
        listOf("Resumen", "Temperatura", "Humedad", "Viento").forEachIndexed { i, title ->
            Tab(selected = selectedTab == i, onClick = { onTabChange(i) }, text = { Text(title) })
        }
    }

    Spacer(Modifier.height(16.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        LegendDot(color = COLOR_1, label = date1)
        LegendDot(color = COLOR_2, label = date2)
    }

    when (selectedTab) {
        0 -> ComparisonSummaryTable(
            date1 = date1, records1 = records1,
            date2 = date2, records2 = records2
        )
        1 -> NativeBarChart(
            title   = "Temperatura (°C)",
            labels  = listOf("Promedio", "Máxima", "Mínima"),
            series1 = listOf(temps1.safeAverage(), temps1.maxOrNull() ?: 0.0, temps1.minOrNull() ?: 0.0),
            series2 = listOf(temps2.safeAverage(), temps2.maxOrNull() ?: 0.0, temps2.minOrNull() ?: 0.0),
            unit    = "°C"
        )
        2 -> NativeBarChart(
            title   = "Humedad (%)",
            labels  = listOf("Promedio", "Máxima", "Mínima"),
            series1 = listOf(humids1.safeAverage(), humids1.maxOrNull() ?: 0.0, humids1.minOrNull() ?: 0.0),
            series2 = listOf(humids2.safeAverage(), humids2.maxOrNull() ?: 0.0, humids2.minOrNull() ?: 0.0),
            unit    = "%"
        )
        3 -> NativeBarChart(
            title   = "Viento (km/h)",
            labels  = listOf("Promedio", "Máxima", "Mínima"),
            series1 = listOf(winds1.safeAverage(), winds1.maxOrNull() ?: 0.0, winds1.minOrNull() ?: 0.0),
            series2 = listOf(winds2.safeAverage(), winds2.maxOrNull() ?: 0.0, winds2.minOrNull() ?: 0.0),
            unit    = "km/h"
        )
    }
}

@Composable
fun NativeBarChart(
    title: String,
    labels: List<String>,
    series1: List<Double>,
    series2: List<Double>,
    unit: String,
    modifier: Modifier = Modifier
) {
    val allValues = (series1 + series2).filter { it > 0.0 }
    val maxVal    = if (allValues.isEmpty()) 1.0 else allValues.max() * 1.15

    val color1     = COLOR_1
    val color2     = COLOR_2
    val textColor  = Color(0xFFCCCCCC)
    val gridColor  = Color(0x33FFFFFF)

    Text(
        text     = title,
        style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        val w        = size.width
        val h        = size.height
        val padLeft  = 56f
        val padRight = 16f
        val padTop   = 16f
        val padBot   = 52f
        val chartW   = w - padLeft - padRight
        val chartH   = h - padTop - padBot

        val gridLines = 4
        for (i in 0..gridLines) {
            val y = padTop + chartH * (1f - i.toFloat() / gridLines)
            drawLine(gridColor, Offset(padLeft, y), Offset(w - padRight, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(
                "%.0f".format(maxVal * i / gridLines),
                padLeft - 8f,
                y + 5f,
                android.graphics.Paint().apply {
                    color     = textColor.toArgb()
                    textSize  = 28f
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
            )
        }

        val n       = labels.size
        val groupW  = chartW / n
        val barW    = groupW * 0.28f
        val gap     = groupW * 0.06f
        val cornerR = CornerRadius(4f, 4f)

        labels.forEachIndexed { idx, label ->
            val groupX  = padLeft + idx * groupW
            val centerX = groupX + groupW / 2f
            val v1 = series1.getOrElse(idx) { 0.0 }
            val v2 = series2.getOrElse(idx) { 0.0 }
            val h1 = if (maxVal > 0) (v1 / maxVal * chartH).toFloat().coerceAtLeast(2f) else 2f
            val h2 = if (maxVal > 0) (v2 / maxVal * chartH).toFloat().coerceAtLeast(2f) else 2f
            val x1 = centerX - gap / 2f - barW
            val x2 = centerX + gap / 2f

            drawRoundRect(
                color        = color1,
                topLeft      = Offset(x1, padTop + chartH - h1),
                size         = Size(barW, h1),
                cornerRadius = cornerR
            )
            if (v1 > 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    "%.1f".format(v1),
                    x1 + barW / 2f,
                    padTop + chartH - h1 - 4f,
                    android.graphics.Paint().apply {
                        color          = color1.toArgb()
                        textSize       = 24f
                        textAlign      = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                        isAntiAlias    = true
                    }
                )
            }

            drawRoundRect(
                color        = color2,
                topLeft      = Offset(x2, padTop + chartH - h2),
                size         = Size(barW, h2),
                cornerRadius = cornerR
            )
            if (v2 > 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    "%.1f".format(v2),
                    x2 + barW / 2f,
                    padTop + chartH - h2 - 4f,
                    android.graphics.Paint().apply {
                        color          = color2.toArgb()
                        textSize       = 24f
                        textAlign      = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                        isAntiAlias    = true
                    }
                )
            }

            drawContext.canvas.nativeCanvas.drawText(
                label,
                centerX,
                padTop + chartH + 36f,
                android.graphics.Paint().apply {
                    color       = textColor.toArgb()
                    textSize    = 28f
                    textAlign   = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }
    }
}

@Composable
fun ComparisonSummaryTable(
    date1: String, records1: List<WeatherRecord>,
    date2: String, records2: List<WeatherRecord>
) {
    val temps1  = records1.mapNotNull { it.temperature }
    val temps2  = records2.mapNotNull { it.temperature }
    val humids1 = records1.mapNotNull { it.humidity }
    val humids2 = records2.mapNotNull { it.humidity }
    val winds1  = records1.mapNotNull { it.windSpeed }
    val winds2  = records2.mapNotNull { it.windSpeed }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ComparisonRow("Métrica", date1, date2, isHeader = true)
        ComparisonRow("🌡 Temp. Prom",   temps1.avg("°C"),     temps2.avg("°C"))
        ComparisonRow("🌡 Temp. Máx",    temps1.maxFmt("°C"),  temps2.maxFmt("°C"))
        ComparisonRow("🌡 Temp. Mín",    temps1.minFmt("°C"),  temps2.minFmt("°C"))
        ComparisonRow("💧 Hum. Prom",    humids1.avg("%"),      humids2.avg("%"))
        ComparisonRow("💧 Hum. Máx",     humids1.maxFmt("%"),   humids2.maxFmt("%"))
        ComparisonRow("💧 Hum. Mín",     humids1.minFmt("%"),   humids2.minFmt("%"))
        ComparisonRow("💨 Viento Prom",  winds1.avg("km/h"),   winds2.avg("km/h"))
        ComparisonRow("💨 Viento Máx",   winds1.maxFmt("km/h"),winds2.maxFmt("km/h"))
        ComparisonRow("📋 Registros",    "${records1.size}",   "${records2.size}")
    }
}

private fun List<Double>.avg(unit: String)    = if (isEmpty()) "--" else "%.1f$unit".format(average())
private fun List<Double>.maxFmt(unit: String) = maxOrNull()?.let { "%.1f$unit".format(it) } ?: "--"
private fun List<Double>.minFmt(unit: String) = minOrNull()?.let { "%.1f$unit".format(it) } ?: "--"

@Composable
fun ComparisonRow(label: String, val1: String, val2: String, isHeader: Boolean = false) {
    val containerColor = if (isHeader)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant
    val style = if (isHeader)
        MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    else
        MaterialTheme.typography.bodyMedium

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(8.dp),
        colors   = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(label, style = style, modifier = Modifier.weight(2f))
            Text(val1,  style = style.copy(color = COLOR_1), modifier = Modifier.weight(1.2f))
            Text(val2,  style = style.copy(color = COLOR_2), modifier = Modifier.weight(1.2f))
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(3.dp))
        )
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
