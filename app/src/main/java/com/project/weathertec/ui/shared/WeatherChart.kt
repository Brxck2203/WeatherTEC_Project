package com.project.weathertec.ui.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.*
import com.patrykandpatrick.vico.compose.cartesian.axis.*
import com.patrykandpatrick.vico.compose.cartesian.layer.*
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.*
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer

/**
 * Gráfica de líneas para una sola variable.
 * [values] = lista de pares (etiqueta, valor)
 * [color] = color de la línea
 * [title] = título encima de la gráfica
 */
@Composable
fun SingleLineChart(
    values: List<Pair<String, Double>>,
    color: Color = MaterialTheme.colorScheme.primary,
    title: String = "",
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(values) {
        modelProducer.runTransaction {
            lineSeries { series(values.map { it.second.toFloat() }) }
        }
    }

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(fill(color))
                        )
                    )
                ),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = { _, x, _ ->
                        values.getOrNull(x.toInt())?.first ?: ""
                    }
                )
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }
}

/**
 * Gráfica de líneas con TRES series (temperatura, humedad, viento) superpuestas.
 * Solo muestra las que el usuario activa con checkboxes.
 */
@Composable
fun MultiLineChart(
    tempValues: List<Pair<String, Double>>,
    humValues: List<Pair<String, Double>>,
    windValues: List<Pair<String, Double>>,
    showTemp: Boolean = true,
    showHum: Boolean = true,
    showWind: Boolean = true,
    modifier: Modifier = Modifier
) {
    val activeSeries = buildList {
        if (showTemp && tempValues.isNotEmpty()) add(tempValues to Color(0xFFE57373))
        if (showHum  && humValues.isNotEmpty())  add(humValues  to Color(0xFF64B5F6))
        if (showWind && windValues.isNotEmpty()) add(windValues to Color(0xFF81C784))
    }
    if (activeSeries.isEmpty()) return

    val labels = activeSeries.first().first.map { it.first }
    val modelProducer = remember(showTemp, showHum, showWind) { CartesianChartModelProducer() }

    LaunchedEffect(showTemp, showHum, showWind, tempValues, humValues, windValues) {
        modelProducer.runTransaction {
            activeSeries.forEach { (series, _) ->
                lineSeries { series(series.map { it.second.toFloat() }) }
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    activeSeries.map { (_, color) ->
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(fill(color))
                        )
                    }
                )
            ),
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(
                valueFormatter = { _, x, _ -> labels.getOrNull(x.toInt()) ?: "" }
            )
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    )
}

/**
 * Gráfica de barras.
 * [series] = lista de pares (etiqueta eje X, valor)
 */
@Composable
fun BarChart(
    series: List<Pair<String, Double>>,
    color: Color = MaterialTheme.colorScheme.primary,
    title: String = "",
    modifier: Modifier = Modifier
) {
    if (series.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(series) {
        modelProducer.runTransaction {
            columnSeries { series(series.map { it.second.toFloat() }) }
        }
    }

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = { _, x, _ ->
                        series.getOrNull(x.toInt())?.first ?: ""
                    }
                )
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }
}

/**
 * Gráfica de barras comparativa: dos series (día 1 vs día 2) por variable.
 */
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
    if (series1.isEmpty() && series2.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(series1, series2) {
        modelProducer.runTransaction {
            columnSeries {
                series(series1.map { it.toFloat() })
                series(series2.map { it.toFloat() })
            }
        }
    }

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        // Leyenda manual
        Row(
            modifier = Modifier.padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(color = Color(0xFF5C6BC0), label = label1)
            LegendItem(color = Color(0xFF26A69A), label = label2)
        }
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = { _, x, _ -> labels.getOrNull(x.toInt()) ?: "" }
                )
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = androidx.compose.foundation.shape.CircleShape)
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
