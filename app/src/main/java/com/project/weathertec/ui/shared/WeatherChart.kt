package com.project.weathertec.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

// Convierte Double a Float de forma segura: NaN/Infinity → 0f
private fun Double.safeFloat(): Float =
    if (isNaN() || isInfinite()) 0f else toFloat()

// ─── Gráfica de líneas de UNA variable ──────────────────────────────────────
@Composable
fun SingleLineChart(
    values: List<Pair<String, Double>>,
    color: Color = MaterialTheme.colorScheme.primary,
    title: String = "",
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) return

    val producer = remember { ChartEntryModelProducer() }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(values.size) {
        producer.setEntries(values.mapIndexed { i, (_, v) -> entryOf(i.toFloat(), v.safeFloat()) })
        ready = true
    }

    if (!ready) return

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp))
        }
        Chart(
            chart = lineChart(lines = listOf(lineSpec(lineColor = color))),
            chartModelProducer = producer,
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(
                valueFormatter = { value, _ -> values.getOrNull(value.toInt())?.first ?: "" },
                itemPlacer = AxisItemPlacer.Horizontal.default(spacing = maxOf(1, values.size / 6))
            ),
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )
    }
}

// ─── Gráfica de líneas de VARIAS variables (comparativa) ─────────────────────
@Composable
fun MultiLineChart(
    tempValues: List<Pair<String, Double>>,
    humValues:  List<Pair<String, Double>>,
    windValues: List<Pair<String, Double>>,
    showTemp: Boolean = true,
    showHum:  Boolean = true,
    showWind: Boolean = true,
    modifier: Modifier = Modifier
) {
    data class Serie(val points: List<Pair<String, Double>>, val color: Color)

    val active = buildList {
        if (showTemp && tempValues.isNotEmpty()) add(Serie(tempValues, Color(0xFFE57373)))
        if (showHum  && humValues.isNotEmpty())  add(Serie(humValues,  Color(0xFF64B5F6)))
        if (showWind && windValues.isNotEmpty()) add(Serie(windValues, Color(0xFF81C784)))
    }
    if (active.isEmpty()) return

    val labels = active.first().points.map { it.first }
    val producer = remember { ChartEntryModelProducer() }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(showTemp, showHum, showWind, tempValues.size, humValues.size, windValues.size) {
        producer.setEntries(
            active.map { serie ->
                serie.points.mapIndexed { i, (_, v) -> entryOf(i.toFloat(), v.safeFloat()) }
            }
        )
        ready = true
    }

    if (!ready) return

    Chart(
        chart = lineChart(lines = active.map { lineSpec(lineColor = it.color) }),
        chartModelProducer = producer,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(
            valueFormatter = { value, _ -> labels.getOrNull(value.toInt()) ?: "" },
            itemPlacer = AxisItemPlacer.Horizontal.default(spacing = maxOf(1, labels.size / 6))
        ),
        modifier = modifier.fillMaxWidth().height(220.dp)
    )
}

// ─── Gráfica de barras de UNA variable ───────────────────────────────────────
@Composable
fun BarChart(
    series: List<Pair<String, Double>>,
    color: Color = MaterialTheme.colorScheme.primary,
    title: String = "",
    modifier: Modifier = Modifier
) {
    if (series.isEmpty()) return

    val producer = remember { ChartEntryModelProducer() }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(series.size) {
        producer.setEntries(series.mapIndexed { i, (_, v) -> entryOf(i.toFloat(), v.safeFloat()) })
        ready = true
    }

    if (!ready) return

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp))
        }
        Chart(
            chart = columnChart(),
            chartModelProducer = producer,
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(
                valueFormatter = { value, _ -> series.getOrNull(value.toInt())?.first ?: "" },
                itemPlacer = AxisItemPlacer.Horizontal.default(spacing = maxOf(1, series.size / 6))
            ),
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )
    }
}

// ─── Gráfica de barras COMPARATIVA (2 series) ────────────────────────────────
@Composable
fun ComparisonBarChart(
    labels: List<String>,
    series1: List<Double>,
    series2: List<Double>,
    label1: String,
    label2: String,
    title: String = "",
    modifier: Modifier = Modifier
) {
    val size = maxOf(series1.size, series2.size)
    if (size == 0) return

    val s1 = if (series1.isEmpty()) List(size) { 0.0 } else series1
    val s2 = if (series2.isEmpty()) List(size) { 0.0 } else series2

    val producer = remember { ChartEntryModelProducer() }
    var ready by remember { mutableStateOf(false) }

    val key = remember(s1, s2) { s1.hashCode() * 31 + s2.hashCode() }
    LaunchedEffect(key) {
        producer.setEntries(
            listOf(
                s1.mapIndexed { i, v -> entryOf(i.toFloat(), v.safeFloat()) },
                s2.mapIndexed { i, v -> entryOf(i.toFloat(), v.safeFloat()) }
            )
        )
        ready = true
    }

    // No renderizar la gráfica hasta que el producer tenga datos
    if (!ready) return

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp))
        }
        Row(
            modifier = Modifier.padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(color = Color(0xFF5C6BC0), label = label1)
            LegendItem(color = Color(0xFF26A69A), label = label2)
        }
        Chart(
            chart = columnChart(),
            chartModelProducer = producer,
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(
                valueFormatter = { value, _ -> labels.getOrNull(value.toInt()) ?: "" }
            ),
            modifier = Modifier.fillMaxWidth().height(220.dp)
        )
    }
}

// ─── Punto de leyenda ─────────────────────────────────────────────────────────
@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
